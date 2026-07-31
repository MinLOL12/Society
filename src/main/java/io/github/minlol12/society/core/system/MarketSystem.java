package io.github.minlol12.society.core.system;

import java.util.List;
import java.util.Random;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;

/**
 * The village marketplace in action: citizens bring what they produce and
 * buy what they need. Farmers sell food, crafters sell bows and weapons,
 * guards buy equipment for defence, healers sell potions, and everyone
 * picks up a little food and tools.
 *
 * <p>This runs alongside the economy every day, turning abstract stockpiles
 * into visible, purposeful transactions at the market stalls.</p>
 */
public final class MarketSystem {

    private MarketSystem() { }

    /**
     * Runs a day of market trading for a settlement. Villagers with goods to
     * sell bring them to market; those who need things buy them. The settlement
     * treasury handles the coin flow, and citizens' personal wealth rises and
     * falls with their trades.
     */
    public static void tick(SocietyEngine engine, Settlement s) {
        int marketCount = s.countBuildings(StructureType.MARKET_STALL)
                + s.countBuildings(StructureType.MARKETPLACE) * 3
                + s.countBuildings(StructureType.TRADING_POST) * 2;
        if (marketCount <= 0) return;

        List<Citizen> people = engine.liveCitizensOf(s);
        if (people.isEmpty()) return;

        Random random = engine.random();
        double tradeVolume = Math.min(1.0, marketCount / 8.0);

        for (Citizen citizen : people) {
            if (!citizen.isAdult(engine.day())) continue;

            // --- Sellers: professionals put surplus goods on the market ---
            sellGoods(engine, s, citizen, random, tradeVolume);

            // --- Buyers: citizens purchase what they need ---
            buyGoods(engine, s, citizen, random, tradeVolume);
        }
    }

    /**
     * A citizen sells surplus goods from their profession's production.
     * Crafters sell bows, weapons, and tools; farmers sell food; healers
     * sell potions and medicine; miners sell ore.
     */
    private static void sellGoods(SocietyEngine engine, Settlement s, Citizen citizen,
                                   Random random, double tradeVolume) {
        SimProfession profession = citizen.profession();
        if (profession == SimProfession.NONE) return;

        Skill primary = profession.primarySkill();
        int level = primary == null ? 0 : citizen.skillLevel(primary);
        double skillFactor = 0.8 + level / 120.0;

        // Each profession puts its products on the market
        Good[] sellable = goodsForSale(profession);
        if (sellable.length == 0) return;

        for (Good good : sellable) {
            double stock = s.stock(good);
            double desired = s.desiredStock(good);
            if (stock <= desired * 0.6) continue; // keep enough for the settlement

            double surplus = stock - desired * 0.5;
            double amountToSell = Math.min(surplus, 0.8 * skillFactor * tradeVolume);
            if (amountToSell <= 0.05) continue;

            double price = s.priceOf(good);
            double income = amountToSell * price * 0.35;

            s.addStock(good, -amountToSell);
            s.addTreasury(income * 0.5);
            citizen.addWealth(income * 0.5);
        }
    }

    /**
     * A citizen buys what they need from the market: guards buy bows,
     * weapons and shields for defence; everyone buys food and tools;
     * scholars buy enchanted items for research.
     */
    private static void buyGoods(SocietyEngine engine, Settlement s, Citizen citizen,
                                  Random random, double tradeVolume) {
        double personalBudget = citizen.personalWealth();
        if (personalBudget < 1.0) return;

        SimProfession profession = citizen.profession();

        // Guards buy equipment: bows for ranged defence, weapons, shields
        if (profession == SimProfession.GUARD) {
            buyGuardEquipment(s, citizen, random, tradeVolume);
        }

        // Everyone buys food if the settlement has it
        buyNecessities(s, citizen, random, tradeVolume);

        // Scholars buy enchanted items and potions for research insight
        if (profession == SimProfession.SCHOLAR) {
            buyScholarlyGoods(s, citizen, random, tradeVolume);
        }

        // Healers buy potions to distribute
        if (profession == SimProfession.HEALER) {
            buyHealingGoods(s, citizen, random, tradeVolume);
        }
    }

