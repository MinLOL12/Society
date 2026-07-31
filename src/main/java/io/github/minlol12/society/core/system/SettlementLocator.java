package io.github.minlol12.society.core.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.CultureSampler;
import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.VillagerSnapshot;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.SimProfession;

/**
 * Watches where loaded villagers actually live (bells, beds, bodies) and
 * keeps the ledger's settlements aligned with the physical world: villages
 * are founded when enough villagers settle together, members join when they
 * wander close, and settlement centres drift with their people.
 *
 * <p>Runs once per simulation day, before the daily tick.</p>
 */
public final class SettlementLocator {

    /** Max gap between two anchors that still counts as one community. */
    private static final int CLUSTER_RADIUS = 64;
    /** A cluster this close to an existing settlement joins it instead. */
    private static final int ADOPT_RADIUS = 150;
    /** New settlements need at least this many grown-up villagers. */
    private static final int FOUNDING_ADULTS = 3;

    private SettlementLocator() { }

    public static void processDaily(SocietyEngine engine, List<VillagerSnapshot> snapshots, CultureSampler sampler) {
        engine.touchedToday().clear();

        int n = snapshots.size();
        if (n == 0) {
            return;
        }

        // --- 1. Union-find clustering over anchors -------------------------
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (sameCluster(snapshots.get(i), snapshots.get(j))) {
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, List<VillagerSnapshot>> clusters = new HashMap<Integer, List<VillagerSnapshot>>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            List<VillagerSnapshot> cluster = clusters.get(Integer.valueOf(root));
            if (cluster == null) {
                cluster = new ArrayList<VillagerSnapshot>();
                clusters.put(Integer.valueOf(root), cluster);
            }
            cluster.add(snapshots.get(i));
        }

        // --- 2. Resolve each cluster to a settlement -----------------------
        for (List<VillagerSnapshot> cluster : clusters.values()) {
            resolveCluster(engine, cluster, sampler);
        }

        // --- 3. Forget villagers whose entities vanished silently ----------
        sweepGhosts(engine);
    }

    private static boolean sameCluster(VillagerSnapshot a, VillagerSnapshot b) {
        double dx = a.anchorX() - b.anchorX();
        double dz = a.anchorZ() - b.anchorZ();
        return dx * dx + dz * dz <= CLUSTER_RADIUS * CLUSTER_RADIUS;
    }

    private static int find(int[] parent, int i) {
        int root = i;
        while (parent[root] != root) root = parent[root];
        while (parent[i] != root) {
            int next = parent[i];
            parent[i] = root;
            i = next;
        }
        return root;
    }

    private static void union(int[] parent, int i, int j) {
        parent[find(parent, i)] = find(parent, j);
    }

    private static void resolveCluster(SocietyEngine engine, List<VillagerSnapshot> cluster, CultureSampler sampler) {
        int day = engine.day();

        // Majority settlement among members who already belong somewhere.
        Map<String, Integer> votes = new HashMap<String, Integer>();
        List<Citizen> members = new ArrayList<Citizen>();
        double sumX = 0, sumY = 0, sumZ = 0;
        for (VillagerSnapshot snap : cluster) {
            Citizen citizen = engine.citizenForSnapshot(snap, sampler);
            if (citizen == null) continue;
            members.add(citizen);
            citizen.noteSeen(day);
            String home = citizen.homeSettlementId();
            if (!home.isEmpty()) {
                Settlement known = engine.settlements().get(home);
                if (known != null && !known.isDestroyed()) {
                    Integer v = votes.get(home);
                    votes.put(home, Integer.valueOf(v == null ? 1 : v.intValue() + 1));
                }
            }
            sumX += snap.anchorX();
            sumY += snap.anchorY();
            sumZ += snap.anchorZ();
        }
        if (members.isEmpty()) return;
        int cx = (int) (sumX / members.size());
        int cy = (int) (sumY / members.size());
        int cz = (int) (sumZ / members.size());

        Settlement settlement = null;
        String bestKey = null;
        int bestVotes = 0;
        for (Map.Entry<String, Integer> e : votes.entrySet()) {
            Settlement candidate = engine.settlements().get(e.getKey());
            if (candidate == null) continue;
            int v = e.getValue().intValue();
            // Ties are broken by settlement name, not id: names are content,
            // ids are random per session.
            if (v > bestVotes
                    || (v == bestVotes && bestKey != null
                        && candidate.name().compareTo(bestKey) < 0)) {
                bestVotes = v;
                settlement = candidate;
                bestKey = candidate.name();
            }
        }
        if (settlement == null) {
            // Adopt into the nearest existing settlement, if close enough.
            settlement = engine.findSettlementNear(cx, cz, ADOPT_RADIUS);
        }
        if (settlement == null) {
            // Found a new settlement when the group is grown enough.
            int adults = 0;
            for (VillagerSnapshot snap : cluster) {
                if (!snap.baby) adults++;
            }
            if (adults >= FOUNDING_ADULTS && engine.settlementsAlive() < engine.cfg().maxSettlements) {
                settlement = engine.foundSettlement(members, cx, cy, cz, sampler);
            }
        }
        if (settlement == null) return;

        // --- Assign & migrate members --------------------------------------
        List<Citizen> memberCopy = new ArrayList<Citizen>(members);
        for (Citizen citizen : memberCopy) {
            if (!settlement.id().equals(citizen.homeSettlementId())) {
                engine.transferCitizen(citizen, settlement);
            }
        }

        // --- Professions spoken by workstations -----------------------------
        for (VillagerSnapshot snap : cluster) {
            Citizen citizen = engine.citizenForEntity(snap.entityUuid);
            if (citizen == null) continue;
            SimProfession mapped = SimProfession.fromVanillaId(snap.professionId);
            if (mapped != SimProfession.NONE
                    && settlement.id().equals(citizen.homeSettlementId())) {
                // The physical world is authoritative for manifested villagers.
                citizen.setProfession(mapped);
                if (citizen.preferredProfession() == SimProfession.NONE) {
                    citizen.setPreferredProfession(mapped);
                }
                engine.noteWorkstationJob(settlement, citizen);
            }
        }

        // --- Drift the centre toward the living heart of the community -----
        settlement.setCenter(cx, cy, cz);
        engine.touchedToday().add(settlement.id());
    }

    /**
     * If a settlement was visited today but one of its manifested citizens'
     * entities has not been seen in several days, the entity is lost
     * (removed by something other than a death event). The citizen lives on
     * in the ledger.
     */
    private static void sweepGhosts(SocietyEngine engine) {
        int day = engine.day();
        List<String> touched = new ArrayList<String>(engine.touchedToday());
        for (String id : touched) {
            Settlement s = engine.settlements().get(id);
            if (s == null || s.isDestroyed()) continue;
            for (String cid : new ArrayList<String>(s.citizenIds())) {
                Citizen c = engine.citizens().get(cid);
                if (c != null && c.isAlive() && c.isManifested()
                        && c.lastSeenDay() >= 0 && day - c.lastSeenDay() > 3) {
                    c.unbindEntity();
                }
            }
        }
    }
}
