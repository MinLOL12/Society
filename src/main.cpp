#include <Geode/Geode.hpp>
#include <Geode/modify/PlayLayer.hpp>
#include <Geode/modify/PauseLayer.hpp>
#include <Geode/modify/LevelInfoLayer.hpp>
#include <Geode/binding/FMODAudioEngine.hpp>
#include "StatsManager.hpp"
#include "HUD.hpp"
#include "Utils.hpp"

using namespace geode::prelude;

namespace progressive {

struct MyPlayLayerFields {
    std::string levelKey;
    StatsHUD* hud = nullptr;
    float lastPercent = 0.f;
    bool hasDied = false;
    bool completed = false;
    int runJumps = 0;
    double runTime = 0.0;
    bool isTracking = true;
    bool hasAddedRunThisAttempt = false;
};

class $modify(StatsPlayLayer, PlayLayer) {
    struct Fields : MyPlayLayerFields {};

    bool init(GJGameLevel* level, bool useReplay, bool dontCreateObjects) {
        if (!PlayLayer::init(level, useReplay, dontCreateObjects)) return false;

        auto mgr = StatsManager::get();
        mgr->load();

        std::string key = Utils::getLevelKey(level);
        m_fields->levelKey = key;
        m_fields->isTracking = true;

        bool trackPractice = Mod::get()->getSettingValue<bool>("track-practice");
        if (this->m_isPracticeMode && !trackPractice) {
            m_fields->isTracking = false;
        }

        mgr->ensureExists(key);
        auto& stats = mgr->getOrCreate(key);
        auto& session = mgr->getSession(key);

        stats.totalAttempts += 1;
        session.attempts += 1;
        m_fields->runJumps = 0;
        m_fields->runTime = 0.0;
        m_fields->lastPercent = 0.f;
        m_fields->hasDied = false;
        m_fields->completed = false;
        m_fields->hasAddedRunThisAttempt = false;

        if (Mod::get()->getSettingValue<bool>("hud-enabled")) {
            auto hud = StatsHUD::create(this);
            if (hud) {
                m_fields->hud = hud;
                this->addChild(hud, 1000);
                hud->setID("ps-hud"_spr);
            }
        }

        log::info("[ProgressiveStats] Started {} total {} sess {}", key, stats.totalAttempts, session.attempts);
        return true;
    }

    void onQuit() {
        auto mgr = StatsManager::get();
        if (m_fields->isTracking && !m_fields->hasAddedRunThisAttempt) {
            float cur = this->getCurrentPercent();
            if (cur > 1.0f && cur < 99.5f) {
                float finalPercent = m_fields->lastPercent > 0 ? m_fields->lastPercent : cur;
                if (finalPercent > 0.5f) {
                    auto& stats = mgr->getOrCreate(m_fields->levelKey);
                    auto& session = mgr->getSession(m_fields->levelKey);
                    stats.addRun(finalPercent);
                    session.addRun(finalPercent);
                    log::info("[ProgressiveStats] Quit run at {}%", finalPercent);
                }
            }
        }

        mgr->save();

        if (Mod::get()->getSettingValue<bool>("reset-session-on-exit")) {
            mgr->resetSession(m_fields->levelKey);
        }

        PlayLayer::onQuit();
    }

    void resetLevel() {
        float curBefore = this->getCurrentPercent();

        if (m_fields->isTracking && !m_fields->hasDied && !m_fields->hasAddedRunThisAttempt && !m_fields->completed) {
            if (curBefore > 1.0f) {
                auto mgr = StatsManager::get();
                auto& stats = mgr->getOrCreate(m_fields->levelKey);
                auto& session = mgr->getSession(m_fields->levelKey);
                stats.addRun(curBefore);
                session.addRun(curBefore);
                m_fields->hasAddedRunThisAttempt = true;
                log::info("[ProgressiveStats] Manual reset {}", curBefore);
            }
        }

        PlayLayer::resetLevel();

        if (m_fields->isTracking) {
            auto mgr = StatsManager::get();
            auto& stats = mgr->getOrCreate(m_fields->levelKey);
            auto& session = mgr->getSession(m_fields->levelKey);
            stats.totalAttempts += 1;
            session.attempts += 1;
            m_fields->runJumps = 0;
            m_fields->runTime = 0.0;
            m_fields->lastPercent = 0.f;
            m_fields->hasDied = false;
            m_fields->completed = false;
            m_fields->hasAddedRunThisAttempt = false;
        }
    }

