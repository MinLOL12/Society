#pragma once
#include <Geode/Geode.hpp>
#include <Geode/binding/GJGameLevel.hpp>
#include <string>
#include <cmath>
#include <cctype>

namespace progressive {

inline std::string sanitizeFileName(std::string s) {
    std::string out;
    for (char c : s) {
        if (std::isalnum((unsigned char)c) || c == '-' || c == '_' || c == ' ') out += c;
        else out += '_';
    }
    if (out.size() > 64) out = out.substr(0, 64);
    return out;
}

inline std::string getLevelKey(GJGameLevel* level) {
    if (!level) return "unknown";
    // Online level
    if (level->m_levelID != 0) {
        return fmt::format("online_{}", level->m_levelID.value());
    }
    // Platformer? still editor
    // Try to use level name + some hash of level string length for stability
    std::string name = level->m_levelName;
    if (name.empty()) name = "Unnamed";
    size_t strHash = std::hash<std::string>{}(level->m_levelString);
    // keep lower bits
    strHash = strHash % 1000000;
    std::string safeName = sanitizeFileName(name);
    // Replace spaces
    for (auto &c : safeName) if (c == ' ') c = '_';
    return fmt::format("editor_{}_{}", safeName, strHash);
}

inline std::string formatPercent(float p, int decimals) {
    if (decimals <= 0) return fmt::format("{:.0f}%", p);
    if (decimals == 1) return fmt::format("{:.1f}%", p);
    return fmt::format("{:.2f}%", p);
}

inline std::string formatTime(double seconds, bool shortForm = false) {
    if (seconds < 0) seconds = 0;
    long long total = (long long)seconds;
    long long days = total / 86400;
    long long hours = (total % 86400) / 3600;
    long long mins = (total % 3600) / 60;
    long long secs = total % 60;

    if (shortForm) {
        if (days > 0) return fmt::format("{}d {}h {}m {}s", days, hours, mins, secs);
        if (hours > 0) return fmt::format("{}h {}m {}s", hours, mins, secs);
        if (mins > 0) return fmt::format("{}m {}s", mins, secs);
        return fmt::format("{}s", secs);
    } else {
        if (days > 0) return fmt::format("{}d {:02}h {:02}m {:02}s", days, hours, mins, secs);
        if (hours > 0) return fmt::format("{}h {:02}m {:02}s", hours, mins, secs);
        if (mins > 0) return fmt::format("{:02}m {:02}s", mins, secs);
        return fmt::format("{}s", secs);
    }
}

inline std::string formatTimeCompact(double seconds) {
    long long total = (long long)seconds;
    long long h = total / 3600;
    long long m = (total % 3600) / 60;
    long long s = total % 60;
    if (h > 0) return fmt::format("{}:{:02}:{:02}", h, m, s);
    return fmt::format("{:02}:{:02}", m, s);
}

} // namespace progressive
