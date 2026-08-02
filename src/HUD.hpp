#pragma once
#include <Geode/Geode.hpp>
#include "StatsManager.hpp"
#include <Geode/binding/PlayLayer.hpp>

namespace progressive {

class StatsHUD : public cocos2d::CCLayer {
public:
    static StatsHUD* create(PlayLayer* playLayer);
    bool init(PlayLayer* playLayer);

    void updateHUD(float currentPercent, int runJumps, double runTime);
    void updatePosition();
    void applySettings();
    void showNewBest(float percent);

private:
    PlayLayer* m_playLayer = nullptr;
    cocos2d::CCLayerColor* m_bg = nullptr;
    cocos2d::CCLabelBMFont* m_currentLabel = nullptr;
    cocos2d::CCLabelBMFont* m_bestLabel = nullptr;
    cocos2d::CCLabelBMFont* m_sessionBestLabel = nullptr;
    cocos2d::CCLabelBMFont* m_attemptsLabel = nullptr;
    cocos2d::CCLabelBMFont* m_jumpsLabel = nullptr;
    cocos2d::CCLabelBMFont* m_playtimeLabel = nullptr;
    cocos2d::CCLabelBMFont* m_sessionTimeLabel = nullptr;
    cocos2d::CCLabelBMFont* m_avgLabel = nullptr;
    cocos2d::CCLabelBMFont* m_consistencyLabel = nullptr;
    cocos2d::CCLabelBMFont* m_progressTitle = nullptr;

    cocos2d::CCNode* m_barBg = nullptr;
    cocos2d::CCNode* m_barFill = nullptr;

    std::string m_levelKey;
    float m_lastCurrent = -1.f;

    void rebuildLabels();
    cocos2d::CCLabelBMFont* createLabel(const std::string& text, float scale = 0.45f, cocos2d::CCPoint anchor = {0,1});
    void layoutLabels();
};

} // namespace progressive