    void destroyPlayer(PlayerObject* player, GameObject* object) {
        if (m_fields->isTracking && !m_fields->hasAddedRunThisAttempt) {
            bool trackPractice = Mod::get()->getSettingValue<bool>("track-practice");
            if (!(this->m_isPracticeMode && !trackPractice)) {
                float percent = this->getCurrentPercent();
                m_fields->lastPercent = percent;
                m_fields->hasDied = true;

                auto mgr = StatsManager::get();
                auto& stats = mgr->getOrCreate(m_fields->levelKey);
                auto& session = mgr->getSession(m_fields->levelKey);

                float prevBest = stats.bestPercent;
                stats.addRun(percent);
                session.addRun(percent);

                bool isNewBest = percent > prevBest + 0.01f;
                if (isNewBest && percent > 1.f) {
                    log::info("[ProgressiveStats] NEW BEST {}% prev {}%", percent, prevBest);
                    if (m_fields->hud) m_fields->hud->showNewBest(percent);
                    if (Mod::get()->getSettingValue<bool>("sfx-new-best")) {
                        FMODAudioEngine::sharedEngine()->playEffect("achievement_01.ogg");
                    }
                }

                m_fields->hasAddedRunThisAttempt = true;
                mgr->save();
            }
        }

        PlayLayer::destroyPlayer(player, object);
    }

    void levelComplete() {
        if (m_fields->isTracking && !m_fields->hasAddedRunThisAttempt) {
            auto mgr = StatsManager::get();
            auto& stats = mgr->getOrCreate(m_fields->levelKey);
            auto& session = mgr->getSession(m_fields->levelKey);

            float prevBest = stats.bestPercent;
            stats.addRun(100.f);
            session.addRun(100.f);
            m_fields->hasAddedRunThisAttempt = true;
            m_fields->completed = true;

            if (100.f > prevBest) {
                log::info("[ProgressiveStats] NEW BEST 100%");
                if (m_fields->hud) m_fields->hud->showNewBest(100.f);
            }
            mgr->save();
        }

        PlayLayer::levelComplete();
    }

    void update(float dt) {
        PlayLayer::update(dt);
        if (!m_fields->isTracking) return;
        if (dt > 1.f || dt <= 0.f) return;
        if (this->m_isPaused) return;
        if (m_fields->hasDied || m_fields->completed) return;

        m_fields->runTime += dt;

        auto mgr = StatsManager::get();
        if (mgr->has(m_fields->levelKey)) {
            auto& stats = mgr->getOrCreate(m_fields->levelKey);
            auto& session = mgr->getSession(m_fields->levelKey);
            stats.totalPlaytime += dt;
            session.playtime += dt;

            if (m_fields->hud) {
                float cur = this->getCurrentPercent();
                m_fields->hud->updateHUD(cur, m_fields->runJumps, m_fields->runTime);
            }
        }
    }

    void incrementJumps() {
        PlayLayer::incrementJumps();
        if (!m_fields->isTracking) return;
        if (m_fields->hasDied) return;
        m_fields->runJumps += 1;
        auto mgr = StatsManager::get();
        if (mgr->has(m_fields->levelKey)) {
            auto& stats = mgr->getOrCreate(m_fields->levelKey);
            stats.totalJumps += 1;
        }
    }
};

class $modify(StatsPauseLayer, PauseLayer) {
    void customSetup() {
        PauseLayer::customSetup();

        auto pl = PlayLayer::get();
        if (!pl) return;
        std::string key = Utils::getLevelKey(pl->m_level);
        auto mgr = StatsManager::get();
        if (!mgr->has(key)) return;

        auto leftMenu = this->getChildByID("left-button-menu");
        if (!leftMenu) return;

        auto spr = CCSprite::createWithSpriteFrameName("GJ_timeIcon_001.png");
        if (!spr) spr = CCSprite::createWithSpriteFrameName("GJ_infoIcon_001.png");
        if (!spr) return;
        spr->setScale(0.75f);

        auto btn = CCMenuItemSpriteExtra::create(spr, this, menu_selector(StatsPauseLayer::onStatsBtn));
        btn->setID("ps-info-btn"_spr);
        leftMenu->addChild(btn);
        leftMenu->updateLayout();
    }

