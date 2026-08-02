#include "HUD.hpp"
#include "Utils.hpp"
#include <Geode/modify/PlayLayer.hpp>

using namespace geode::prelude;

namespace progressive {

StatsHUD* StatsHUD::create(PlayLayer* playLayer) {
    auto ret = new StatsHUD();
    if (ret && ret->init(playLayer)) {
        ret->autorelease();
        return ret;
    }
    CC_SAFE_DELETE(ret);
    return nullptr;
}

bool StatsHUD::init(PlayLayer* playLayer) {
    if (!CCLayer::init()) return false;
    m_playLayer = playLayer;
    m_levelKey = Utils::getLevelKey(playLayer->m_level);
    
    // background
    float opacity = Mod::get()->getSettingValue<int64_t>("bg-opacity");
    m_bg = CCLayerColor::create(ccc4(0,0,0, (GLubyte)(opacity * 2.55f)));
    m_bg->setContentSize({220, 100});
    m_bg->setAnchorPoint({0,1});
    this->addChild(m_bg);

    // progress bar bg
    m_barBg = CCNode::create();
    m_barBg->setContentSize({200, 6});
    m_barBg->setAnchorPoint({0,0});
    // using LayerColor for bar background
    auto barBgColor = CCLayerColor::create(ccc4(60,60,60,190), 200, 6);
    barBgColor->setAnchorPoint({0,0});
    m_barBg->addChild(barBgColor);
    m_barFill = CCLayerColor::create(ccc4(0,255,100,255), 0, 6);
    m_barFill->setAnchorPoint({0,0});
    m_barBg->addChild(m_barFill);
    this->addChild(m_barBg);

    rebuildLabels();
    updatePosition();
    applySettings();
    return true;
}

cocos2d::CCLabelBMFont* StatsHUD::createLabel(const std::string& text, float scale, cocos2d::CCPoint anchor) {
    auto label = CCLabelBMFont::create(text.c_str(), "bigFont.fnt");
    label->setScale(scale);
    label->setAnchorPoint(anchor);
    label->setAlignment(kCCTextAlignmentLeft);
    this->addChild(label);
    return label;
}

void StatsHUD::rebuildLabels() {
    // remove old if exists
    if (m_currentLabel) { m_currentLabel->removeFromParent(); m_currentLabel=nullptr; }
    if (m_bestLabel) { m_bestLabel->removeFromParent(); m_bestLabel=nullptr; }
    if (m_sessionBestLabel) { m_sessionBestLabel->removeFromParent(); m_sessionBestLabel=nullptr; }
    if (m_attemptsLabel) { m_attemptsLabel->removeFromParent(); m_attemptsLabel=nullptr; }
    if (m_jumpsLabel) { m_jumpsLabel->removeFromParent(); m_jumpsLabel=nullptr; }
    if (m_playtimeLabel) { m_playtimeLabel->removeFromParent(); m_playtimeLabel=nullptr; }
    if (m_sessionTimeLabel) { m_sessionTimeLabel->removeFromParent(); m_sessionTimeLabel=nullptr; }
    if (m_avgLabel) { m_avgLabel->removeFromParent(); m_avgLabel=nullptr; }
    if (m_consistencyLabel) { m_consistencyLabel->removeFromParent(); m_consistencyLabel=nullptr; }

    float s = Mod::get()->getSettingValue<bool>("compact-mode") ? 0.35f : 0.42f;
    m_currentLabel = createLabel("NOW: 0.00%", s);
    m_bestLabel = createLabel("BEST: 0%", s);
    m_sessionBestLabel = createLabel("SESS BEST: 0%", s);
    m_attemptsLabel = createLabel("ATT: 0 (0)", s);
    m_jumpsLabel = createLabel("JUMPS: 0", s);
    m_playtimeLabel = createLabel("TIME: 0s", s);
    m_sessionTimeLabel = createLabel("SESS: 0s", s);
    m_avgLabel = createLabel("AVG(50): 0%", s);
    m_consistencyLabel = createLabel("CONS: -", 0.32f);

    layoutLabels();
}

void StatsHUD::layoutLabels() {
    bool compact = Mod::get()->getSettingValue<bool>("compact-mode");
    float lineH = compact ? 12.f : 14.f;
    float y = 0;
    float x = 5;

    std::vector<CCLabelBMFont*> order;
    if (Mod::get()->getSettingValue<bool>("show-current")) order.push_back(m_currentLabel);
    if (Mod::get()->getSettingValue<bool>("show-best")) order.push_back(m_bestLabel);
    if (Mod::get()->getSettingValue<bool>("show-session-best")) order.push_back(m_sessionBestLabel);
    if (Mod::get()->getSettingValue<bool>("show-attempts")) order.push_back(m_attemptsLabel);
    if (Mod::get()->getSettingValue<bool>("show-jumps")) order.push_back(m_jumpsLabel);
    if (Mod::get()->getSettingValue<bool>("show-playtime")) order.push_back(m_playtimeLabel);
    if (Mod::get()->getSettingValue<bool>("show-session-time")) order.push_back(m_sessionTimeLabel);
    if (Mod::get()->getSettingValue<bool>("show-average")) order.push_back(m_avgLabel);
    if (Mod::get()->getSettingValue<bool>("show-consistency")) order.push_back(m_consistencyLabel);

    float maxW = 0;
    for (auto* lbl : order) {
        if (!lbl) continue;
        lbl->setPosition({x, y});
        lbl->setVisible(true);
        // estimate width
        float w = lbl->getScaledContentSize().width;
        if (w > maxW) maxW = w;
        y -= lineH;
    }

    // hide rest
    std::vector<CCLabelBMFont*> all = {m_currentLabel,m_bestLabel,m_sessionBestLabel,m_attemptsLabel,m_jumpsLabel,m_playtimeLabel,m_sessionTimeLabel,m_avgLabel,m_consistencyLabel};
    for (auto* lbl : all) {
        if (std::find(order.begin(), order.end(), lbl)==order.end() && lbl) lbl->setVisible(false);
    }

    float totalH = (float)order.size() * lineH + 10 + (Mod::get()->getSettingValue<bool>("show-progress-bar") ? 10 : 0);
    float totalW = maxW + 10;
    if (totalW < 180) totalW = 180;
    if (totalW > 380) totalW = 380;
    if (totalH < 20) totalH = 20;

    if (m_bg) m_bg->setContentSize({totalW, totalH + 4});

    // bar position
    if (m_barBg && Mod::get()->getSettingValue<bool>("show-progress-bar")) {
        m_barBg->setVisible(true);
        m_barBg->setPosition({5, y + lineH - 4});
        m_barBg->setContentSize({totalW - 10, 6});
        if (auto bgCol = dynamic_cast<CCLayerColor*>(m_barBg->getChildren()->objectAtIndex(0))) {
            bgCol->setContentSize({totalW - 10, 6});
        }
    } else if (m_barBg) {
        m_barBg->setVisible(false);
    }

    // adjust own content size for positioning
    this->setContentSize({totalW, totalH});
}

void StatsHUD::updatePosition() {
    auto winSize = CCDirector::sharedDirector()->getWinSize();
    std::string pos = Mod::get()->getSettingValue<std::string>("hud-position");
    float scale = Mod::get()->getSettingValue<double>("hud-scale");
    this->setScale(scale);

    CCPoint p;
    if (pos == "Top Left") {
        p = {winSize.width * 0.02f, winSize.height * 0.98f};
        this->setAnchorPoint({0,1});
    } else if (pos == "Top Right") {
        p = {winSize.width * 0.98f, winSize.height * 0.98f};
        this->setAnchorPoint({1,1});
    } else if (pos == "Bottom Left") {
        p = {winSize.width * 0.02f, winSize.height * 0.12f};
        this->setAnchorPoint({0,0});
    } else if (pos == "Bottom Right") {
        p = {winSize.width * 0.98f, winSize.height * 0.12f};
        this->setAnchorPoint({1,0});
    } else { // Custom
        float xPct = Mod::get()->getSettingValue<double>("hud-x");
        float yPct = Mod::get()->getSettingValue<double>("hud-y");
        p = {winSize.width * xPct, winSize.height * yPct};
        this->setAnchorPoint({0,1});
    }

    // offset based on anchor
    this->setPosition(p);

    // apply bg opacity
    int opacity = Mod::get()->getSettingValue<int64_t>("bg-opacity");
    if (m_bg) m_bg->setOpacity((GLubyte)(opacity * 2.55f));
}

void StatsHUD::applySettings() {
    layoutLabels();
    updatePosition();
}

void StatsHUD::updateHUD(float currentPercent, int runJumps, double runTime) {
    if (!m_playLayer) return;
    auto mgr = StatsManager::get();
    if (!mgr->has(m_levelKey)) return;
    auto& stats = mgr->getOrCreate(m_levelKey);
    auto& session = mgr->getSession(m_levelKey);

    int decimals = Mod::get()->getSettingValue<int64_t>("decimals");
    bool coloredBest = Mod::get()->getSettingValue<bool>("colored-best");

    // Current
    if (m_currentLabel) {
        m_currentLabel->setString(fmt::format("NOW: {} ({}s)", Utils::formatPercent(currentPercent, decimals), Utils::formatTimeCompact(runTime)).c_str());
    }

    // Best
    if (m_bestLabel) {
        std::string txt = fmt::format("BEST: {}", Utils::formatPercent(stats.bestPercent, decimals));
        m_bestLabel->setString(txt.c_str());
        if (coloredBest && currentPercent > stats.bestPercent - 0.01f && stats.bestPercent > 1.f) {
            m_bestLabel->setColor(ccc3(100,255,100));
        } else {
            m_bestLabel->setColor(ccc3(255,255,255));
        }
    }

    if (m_sessionBestLabel) {
        m_sessionBestLabel->setString(fmt::format("SESS BEST: {}", Utils::formatPercent(session.best, decimals)).c_str());
    }

    if (m_attemptsLabel) {
        m_attemptsLabel->setString(fmt::format("ATT: {} (sess {})", stats.totalAttempts, session.attempts).c_str());
    }

    if (m_jumpsLabel) {
        m_jumpsLabel->setString(fmt::format("JUMPS: {} (total {})", runJumps, stats.totalJumps).c_str());
    }

    if (m_playtimeLabel) {
        m_playtimeLabel->setString(fmt::format("TIME: {}", Utils::formatTime(stats.totalPlaytime, true)).c_str());
    }

    if (m_sessionTimeLabel) {
        m_sessionTimeLabel->setString(fmt::format("SESS TIME: {}", Utils::formatTime(session.playtime, true)).c_str());
    }

    if (m_avgLabel) {
        float avg = stats.getAverage(50);
        float sessAvg = session.getAverage(20);
        m_avgLabel->setString(fmt::format("AVG 50: {} | S: {}", Utils::formatPercent(avg,1), Utils::formatPercent(sessAvg,1)).c_str());
    }

    if (m_consistencyLabel) {
        // calculate consistency for thresholds
        float c25 = stats.getConsistency(25.f);
        float c50 = stats.getConsistency(50.f);
        float c75 = stats.getConsistency(75.f);
        float c90 = stats.getConsistency(90.f);
        m_consistencyLabel->setString(fmt::format("CONS:25%:{:.0f}% 50%:{:.0f}% 75%:{:.0f}% 90%:{:.0f}%", c25,c50,c75,c90).c_str());
    }

    // progress bar
    if (m_barBg && m_barFill && Mod::get()->getSettingValue<bool>("show-progress-bar")) {
        float barW = m_barBg->getContentSize().width;
        float fillW = barW * (currentPercent / 100.f);
        if (fillW < 0) fillW = 0;
        if (fillW > barW) fillW = barW;
        if (auto col = dynamic_cast<CCLayerColor*>(m_barFill)) {
            col->setContentSize({fillW, 6});
        }
        // color based on progress vs best
        if (currentPercent > stats.bestPercent && stats.bestPercent > 1.f) {
            if (auto col = dynamic_cast<CCLayerColor*>(m_barFill)) col->setColor(ccc3(0,255,150));
        } else {
            if (auto col = dynamic_cast<CCLayerColor*>(m_barFill)) col->setColor(ccc3(0,160,255));
        }
    }

    m_lastCurrent = currentPercent;
}

void StatsHUD::showNewBest(float percent) {
    if (!Mod::get()->getSettingValue<bool>("colored-best")) return;
    // simple animation: scale and color
    this->runAction(CCSequence::create(
        CCScaleTo::create(0.1f, Mod::get()->getSettingValue<double>("hud-scale") * 1.15f),
        CCScaleTo::create(0.1f, Mod::get()->getSettingValue<double>("hud-scale")),
        nullptr
    ));
    if (m_currentLabel) {
        m_currentLabel->setColor(ccc3(80,255,120));
        m_currentLabel->runAction(CCTintTo::create(0.8f, 255,255,255));
    }
}

} // namespace progressive
