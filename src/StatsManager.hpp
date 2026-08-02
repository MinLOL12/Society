#pragma once
#include <Geode/Geode.hpp>
#include <string>
#include <vector>
#include <unordered_map>
#include <mutex>

namespace progressive {

struct LevelStats {
    int totalAttempts = 0;
    int totalJumps = 0;
    double totalPlaytime = 0.0; // seconds
    float bestPercent = 0.0f;
    std::vector<float> runs; // last 1000 run percents
    // session data not persisted, but we cache here for convenience? we will manage session separately

    void addRun(float percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        runs.push_back(percent);
        if (runs.size() > 1000) {
            runs.erase(runs.begin(), runs.begin() + (runs.size() - 1000));
        }
        if (percent > bestPercent) {
            bestPercent = percent;
        }
    }

    float getAverage(int lastN = 50) const {
        if (runs.empty()) return 0.f;
        int count = std::min<int>(lastN, (int)runs.size());
        float sum = 0.f;
        for (int i = (int)runs.size() - count; i < (int)runs.size(); ++i) sum += runs[i];
        return sum / count;
    }

    float getConsistency(float threshold) const {
        if (runs.empty()) return 0.f;
        int passed = 0;
        for (float r : runs) if (r >= threshold) ++passed;
        return (float)passed / (float)runs.size() * 100.f;
    }

    int getDeathsAbove(float threshold) const {
        int c = 0;
        for (float r : runs) if (r >= threshold) ++c;
        return c;
    }
};

struct SessionStats {
    int attempts = 0;
    double playtime = 0.0;
    float best = 0.0f;
    std::vector<float> runs;
    void addRun(float p) {
        runs.push_back(p);
        if (p > best) best = p;
    }
    float getAverage(int lastN = 50) const {
        if (runs.empty()) return 0.f;
        int count = std::min<int>(lastN, (int)runs.size());
        float sum = 0.f;
        for (int i = (int)runs.size() - count; i < (int)runs.size(); ++i) sum += runs[i];
        return sum / count;
    }
    float getConsistency(float threshold) const {
        if (runs.empty()) return 0.f;
        int passed = 0;
        for (float r : runs) if (r >= threshold) ++passed;
        return (float)passed / (float)runs.size() * 100.f;
    }
};

class StatsManager {
public:
    static StatsManager* get();
    void load();
    void save();
    LevelStats& getOrCreate(const std::string& key);
    bool has(const std::string& key) const;
    void ensureExists(const std::string& key);

    SessionStats& getSession(const std::string& key);
    void resetSession(const std::string& key);
    void resetAllSessions();

    std::unordered_map<std::string, LevelStats>& getAllLevels() { return m_levels; }

private:
    StatsManager() {}
    std::unordered_map<std::string, LevelStats> m_levels;
    std::unordered_map<std::string, SessionStats> m_sessions;
    bool m_loaded = false;
    std::string getSavePath();
};

} // namespace progressive
