#include "StatsManager.hpp"
#include <Geode/loader/Mod.hpp>

using namespace geode::prelude;

namespace progressive {

StatsManager* StatsManager::get() {
    static StatsManager instance;
    return &instance;
}

std::string StatsManager::getSavePath() {
    auto dir = Mod::get()->getSaveDir();
    return (dir / "level_stats.json").string();
}

void StatsManager::load() {
    if (m_loaded) return;
    m_loaded = true;
    m_levels.clear();

    auto path = getSavePath();
    if (!std::filesystem::exists(path)) {
        log::info("[ProgressiveStats] No save file found, starting fresh");
        return;
    }

    auto res = file::readJson(path);
    if (!res.isOk()) {
        log::warn("[ProgressiveStats] Failed to read save: {}", res.unwrapErr());
        return;
    }

    auto json = res.unwrap();
    if (!json.contains("levels") || !json["levels"].isObject()) {
        log::warn("[ProgressiveStats] Invalid save format");
        return;
    }

    auto levels = json["levels"];
    for (auto& [key, val] : levels) {
        LevelStats st;
        if (val.contains("totalAttempts")) st.totalAttempts = val["totalAttempts"].asInt().unwrapOr(0);
        if (val.contains("totalJumps")) st.totalJumps = val["totalJumps"].asInt().unwrapOr(0);
        if (val.contains("totalPlaytime")) st.totalPlaytime = val["totalPlaytime"].asDouble().unwrapOr(0.0);
        if (val.contains("bestPercent")) st.bestPercent = (float)val["bestPercent"].asDouble().unwrapOr(0.0);
        if (val.contains("runs") && val["runs"].isArray()) {
            for (auto& r : val["runs"]) {
                float f = (float)r.asDouble().unwrapOr(0.0);
                if (f >= 0 && f <= 100) st.runs.push_back(f);
            }
        }
        m_levels[key] = std::move(st);
    }

    log::info("[ProgressiveStats] Loaded {} levels", m_levels.size());
}

void StatsManager::save() {
    matjson::Value root = matjson::Value::object();
    matjson::Value levelsObj = matjson::Value::object();

    for (auto& [key, st] : m_levels) {
        matjson::Value obj = matjson::Value::object();
        obj["totalAttempts"] = st.totalAttempts;
        obj["totalJumps"] = st.totalJumps;
        obj["totalPlaytime"] = st.totalPlaytime;
        obj["bestPercent"] = st.bestPercent;
        auto arr = matjson::Value::array();
        // limit runs saved to 1000 already limited
        for (float r : st.runs) arr.push(r);
        obj["runs"] = arr;
        levelsObj[key] = obj;
    }
    root["levels"] = levelsObj;
    root["version"] = 1;

    auto path = getSavePath();
    auto dir = Mod::get()->getSaveDir();
    std::filesystem::create_directories(dir);

    auto res = file::writeToJson(path, root);
    if (!res.isOk()) {
        log::warn("[ProgressiveStats] Failed to save: {}", res.unwrapErr());
    } else {
        log::debug("[ProgressiveStats] Saved {} levels", m_levels.size());
    }
}

LevelStats& StatsManager::getOrCreate(const std::string& key) {
    if (!m_loaded) load();
    auto it = m_levels.find(key);
    if (it == m_levels.end()) {
        m_levels[key] = LevelStats();
    }
    return m_levels[key];
}

bool StatsManager::has(const std::string& key) const {
    return m_levels.find(key) != m_levels.end();
}

void StatsManager::ensureExists(const std::string& key) {
    getOrCreate(key);
    // ensure session exists too
    if (m_sessions.find(key) == m_sessions.end()) {
        m_sessions[key] = SessionStats();
    }
}

SessionStats& StatsManager::getSession(const std::string& key) {
    auto it = m_sessions.find(key);
    if (it == m_sessions.end()) {
        m_sessions[key] = SessionStats();
    }
    return m_sessions[key];
}

void StatsManager::resetSession(const std::string& key) {
    m_sessions[key] = SessionStats();
}

void StatsManager::resetAllSessions() {
    m_sessions.clear();
}

} // namespace progressive