    /**
     * Guards purchase bows, weapons and shields from the market to defend
     * the settlement. Well-equipped guards provide much better security.
     */
    private static void buyGuardEquipment(Settlement s, Citizen guard, Random random, double tradeVolume) {
        double budget = guard.personalWealth();

        // Buy a bow if the settlement has them (for watchtower duty)
        if (s.stock(Good.BOWS) > 0.3 && budget >= s.priceOf(Good.BOWS) * 0.5) {
            double amount = Math.min(0.3 * tradeVolume, s.stock(Good.BOWS));
            double cost = amount * s.priceOf(Good.BOWS) * 0.4;
            if (budget >= cost) {
                s.addStock(Good.BOWS, -amount);
                guard.addWealth(-cost);
            }
        }

        // Buy a weapon
        if (s.stock(Good.WEAPONS) > 0.2 && budget >= s.priceOf(Good.WEAPONS) * 0.3) {
            double amount = Math.min(0.2 * tradeVolume, s.stock(Good.WEAPONS));
            double cost = amount * s.priceOf(Good.WEAPONS) * 0.35;
            if (budget >= cost) {
                s.addStock(Good.WEAPONS, -amount);
                guard.addWealth(-cost);
            }
        }

        // Buy a shield
        if (s.stock(Good.SHIELDS) > 0.15 && budget >= s.priceOf(Good.SHIELDS) * 0.3) {
            double amount = Math.min(0.15 * tradeVolume, s.stock(Good.SHIELDS));
            double cost = amount * s.priceOf(Good.SHIELDS) * 0.35;
            if (budget >= cost) {
                s.addStock(Good.SHIELDS, -amount);
                guard.addWealth(-cost);
            }
        }
    }

    /** Everyone buys food and tools from the market to stay fed and productive. */
    private static void buyNecessities(Settlement s, Citizen citizen, Random random, double tradeVolume) {
        double budget = citizen.personalWealth();

        // Buy food
        if (s.stock(Good.FOOD) > 2.0 && budget >= 0.2) {
            double amount = Math.min(0.4 * tradeVolume, s.stock(Good.FOOD));
            double cost = amount * s.priceOf(Good.FOOD) * 0.25;
            if (budget >= cost) {
                s.addStock(Good.FOOD, -amount);
                citizen.addWealth(-cost);
                s.addTreasury(cost * 0.3);
            }
        }

        // Buy tools
        if (s.stock(Good.TOOLS) > 0.5 && budget >= 0.5) {
            double amount = Math.min(0.1 * tradeVolume, s.stock(Good.TOOLS));
            double cost = amount * s.priceOf(Good.TOOLS) * 0.3;
            if (budget >= cost) {
                s.addStock(Good.TOOLS, -amount);
                citizen.addWealth(-cost);
                s.addTreasury(cost * 0.3);
            }
        }
    }

    /** Scholars buy potions and enchanted items to boost research. */
    private static void buyScholarlyGoods(Settlement s, Citizen scholar, Random random, double tradeVolume) {
        double budget = scholar.personalWealth();

        if (s.stock(Good.POTIONS) > 0.2 && budget >= s.priceOf(Good.POTIONS) * 0.3) {
            double amount = Math.min(0.15 * tradeVolume, s.stock(Good.POTIONS));
            double cost = amount * s.priceOf(Good.POTIONS) * 0.35;
            if (budget >= cost) {
                s.addStock(Good.POTIONS, -amount);
                scholar.addWealth(-cost);
                s.addTreasury(cost * 0.2);
            }
        }

        if (s.stock(Good.ENCHANTED) > 0.1 && budget >= s.priceOf(Good.ENCHANTED) * 0.2) {
            double amount = Math.min(0.05 * tradeVolume, s.stock(Good.ENCHANTED));
            double cost = amount * s.priceOf(Good.ENCHANTED) * 0.25;
            if (budget >= cost) {
                s.addStock(Good.ENCHANTED, -amount);
                scholar.addWealth(-cost);
                s.addTreasury(cost * 0.2);
            }
        }
    }

    /** Healers buy potions for treatment. */
    private static void buyHealingGoods(Settlement s, Citizen healer, Random random, double tradeVolume) {
        double budget = healer.personalWealth();

        if (s.stock(Good.POTIONS) > 0.3 && budget >= s.priceOf(Good.POTIONS) * 0.3) {
            double amount = Math.min(0.2 * tradeVolume, s.stock(Good.POTIONS));
            double cost = amount * s.priceOf(Good.POTIONS) * 0.35;
            if (budget >= cost) {
                s.addStock(Good.POTIONS, -amount);
                healer.addWealth(-cost);
            }
        }
    }

    /** What each profession brings to market. */
    private static Good[] goodsForSale(SimProfession profession) {
        switch (profession) {
            case FARMER: return new Good[]{ Good.FOOD };
            case LUMBERJACK: return new Good[]{ Good.WOOD };
            case MINER: return new Good[]{ Good.STONE, Good.IRON, Good.GEMS };
            case CRAFTER: return new Good[]{ Good.TOOLS, Good.BOWS, Good.WEAPONS, Good.SHIELDS, Good.CLOTH };
            case HEALER: return new Good[]{ Good.MEDICINE, Good.POTIONS };
            case TRADER: return new Good[]{ Good.LUXURY, Good.ENCHANTED };
            default: return new Good[0];
        }
    }
}
