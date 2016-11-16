package com.frank.ijkvideoplayer.sample;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.frank.ijkvideoplayer.widget.media.IjkVideoStreamBean;
import com.frank.ijkvideoplayer.widget.media.IjkVideoView;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {

    private IjkVideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        String url1 = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f30.mp4";
        String url2 = "http://hdl1a.douyucdn.cn/live/58428ruL0AF7ICZb.flv?wsAuth=68793ca532f7350e8c8271ddfbb996fc&token=cpn-dotamax-0-58428-1098654df0a8d416edc9b48b64d2ec93&logo=0&expire=0";
        List<IjkVideoStreamBean> ijkVideoStreamBeanList = new ArrayList<>();
        IjkVideoStreamBean stream1 = new IjkVideoStreamBean();
        stream1.setName("流畅");
        stream1.setUri(url1);
        stream1.setLive(false);
        ijkVideoStreamBeanList.add(stream1);
        IjkVideoStreamBean stream2 = new IjkVideoStreamBean();
        stream2.setName("高清");
        stream2.setUri(url2);
        stream2.setLive(true);
        ijkVideoStreamBeanList.add(stream2);
        mVideoView.setTitle(getString(R.string.app_name));
        mVideoView.setVideoStream(ijkVideoStreamBeanList);
        mVideoView.hideStreamListView();
        mVideoView.hideTopFullscreenView();
//        mVideoView.setOnlyFullScreen(true);
        mVideoView.setOnOrientationChangedListener(new IjkVideoView.OnOrientationChangedListener() {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    mVideoView.hideStreamListView();
                    mVideoView.hideTopFullscreenView();
                    mVideoView.showBottomFullscreenView();
                } else {
                    mVideoView.hideBottomFullscreenView();
                    mVideoView.showStreamListView();
                    mVideoView.showTopFullscreenView();
                }
            }
        });
        mVideoView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVideoView.onStop();
    }

}
