package com.nyrds.pixeldungeon.support;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.RewardedVideoCallbacks;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.watabou.noosa.Game;
import com.watabou.noosa.InterstitialPoint;
import com.watabou.pixeldungeon.RemixedDungeon;

class AppodealRewardVideoProvider implements AdsUtilsCommon.IRewardVideoProvider {

    private static final String APPODEAL_REWARD_VIDEO = "appodeal reward video";
    private static InterstitialPoint returnTo;

    static {
        init();
    }

    public static void init() {

        Game.instance().runOnUiThread(() -> {
            AppodealAdapter.init();
            EventCollector.startTrace(APPODEAL_REWARD_VIDEO);

            Appodeal.cache(RemixedDungeon.instance(), Appodeal.REWARDED_VIDEO);
            Appodeal.setAutoCache(Appodeal.REWARDED_VIDEO, true);

            Appodeal.setRewardedVideoCallbacks(new RewardedVideoCallbacks() {

                @Override
                public void onRewardedVideoLoaded(boolean b) {
                    EventCollector.stopTrace(APPODEAL_REWARD_VIDEO, APPODEAL_REWARD_VIDEO, "ok", "");
                }

                @Override
                public void onRewardedVideoFailedToLoad() {
                    EventCollector.stopTrace(APPODEAL_REWARD_VIDEO, APPODEAL_REWARD_VIDEO, "fail", "");
                }

                @Override
                public void onRewardedVideoShown() {
                }

                @Override
                public void onRewardedVideoFinished(double v, String s) {

                }

                @Override
                public void onRewardedVideoClosed(final boolean finished) {
                    returnTo.returnToWork(finished);
                }

                @Override
                public void onRewardedVideoExpired() {
                }

                @Override
                public void onRewardedVideoClicked() {
                }
            });
        });
    }


    @Override
    public boolean isReady() {
        return Appodeal.isLoaded(Appodeal.REWARDED_VIDEO);
    }

    @Override
    public void showRewardVideo(InterstitialPoint ret) {
        returnTo = ret;
        Appodeal.show(RemixedDungeon.instance(), Appodeal.REWARDED_VIDEO);
    }
}