    void onStatsBtn(CCObject*) {
        auto pl = PlayLayer::get();
        if (!pl) return;
        std::string key = Utils::getLevelKey(pl->m_level);
        auto mgr = StatsManager::get();
        if (!mgr->has(key)) {
            FLAlertLayer::create("Progressive Stats", "No data for this level yet.", "OK")->show();
            return;
        }
        auto& stats = mgr->getOrCreate(key);
        auto& session = mgr->getSession(key);

        std::string content = fmt::format(
            "Level: {}\n\n"
            "<cy>Total</c>\n"
            "Best: {:.2f}%\n"
            "Attempts: {}\n"
            "Jumps: {}\n"
            "Playtime: {} ({:.1f}h)\n"
            "Avg (50): {:.2f}%\n"
            "Consistency: 25% {:.0f}% | 50% {:.0f}% | 75% {:.0f}% | 90% {:.0f}%\n\n"
            "<cg>Session</c>\n"
            "Best: {:.2f}%\n"
            "Attempts: {}\n"
            "Playtime: {}\n"
            "Avg (20): {:.2f}%\n"
            "Runs stored: {}",
            key,
            stats.bestPercent,
            stats.totalAttempts,
            stats.totalJumps,
            Utils::formatTime(stats.totalPlaytime, true), stats.totalPlaytime/3600.0,
            stats.getAverage(50),
            stats.getConsistency(25.f), stats.getConsistency(50.f), stats.getConsistency(75.f), stats.getConsistency(90.f),
            session.best,
            session.attempts,
            Utils::formatTime(session.playtime, true),
            session.getAverage(20),
            stats.runs.size()
        );

        FLAlertLayer::create("Progressive Stats", content, "OK")->show();
    }
};

class $modify(StatsLevelInfoLayer, LevelInfoLayer) {
    bool init(GJGameLevel* level, bool challenge) {
        if (!LevelInfoLayer::init(level, challenge)) return false;

        std::string key = Utils::getLevelKey(level);
        auto mgr = StatsManager::get();
        mgr->load();

        if (mgr->has(key)) {
            auto leftMenu = this->getChildByID("left-side-menu");
            if (!leftMenu) leftMenu = this->getChildByID("other-menu");
            if (!leftMenu) return true;

            auto spr = CCSprite::createWithSpriteFrameName("GJ_timeIcon_001.png");
            if (!spr) spr = CCSprite::createWithSpriteFrameName("GJ_completesIcon_001.png");
            if (!spr) return true;
            spr->setScale(0.85f);

            auto btn = CCMenuItemSpriteExtra::create(spr, this, menu_selector(StatsLevelInfoLayer::onStatsInfo));
            btn->setID("ps-level-stats-btn"_spr);
            btn->setUserObject(CCString::create(key));
            leftMenu->addChild(btn);
            leftMenu->updateLayout();
        }

        return true;
    }

    void onStatsInfo(CCObject* sender) {
        auto node = typeinfo_cast<CCMenuItemSpriteExtra*>(sender);
        std::string key = "unknown";
        if (node) {
            if (auto obj = typeinfo_cast<CCString*>(node->getUserObject())) {
                key = obj->getCString();
            }
        }

        auto mgr = StatsManager::get();
        if (!mgr->has(key)) {
            FLAlertLayer::create("No Stats", "No data for this level yet. Play it!", "OK")->show();
            return;
        }
        auto& stats = mgr->getOrCreate(key);
        auto& session = mgr->getSession(key);

        std::string content = fmt::format(
            "Level: {}\n\n"
            "Best: {:.2f}%\n"
            "Attempts: {} (sess {})\n"
            "Time: {} (sess {})\n"
            "Jumps: {}\n"
            "Average (50): {:.2f}%\n"
            "Consistency:\n"
            "  25%: {:.0f}% ({}/{} )\n"
            "  50%: {:.0f}%\n"
            "  75%: {:.0f}%\n"
            "  90%: {:.0f}%\n"
            "Runs stored: {}\n\n"
            "Session best: {:.2f}%\n"
            "Tip: Configure HUD in mod settings. Disable/reset session as you like.",
            key,
            stats.bestPercent,
            stats.totalAttempts, session.attempts,
            Utils::formatTime(stats.totalPlaytime, true), Utils::formatTime(session.playtime, true),
            stats.totalJumps,
            stats.getAverage(50),
            stats.getConsistency(25.f), stats.getDeathsAbove(25.f), (int)stats.runs.size(),
            stats.getConsistency(50.f),
            stats.getConsistency(75.f),
            stats.getConsistency(90.f),
            stats.runs.size(),
            session.best
        );

        FLAlertLayer::create("Progressive Stats", content, "OK")->show();
    }
};

} // namespace progressive

$on_mod(Loaded) {
    progressive::StatsManager::get()->load();
    geode::log::info("[ProgressiveStats] Mod loaded");
}

$execute {
    geode::log::info("[ProgressiveStats] Execute save hook installed");
}
