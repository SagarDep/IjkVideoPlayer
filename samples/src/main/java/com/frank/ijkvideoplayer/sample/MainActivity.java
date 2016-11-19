package com.frank.ijkvideoplayer.sample;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.frank.ijkvideoplayer.widget.media.IjkVideoStreamBean;
import com.frank.ijkvideoplayer.widget.media.IjkVideoView;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {

    private IjkVideoView mVideoView;
    private boolean mSubscribed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        String url1 = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f30.mp4";
        String url2 = "http://pl.youku.com/playlist/m3u8?vid=454701566&time=1479206476&ts=1479206476&ctype=12&token=2509&keyframe=0&sid=547920641881112285ee8&ev=1&type=hd2&ep=dyaTE0uNUMoB7SveiT8bZSTnISIHXPoP8hiFgNdkBtQmTei2&oip=2096835317";
        List<IjkVideoStreamBean> ijkVideoStreamBeanList = new ArrayList<>();
        IjkVideoStreamBean stream1 = new IjkVideoStreamBean();
        stream1.setName(getString(R.string.hd_720));
        stream1.setUri(url1);
        stream1.setLive(false);
        ijkVideoStreamBeanList.add(stream1);
        IjkVideoStreamBean stream2 = new IjkVideoStreamBean();
        stream2.setName(getString(R.string.hd_1080));
        stream2.setUri(url2);
        stream2.setLive(true);
        ijkVideoStreamBeanList.add(stream2);
        mVideoView.setTitle(getString(R.string.app_name));
        mVideoView.setVideoStream(ijkVideoStreamBeanList);
        mVideoView.setStreamListVisible(false);
        mVideoView.setTopFullscreenVisible(false);
        mVideoView.showSubscribeButton(getString(R.string.subscribe));
        mVideoView.setSubscribeBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSubscribed = !mSubscribed;
                if (mSubscribed) {
                    mVideoView.showSubscribeButton(getString(R.string.unsubscribe));
                } else {
                    mVideoView.showSubscribeButton(getString(R.string.subscribe));
                }
            }
        });
//        mVideoView.setOnlyFullScreen(true);
        mVideoView.setOnOrientationChangedListener(new IjkVideoView.OnOrientationChangedListener() {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    mVideoView.setStreamListVisible(false);
                    mVideoView.setTopFullscreenVisible(false);
                    mVideoView.setBottomFullscreenVisible(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        View decorView = getWindow().getDecorView();
                        decorView.setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_VISIBLE);
                    }
                } else {
                    mVideoView.setBottomFullscreenVisible(false);
                    mVideoView.setStreamListVisible(true);
                    mVideoView.setTopFullscreenVisible(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        View decorView = getWindow().getDecorView();
                        decorView.setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                }
            }
        });
        mVideoView.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVideoView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVideoView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.destroy();
    }
}
