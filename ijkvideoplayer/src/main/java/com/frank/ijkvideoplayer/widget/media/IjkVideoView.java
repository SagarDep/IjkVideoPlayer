/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frank.ijkvideoplayer.widget.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.frank.ijkvideoplayer.R;
import com.frank.ijkvideoplayer.widget.setting.Settings;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.exo.IjkExoMediaPlayer;
import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.TextureMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class IjkVideoView extends FrameLayout implements View.OnTouchListener, View.OnClickListener {

    private static final String TAG = "IjkVideoView";

    private static final int MSG_FADE_OUT = 1;
    private static final int MSG_SHOW_PROGRESS = 2;
    private static final int MEDIA_CONTROLLER_TIMEOUT = 3000;

    // settable by the client
    private Uri mUri;
    private Map<String, String> mHeaders;

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private IRenderView.ISurfaceHolder mSurfaceHolder = null;
    private IMediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoInitHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mCurrentAspectRatio;
    private int mVideoRotationDegree;
    private int mCurrentBufferPercentage;
    private int mSeekWhenPrepared;  // recording the seek position while preparing

    /** Subtitle rendering widget overlaid on top of the video. */
    // private RenderingWidget mSubtitleWidget;

    /**
     * Listener for changes to subtitle data, used to redraw when needed.
     */
    // private RenderingWidget.OnChangedListener mSubtitlesChangedListener;

    private Context mAppContext;
    private Activity mActivity;
    private Settings mSettings;
    private IRenderView mRenderView;
    private int mVideoSarNum;
    private int mVideoSarDen;

    private long mPrepareStartTime = 0;
    private long mPrepareEndTime = 0;
    private long mSeekStartTime = 0;
    private long mSeekEndTime = 0;

    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;
    private OrientationEventListener mOrientationEventListener;
    private OnOrientationChangedListener mOnOrientationChangedListener;

    private boolean mEnableBackgroundPlay;
    private boolean mEnableLogging;
    private boolean mForceFullScreen;
    private boolean mForcePortrait;
    private boolean mLockRotation;
    private boolean mOnlyFullScreen;
    private boolean mMediaControllerShowing;
    private boolean mMediaControllerDragging;
    private boolean mLive;
    private int mTouchSlop;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private AudioManager mAudioManager;
    private int mVolume;
    private int mMaxVolume;
    private float mBrightness;
    private List<IjkVideoStreamBean> mIjkVideoStreamList = new ArrayList<>();
    private int mCurrentStreamIndex;
    private int mCurrentPosition;

    private View mControlPanelLayout;
    private View mMediaControllerLayout;
    private ImageView iv_back;
    private TextView tv_title;
    private RelativeLayout rl_stream_list_container;
    private ImageView iv_top_fullscreen;
    private ImageView iv_bottom_fullscreen;
    private ImageView iv_lock_rotation;
    private SeekBar sb_progress;
    private TextView tv_current_time;
    private TextView tv_end_time;
    private ImageView iv_pause;
    private LinearLayout ll_volume_brightness_container;
    private ImageView iv_volume_brightness;
    private TextView tv_volume_brightness;
    private LinearLayout ll_loading_container;
    private ProgressBar pb_loading;
    private TextView tv_loading_description;
    private LinearLayout ll_error_container;
    private TextView tv_error_message;
    private Button btn_error_action;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case MSG_FADE_OUT:
                    hideMediaController();
                    break;
                case MSG_SHOW_PROGRESS:
                    pos = setMediaControllerProgress();
                    if (!mMediaControllerDragging && mMediaControllerShowing && isPlaying()) {
                        msg = obtainMessage(MSG_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };

    public IjkVideoView(Context context) {
        super(context);
        mActivity = (Activity) context;
        initVideoView();
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (Activity) context;
        initVideoView();
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivity = (Activity) context;
        initVideoView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mActivity = (Activity) context;
        initVideoView();
    }

    // REMOVED: onMeasure
    // REMOVED: onInitializeAccessibilityEvent
    // REMOVED: onInitializeAccessibilityNodeInfo
    // REMOVED: resolveAdjustedSize

    private void initVideoView() {

        mAppContext = mActivity.getApplicationContext();
        mSettings = new Settings(mAppContext);
        mEnableLogging = mSettings.getEnableLogging();
        mCurrentAspectRatio = Settings.LAYOUT_FILL_PARENT;
        mScreenWidth = mActivity.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mActivity.getResources().getDisplayMetrics().heightPixels;
        mVideoWidth = 0;
        mVideoHeight = 0;
        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mVideoInitHeight <= 0) {
                    mVideoInitHeight = getHeight();
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        mTouchSlop = ViewConfiguration.get(mActivity).getScaledTouchSlop();
        mAudioManager = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        initBackground();
        initRenders();
        initControlPanel();
        initMediaController();

        setOrientationEventListener(new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (mLockRotation
                        || orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }
                int requestOrientation;
                if (!mForceFullScreen && (((orientation >= 0) && (orientation <= 45)) || (orientation > 315))) {
                    mForcePortrait = false;
                    requestOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else if (!mForcePortrait && (orientation > 45) && (orientation <= 135)) {
                    mForceFullScreen = false;
                    requestOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                } else if (!mForceFullScreen && ((orientation > 135) && (orientation <= 225))) {
                    mForcePortrait = false;
                    requestOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else if (!mForcePortrait && (orientation > 225) && (orientation <= 315)) {
                    mForceFullScreen = false;
                    requestOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    requestOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
                final boolean portrait = (requestOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || requestOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                if (!mForceFullScreen && !mForcePortrait && (!mOnlyFullScreen || !portrait)) {
                    mActivity.setRequestedOrientation(requestOrientation);
                    if (mOnOrientationChangedListener != null) {
                        mOnOrientationChangedListener.onOrientationChanged(requestOrientation);
                    }
                }
            }
        });

        // REMOVED: getHolder().addCallback(mSHCallback);
        // REMOVED: getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        setOnTouchListener(this);
        // REMOVED: mPendingSubtitleTracks = new Vector<Pair<InputStream, MediaFormat>>();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    public void destroy() {
        mOrientationEventListener.disable();
        if (!isBackgroundPlayEnabled()) {
            stopPlayback();
            release(true);
            stopBackgroundPlay();
        } else {
            enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        final boolean portrait = (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                || newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        post(new Runnable() {
            @Override
            public void run() {
                toggleWindowFlags(!portrait);
                if (portrait) {
                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.height = mVideoInitHeight;
                    setLayoutParams(layoutParams);
                } else {
                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.height = Math.min(mScreenWidth, mScreenHeight);
                    setLayoutParams(layoutParams);
                }
                updateFullScreenButton();
            }
        });
    }

    private void initBackground() {
        mEnableBackgroundPlay = mSettings.getEnableBackgroundPlay();
        if (mEnableBackgroundPlay) {
            MediaPlayerService.intentToStart(getContext());
            mMediaPlayer = MediaPlayerService.getMediaPlayer();
        }
    }

    private void initRenders() {
        int render = mSettings.getRender();
        switch (render) {
            case Settings.RENDER_TEXTURE_VIEW:
                TextureRenderView textureRenderView = new TextureRenderView(getContext());
                if (mMediaPlayer != null) {
                    textureRenderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer);
                    textureRenderView.setVideoSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                    textureRenderView.setVideoSampleAspectRatio(mMediaPlayer.getVideoSarNum(), mMediaPlayer.getVideoSarDen());
                    textureRenderView.setAspectRatio(mCurrentAspectRatio);
                }
                setRenderView(textureRenderView);
                break;
            case Settings.RENDER_SURFACE_VIEW:
                SurfaceRenderView surfaceRenderView = new SurfaceRenderView(getContext());
                setRenderView(surfaceRenderView);
                break;
            default:
                SurfaceRenderView renderView = new SurfaceRenderView(getContext());
                setRenderView(renderView);
                break;
        }
    }

    public void setRenderView(IRenderView renderView) {
        if (mRenderView != null) {
            if (mMediaPlayer != null)
                mMediaPlayer.setDisplay(null);

            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }
        if (renderView == null) {
            return;
        }
        mRenderView = renderView;
        renderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0)
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        if (mVideoSarNum > 0 && mVideoSarDen > 0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
        View renderUIView = mRenderView.getView();
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private void initControlPanel() {
        if (mControlPanelLayout != null) {
            removeView(mControlPanelLayout);
        }

        LayoutInflater inflate = (LayoutInflater) mAppContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mControlPanelLayout = inflate.inflate(R.layout.ijk_video_player_control_panel, this, false);

        ll_volume_brightness_container = (LinearLayout) mControlPanelLayout.findViewById(R.id.ll_volume_brightness_container);
        iv_volume_brightness = (ImageView) mControlPanelLayout.findViewById(R.id.iv_volume_brightness);
        tv_volume_brightness = (TextView) mControlPanelLayout.findViewById(R.id.tv_volume_brightness);

        ll_loading_container = (LinearLayout) mControlPanelLayout.findViewById(R.id.ll_loading_container);
        pb_loading = (ProgressBar) mControlPanelLayout.findViewById(R.id.pb_loading);
        tv_loading_description = (TextView) mControlPanelLayout.findViewById(R.id.tv_loading_description);

        ll_error_container = (LinearLayout) mControlPanelLayout.findViewById(R.id.ll_error_container);
        tv_error_message = (TextView) mControlPanelLayout.findViewById(R.id.tv_error_message);
        btn_error_action = (Button) mControlPanelLayout.findViewById(R.id.btn_error_action);

        addView(mControlPanelLayout);
    }

    private void initMediaController() {
        if (mMediaControllerLayout != null) {
            removeView(mMediaControllerLayout);
        }

        LayoutInflater inflate = (LayoutInflater) mAppContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMediaControllerLayout = inflate.inflate(R.layout.ijk_video_player_media_controller, this, false);
        iv_back = (ImageView) mMediaControllerLayout.findViewById(R.id.iv_back);
        tv_title = (TextView) mMediaControllerLayout.findViewById(R.id.tv_title);
        rl_stream_list_container = (RelativeLayout) mMediaControllerLayout.findViewById(R.id.rl_stream_list_container);
        iv_top_fullscreen = (ImageView) mMediaControllerLayout.findViewById(R.id.iv_top_fullscreen);
        iv_bottom_fullscreen = (ImageView) mMediaControllerLayout.findViewById(R.id.iv_bottom_fullscreen);
        iv_lock_rotation = (ImageView) mMediaControllerLayout.findViewById(R.id.iv_lock_rotation);
        sb_progress = (SeekBar) mMediaControllerLayout.findViewById(R.id.sb_progress);
        tv_current_time = (TextView) mMediaControllerLayout.findViewById(R.id.tv_current_time);
        tv_end_time = (TextView) mMediaControllerLayout.findViewById(R.id.tv_end_time);
        iv_pause = (ImageView) mMediaControllerLayout.findViewById(R.id.iv_pause);

        iv_back.setOnClickListener(this);
        tv_title.setOnClickListener(this);
        iv_top_fullscreen.setOnClickListener(this);
        iv_bottom_fullscreen.setOnClickListener(this);
        iv_lock_rotation.setOnClickListener(this);
        iv_pause.setOnClickListener(this);
        sb_progress.setMax(1000);
        sb_progress.setOnSeekBarChangeListener(mPlayerProgressSeekListener);
        addView(mMediaControllerLayout);

        attachMediaController();
    }

    public void showMediaController() {
        showMediaController(MEDIA_CONTROLLER_TIMEOUT);
    }

    /**
     * 显示媒体控制器，如果timeout为0则会一直显示，否则延时timeout自动隐藏
     *
     * @param timeout 显示媒体控制器的时间，如果为0则一直显示
     */
    public void showMediaController(int timeout) {
        if (!mMediaControllerShowing) {
            setMediaControllerProgress();
            if (iv_pause != null) {
                iv_pause.setVisibility(View.VISIBLE);
                iv_pause.requestFocus();
            }
            if (ll_volume_brightness_container != null) {
                if (ll_volume_brightness_container.getVisibility() == View.VISIBLE) {
                    ll_volume_brightness_container.setVisibility(GONE);
                }
            }
            mMediaControllerLayout.setVisibility(View.VISIBLE);
            mMediaControllerShowing = true;
        }
        updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS);

        if (timeout != 0) {
            mHandler.removeMessages(MSG_FADE_OUT);
            Message msg = mHandler.obtainMessage(MSG_FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public void hideMediaController() {
        if (mMediaControllerShowing) {
            try {
                mHandler.removeMessages(MSG_SHOW_PROGRESS);
                mMediaControllerLayout.setVisibility(View.GONE);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
            mMediaControllerShowing = false;
        }
    }

    private int setMediaControllerProgress() {
        if (mMediaControllerDragging) {
            return 0;
        }
        int position = getCurrentPosition();
        int duration = getDuration();
        if (sb_progress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                sb_progress.setProgress((int) pos);
            }
            int percent = getBufferPercentage();
            sb_progress.setSecondaryProgress(percent * 10);
        }

        if (tv_end_time != null)
            tv_end_time.setText(stringForTime(duration));
        if (tv_current_time != null)
            tv_current_time.setText(stringForTime(position));

        return position;
    }

    public void setAspectRatio(int aspectRatio) {
        mCurrentAspectRatio = aspectRatio;
        if (mRenderView != null) {
            mRenderView.setAspectRatio(mCurrentAspectRatio);
        }
    }

    public void setVideoRotation(int videoRotationDegree) {
        mLockRotation = false;
        if (videoRotationDegree == 0) {
            videoRotationDegree = 90;
        } else if (videoRotationDegree == 90) {
            videoRotationDegree = 270;
        } else if (videoRotationDegree == 270) {
            videoRotationDegree = 0;
        }
        mVideoRotationDegree = videoRotationDegree;
        if (mRenderView != null) {
            mRenderView.setVideoRotation(mVideoRotationDegree);
        }
        setAspectRatio(mCurrentAspectRatio);
    }

    /**
     * 设置Video URI
     *
     * @param uri    URI
     * @param isLive 是否是直播
     */
    public void setVideoURI(String uri, boolean isLive) {
        setVideoURI(Uri.parse(uri), isLive);
    }

    /**
     * 设置Video URI
     *
     * @param uri    URI
     * @param isLive 是否是直播
     */
    public void setVideoURI(Uri uri, boolean isLive) {
        setVideoURI(uri, isLive, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param isLive  是否是直播
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, boolean isLive, Map<String, String> headers) {
        mUri = uri;
        mLive = isLive;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        if (mLive) {
            setSeekBarVisible(false);
        } else {
            setSeekBarVisible(true);
        }
        openVideo();
        requestLayout();
        invalidate();
    }

    /**
     * 设置多个可切换的视频流
     *
     * @param streamList 视频流列表，需要设置name(名字),url(路径),isLive(是否是直播)
     */
    public void setVideoStream(List<IjkVideoStreamBean> streamList) {
        mIjkVideoStreamList.clear();
        if (streamList != null) {
            mIjkVideoStreamList.addAll(streamList);
            switchStream(0);
            updateStreamList();
        }
    }

    /**
     * 设置视频流，需要设置name(名字),url(路径),isLive(是否是直播)
     */
    public void setVideoStream(IjkVideoStreamBean stream) {
        mIjkVideoStreamList.clear();
        if (stream != null) {
            mIjkVideoStreamList.add(stream);
            switchStream(0);
            updateStreamList();
        }
    }

    public void switchStream(int index) {
        if (index < 0 || index >= mIjkVideoStreamList.size()) {
            return;
        }
        for (IjkVideoStreamBean ijkVideoStreamBean :
                mIjkVideoStreamList) {
            ijkVideoStreamBean.setSelected(false);
        }
        IjkVideoStreamBean ijkVideoStreamBean = mIjkVideoStreamList.get(index);
        ijkVideoStreamBean.setSelected(true);
        mCurrentPosition = getCurrentPosition();
        mUri = Uri.parse(ijkVideoStreamBean.getUri());
        mLive = ijkVideoStreamBean.isLive();
        if (mLive) {
            setSeekBarVisible(false);
        } else {
            setSeekBarVisible(true);
        }
    }

    public void updateStreamList() {
        if (rl_stream_list_container == null) {
            return;
        }
        rl_stream_list_container.removeAllViews();
        for (int i = 0; i < mIjkVideoStreamList.size(); i++) {
            IjkVideoStreamBean ijkVideoStreamBean = mIjkVideoStreamList.get(i);
            ijkVideoStreamBean.setIndex(i);
            TextView textView = new TextView(mActivity);
            textView.setTag(ijkVideoStreamBean);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IjkVideoStreamBean streamBean = (IjkVideoStreamBean) v.getTag();
                    if (streamBean != null) {
                        int position = streamBean.getIndex();
                        if (mCurrentStreamIndex == position) {
                            return;
                        }
                        mCurrentStreamIndex = position;
                        switchStream(position);
                        tv_loading_description.setText(getResources().getString(R.string.streams_switching));
                        setLoadingDescriptionVisible(true);
                        setLoadingContainerVisible(true);
                        if (mLive) {
                            setVideoURI(mUri, true);
                        } else {
                            setVideoURI(mUri, false);
                            seekTo(mCurrentPosition);
                        }
                        refreshStreamList();
                        start();
                    }
                }
            });
            RelativeLayout.LayoutParams relativeLayoutparams = new RelativeLayout.LayoutParams(
                    MeasureHelper.dp2px(mActivity, 30), MeasureHelper.dp2px(mActivity, 20));
            relativeLayoutparams.rightMargin = MeasureHelper.dp2px(mActivity, i * 40);
            relativeLayoutparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            relativeLayoutparams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            rl_stream_list_container.addView(textView, relativeLayoutparams);
        }
        refreshStreamList();
    }

    public void refreshStreamList() {
        int childCount = rl_stream_list_container.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TextView child = (TextView) rl_stream_list_container.getChildAt(i);
            IjkVideoStreamBean ijkVideoStreamBean = (IjkVideoStreamBean) child.getTag();
            if (ijkVideoStreamBean != null) {
                child.setText(ijkVideoStreamBean.getName());
                child.setTextSize(8);
                child.setGravity(Gravity.CENTER);
                if (ijkVideoStreamBean.isSelected()) {
                    child.setTextColor(0xFFFFFFFF);
                    child.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.ijk_video_player_round_corner_white));
                } else {
                    child.setTextColor(0xFFA8A8A8);
                    child.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.ijk_video_player_round_corner_gray));
                }
            }
        }
    }

    // REMOVED: addSubtitleSource
    // REMOVED: mPendingSubtitleTracks

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try {
            mMediaPlayer = createPlayer(mSettings.getPlayer());

            // TODO: create SubtitleController in MediaPlayer, but we need
            // a context for the subtitle renderers
            final Context context = getContext();
            // REMOVED: SubtitleController

            // REMOVED: mAudioSession
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mCurrentBufferPercentage = 0;
            String scheme = mUri.getScheme();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    mSettings.getUsingMediaDataSource() &&
                    (TextUtils.isEmpty(scheme) || scheme.equalsIgnoreCase("file"))) {
                IMediaDataSource dataSource = new FileMediaDataSource(new File(mUri.toString()));
                mMediaPlayer.setDataSource(dataSource);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            } else {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mPrepareStartTime = System.currentTimeMillis();
            mMediaPlayer.prepareAsync();

            // REMOVED: mPendingSubtitleTracks

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (Exception e) {
            e.printStackTrace();
            log("Unable to open content: " + mUri);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private void attachMediaController() {
        if (mMediaControllerLayout != null) {
            mMediaControllerShowing = true;
            hideMediaController();
            setMediaControllerEnabled(isInPlaybackState());
        }
    }

    private final SeekBar.OnSeekBarChangeListener mPlayerProgressSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            showMediaController(3600000);

            mMediaControllerDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(MSG_SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            long duration = getDuration();
            long newPosition = (duration * progress) / 1000L;
            seekTo((int) newPosition);
            if (tv_current_time != null) {
                tv_current_time.setText(stringForTime((int) newPosition));
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mMediaControllerDragging = false;
            setMediaControllerProgress();
            updatePausePlay();
            showMediaController(MEDIA_CONTROLLER_TIMEOUT);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS);
        }
    };

    private OnClickListener mReplayClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            replay();
        }
    };

    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IMediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    mVideoSarNum = mp.getVideoSarNum();
                    mVideoSarDen = mp.getVideoSarDen();
                    if (mVideoWidth != 0 && mVideoHeight != 0) {
                        if (mRenderView != null) {
                            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                            mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                        }
                        // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                        requestLayout();
                    }
                }
            };

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            mPrepareEndTime = System.currentTimeMillis();
            mCurrentState = STATE_PREPARED;

            // Get the capabilities of the player for this stream
            // REMOVED: Metadata

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaControllerLayout != null) {
                setMediaControllerEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                    if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                        // We didn't actually change the size (it was already at the size
                        // we need), so we won't get a "surface changed" callback, so
                        // start the video here instead of in the callback.
                        if (mTargetState == STATE_PLAYING) {
                            start();
                            setLoadingContainerVisible(false);
                            if (mMediaControllerLayout != null) {
                                showMediaController();
                            }
                        } else if (!isPlaying() &&
                                (seekToPosition != 0 || getCurrentPosition() > 0)) {
                            if (mMediaControllerLayout != null) {
                                // Show the media controls when we're paused into a video and make 'em stick.
                                showMediaController(0);
                            }

                        }
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                    if (mMediaControllerLayout != null) {
                        showMediaController(0);
                    }
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                }
            };

    private IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
                    if (mOnInfoListener != null) {
                        mOnInfoListener.onInfo(mp, arg1, arg2);
                    }
                    switch (arg1) {
                        case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            // 视频日志跟踪
                            log("MEDIA_INFO_VIDEO_TRACK_LAGGING");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            // 开始视频渲染
                            log("MEDIA_INFO_VIDEO_RENDERING_START");
                            setLoadingContainerVisible(false);
                            setErrorContainerVisible(false);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                            // 开始缓冲
                            log("MEDIA_INFO_BUFFERING_START");
                            setLoadingDescriptionVisible(false);
                            setLoadingContainerVisible(true);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                            // 缓冲结束
                            log("MEDIA_INFO_BUFFERING_END");
                            setLoadingContainerVisible(false);
                            break;
                        case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                            // 网络带宽
                            log("MEDIA_INFO_NETWORK_BANDWIDTH" + arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                            // 交叉存取异常
                            log("MEDIA_INFO_BAD_INTERLEAVING");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                            // 不可拖动
                            log("MEDIA_INFO_NOT_SEEKABLE");
                            break;
                        case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                            // meta数据更新
                            log("MEDIA_INFO_METADATA_UPDATE");
                            break;
                        case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                            // 不支持字幕
                            log("MEDIA_INFO_UNSUPPORTED_SUBTITLE");
                            break;
                        case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                            // 字幕请求超时
                            log("MEDIA_INFO_SUBTITLE_TIMED_OUT");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                            // 视频方向更改
                            log("MEDIA_INFO_VIDEO_ROTATION_CHANGED:" + arg2);
                            mVideoRotationDegree = arg2;
                            if (mRenderView != null) {
                                mRenderView.setVideoRotation(arg2);
                            }
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                            // 开始音频渲染
                            log("MEDIA_INFO_AUDIO_RENDERING_START:");
                            setLoadingContainerVisible(false);
                            setErrorContainerVisible(false);
                            break;
                    }
                    return true;
                }
            };

    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                    log("Error: " + framework_err + "," + impl_err);
                    mCurrentState = STATE_ERROR;
                    mTargetState = STATE_ERROR;
                    if (mMediaControllerLayout != null) {
                        hideMediaController();
                    }
                    /* If an error handler has been supplied, use it and finish. */
                    if (mOnErrorListener != null) {
                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                            return true;
                        }
                    }
                    switch (framework_err) {
                        case IMediaPlayer.MEDIA_ERROR_UNKNOWN: // 未知错误
                        case IMediaPlayer.MEDIA_ERROR_SERVER_DIED: // 服务器错误
                        case IMediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK: // 连续播放错误
                        case IMediaPlayer.MEDIA_ERROR_IO: // IO读写错误
                        case IMediaPlayer.MEDIA_ERROR_UNSUPPORTED: // 格式不支持错误
                            tv_error_message.setText(getResources().getString(R.string.error_unknown));
                            btn_error_action.setText(getResources().getString(R.string.retrieve));
                            btn_error_action.setOnClickListener(mReplayClickListener);
                            setErrorContainerVisible(true);
                            break;
                        case IMediaPlayer.MEDIA_ERROR_TIMED_OUT: // 超时错误
                            tv_error_message.setText(getResources().getString(R.string.error_timeout));
                            btn_error_action.setText(getResources().getString(R.string.retrieve));
                            btn_error_action.setOnClickListener(mReplayClickListener);
                            setErrorContainerVisible(true);
                            break;
                        default:
                            tv_error_message.setText(getResources().getString(R.string.error_unknown));
                            btn_error_action.setText(getResources().getString(R.string.retrieve));
                            btn_error_action.setOnClickListener(mReplayClickListener);
                            setErrorContainerVisible(true);
                            break;
                    }
                    return true;
                }
            };

    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new IMediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                }
            };

    private IMediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new IMediaPlayer.OnSeekCompleteListener() {

        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            mSeekEndTime = System.currentTimeMillis();
        }
    };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }

    // REMOVED: mSHCallback
    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null)
            return;

        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                log("onSurfaceChanged: unmatched render callback\n");
                return;
            }

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                log("onSurfaceCreated: unmatched render callback\n");
                return;
            }

            mSurfaceHolder = holder;
            if (mMediaPlayer != null)
                bindSurfaceHolder(mMediaPlayer, holder);
            else
                openVideo();
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                log("onSurfaceDestroyed: unmatched render callback\n");
                return;
            }

            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            // REMOVED: if (mMediaController != null) mMediaController.hide();
            // REMOVED: release(true);
            releaseWithoutStop();
        }
    };

    public void releaseWithoutStop() {
        if (mMediaPlayer != null)
            mMediaPlayer.setDisplay(null);
    }

    /*
     * release the media player in any state
     */
    public void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            // REMOVED: mPendingSubtitleTracks.clear();
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaControllerLayout != null) {
            toggleMediaControllerVisibility();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaControllerLayout != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    stop();
                    showMediaController();
                } else {
                    start();
                    hideMediaController();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    hideMediaController();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    stop();
                    showMediaController();
                }
                return true;
            } else {
                toggleMediaControllerVisibility();
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK
                && !mOnlyFullScreen
                && (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)) {
            mForcePortrait = true;
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (mOnOrientationChangedListener != null) {
                mOnOrientationChangedListener.onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaControllerLayout != null) {
            toggleMediaControllerVisibility();
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialMotionY = event.getY();
                mInitialMotionX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                mMediaControllerDragging = true;
                Display disp = mActivity.getWindowManager().getDefaultDisplay();
                int windowWidth = disp.getWidth();
                int windowHeight = disp.getHeight();
                final float y = event.getY(), x = event.getX();
                final float diffY, absDiffY;
                final float diffX, absDiffX;
                diffY = mInitialMotionY - y;
                absDiffY = Math.abs(diffY);
                diffX = mInitialMotionX - x;
                absDiffX = Math.abs(diffX);
                if (absDiffX > mTouchSlop && absDiffX > absDiffY) {
                    if (!mLive) {
                        onProgressSlide((-diffX / getWidth()));
                    }
                    mMediaControllerDragging = false;
                } else if (absDiffY > mTouchSlop) {
                    if (mInitialMotionX > windowWidth * 3.0 / 5) {
                        onVolumeSlide(diffY / windowHeight);
                    } else if (mInitialMotionX < windowWidth * 2.0 / 5) {
                        onBrightnessSlide(diffY / windowHeight);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mMediaControllerDragging = false;
                toggleMediaControllerVisibility();
                setVolumeBrightnessVisible(false);
                mVolume = -1;
                mBrightness = -1f;
                break;
        }
        return true;
    }

    private void onVolumeSlide(float percent) {
        if (mVolume == -1) {
            mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mVolume < 0) {
                mVolume = 0;
            }
        }
        int index = (int) (percent * mMaxVolume) + mVolume;
        if (index > mMaxVolume) {
            index = mMaxVolume;
        } else if (index < 0) {
            index = 0;
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }
        if (ll_volume_brightness_container != null) {
            iv_volume_brightness.setImageResource(R.drawable.ic_volume);
            tv_volume_brightness.setText(s);
            setVolumeBrightnessVisible(true);
        }
    }

    private void onBrightnessSlide(float percent) {
        if (mBrightness < 0) {
            mBrightness = mActivity.getWindow().getAttributes().screenBrightness;
            if (mBrightness <= 0.00f) {
                mBrightness = 0.50f;
            } else if (mBrightness < 0.01f) {
                mBrightness = 0.01f;
            }
        }
        WindowManager.LayoutParams lpa = mActivity.getWindow().getAttributes();
        lpa.screenBrightness = mBrightness + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        mActivity.getWindow().setAttributes(lpa);
        if (ll_volume_brightness_container != null) {
            iv_volume_brightness.setImageResource(R.drawable.ic_brightness);
            tv_volume_brightness.setText(getPercent(lpa.screenBrightness));
            setVolumeBrightnessVisible(true);
        }
    }

    private void onProgressSlide(float progress) {
        int position = getCurrentPosition();
        long duration = getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * progress);
        long newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition = 0;
            delta = -position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0 && sb_progress != null) {
            if (newPosition >= 0) {
                seekTo((int) newPosition);
            }
            if (tv_current_time != null) {
                tv_current_time.setText(stringForTime((int) newPosition));
            }
            if (showDelta > 0) {
                iv_volume_brightness.setImageResource(android.R.drawable.ic_media_ff);
                tv_volume_brightness.setText("+" + " " + showDelta + " " + "s");
            } else if (showDelta < 0) {
                iv_volume_brightness.setImageResource(android.R.drawable.ic_media_rew);
                tv_volume_brightness.setText(String.valueOf(showDelta) + " " + "s");
            }
            setVolumeBrightnessVisible(true);
            setMediaControllerProgress();
            updatePausePlay();
            showMediaController(MEDIA_CONTROLLER_TIMEOUT);
            mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.iv_back || viewId == R.id.tv_title) {
            if (!mOnlyFullScreen &&
                    (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            || getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)) {
                mForcePortrait = true;
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                if (mOnOrientationChangedListener != null) {
                    mOnOrientationChangedListener.onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else {
                mActivity.onBackPressed();
            }
        } else if (viewId == R.id.iv_top_fullscreen || viewId == R.id.iv_bottom_fullscreen) {
            toggleFullScreen();
        } else if (viewId == R.id.iv_lock_rotation) {
            toggleLockRotation();
        } else if (viewId == R.id.iv_pause) {
            togglePause();
        }
    }

    public void toggleFullScreen() {
        if (mLockRotation) {
            return;
        }
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            mForcePortrait = true;
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (mOnOrientationChangedListener != null) {
                mOnOrientationChangedListener.onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            mForceFullScreen = true;
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (mOnOrientationChangedListener != null) {
                mOnOrientationChangedListener.onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        updateFullScreenButton();
    }

    private void updateFullScreenButton() {
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            iv_top_fullscreen.setImageResource(R.drawable.ic_fullscreen_shrink);
            iv_bottom_fullscreen.setImageResource(R.drawable.ic_fullscreen_shrink);
        } else {
            iv_top_fullscreen.setImageResource(R.drawable.ic_fullscreen_stretch);
            iv_bottom_fullscreen.setImageResource(R.drawable.ic_fullscreen_stretch);
        }
    }

    private void toggleWindowFlags(boolean fullScreen) {
        if (mActivity instanceof AppCompatActivity) {
            ActionBar supportActionBar = ((AppCompatActivity) mActivity).getSupportActionBar();
            if (supportActionBar != null) {
                if (fullScreen) {
                    supportActionBar.hide();
                } else {
                    supportActionBar.show();
                }
            }
        }
        WindowManager.LayoutParams attrs = mActivity.getWindow().getAttributes();
        if (fullScreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            mActivity.getWindow().setAttributes(attrs);
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mActivity.getWindow().setAttributes(attrs);
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    public void toggleLockRotation() {
        mLockRotation = !mLockRotation;
        if (mLockRotation) {
            mOrientationEventListener.disable();
        } else {
            mOrientationEventListener.enable();
        }
        updateLockRotationButton();
    }

    private void updateLockRotationButton() {
        if (mLockRotation) {
            iv_lock_rotation.setImageResource(R.drawable.ic_rotation_locked);
        } else {
            iv_lock_rotation.setImageResource(R.drawable.ic_rotation_unlocked);
        }
    }

    public void togglePause() {
        doPauseResume();
    }

    private void doPauseResume() {
        if (isPlaying()) {
            stop();
        } else {
            start();
        }
        updatePausePlay();
    }

    private void updatePausePlay() {
        if (iv_pause == null) {
            return;
        }
        if (mCurrentState == STATE_PLAYING) {
            iv_pause.setImageResource(R.drawable.ic_pause);
        } else if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
            iv_pause.setImageResource(R.drawable.ic_replay);
        } else {
            iv_pause.setImageResource(R.drawable.ic_play);
        }
    }

    public int getScreenOrientation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    private void toggleMediaControllerVisibility() {
        if (mMediaControllerShowing) {
            hideMediaController();
        } else {
            showMediaController();
        }
    }

    public void start() {
        if (mOnlyFullScreen
                && (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                || getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)) {
            mForceFullScreen = true;
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (mOnOrientationChangedListener != null) {
                mOnOrientationChangedListener.onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
        mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS);
    }

    public void stop() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void replay() {
        setLoadingDescriptionVisible(false);
        setLoadingContainerVisible(true);
        if (mLive) {
            setVideoURI(mUri, true);
        } else {
            setVideoURI(mUri, false);
            seekTo(mCurrentPosition);
        }
        start();
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mSeekStartTime = System.currentTimeMillis();
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public IMediaPlayer createPlayer(int playerType) {
        IMediaPlayer mediaPlayer;
        switch (playerType) {
            case Settings.PLAYER_IJK_EXO_MEDIA_PLAYER:
                mediaPlayer = new IjkExoMediaPlayer(mAppContext);
                break;
            case Settings.PLAYER_ANDROID_MEDIA_PLAYER:
                mediaPlayer = new AndroidMediaPlayer();
                break;
            default:
                IjkMediaPlayer ijkMediaPlayer = null;
                if (mUri != null) {
                    ijkMediaPlayer = new IjkMediaPlayer();

                    if (mSettings.getUsingMediaCodec()) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                        if (mSettings.getUsingMediaCodecAutoRotate()) {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                        } else {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
                        }
                        if (mSettings.getMediaCodecHandleResolutionChange()) {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
                        } else {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
                        }
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
                    }

                    if (mSettings.getUsingOpenSLES()) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
                    }

                    String pixelFormat = mSettings.getPixelFormat();
                    if (TextUtils.isEmpty(pixelFormat)) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
                    }
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
                }
                mediaPlayer = ijkMediaPlayer;
                break;
        }

        if (mSettings.getEnableDetachedSurfaceTextureView()) {
            mediaPlayer = new TextureMediaPlayer(mediaPlayer);
        }

        return mediaPlayer;
    }

    //-------------------------
    // Extend: Background
    //-------------------------

    public boolean isBackgroundPlayEnabled() {
        return mEnableBackgroundPlay;
    }

    public void enterBackground() {
        MediaPlayerService.setMediaPlayer(mMediaPlayer);
    }

    public void stopBackgroundPlay() {
        MediaPlayerService.setMediaPlayer(null);
    }

    private String stringForTime(long duration) {
        long total_seconds = duration / 1000;
        long hours = total_seconds / 3600;
        long minutes = (total_seconds % 3600) / 60;
        long seconds = total_seconds % 60;
        if (duration <= 0) {
            return "--:--";
        }
        if (hours >= 100) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private String getPercent(double value) {
        NumberFormat numberFormat = NumberFormat.getPercentInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setGroupingUsed(false);
        return numberFormat.format(value);
    }

    public ITrackInfo[] getTrackInfo() {
        if (mMediaPlayer == null)
            return null;

        return mMediaPlayer.getTrackInfo();
    }

    public void selectTrack(int stream) {
        MediaPlayerCompat.selectTrack(mMediaPlayer, stream);
    }

    public void deselectTrack(int stream) {
        MediaPlayerCompat.deselectTrack(mMediaPlayer, stream);
    }

    public int getSelectedTrack(int trackType) {
        return MediaPlayerCompat.getSelectedTrack(mMediaPlayer, trackType);
    }

    private void log(String message) {
        if (mEnableLogging) {
            Log.d(TAG, message);
        }
    }

    public void setLoadingContainerVisible(boolean visible) {
        if (visible && !mMediaControllerDragging) {
            setViewVisible(ll_loading_container, true);
            setViewVisible(ll_error_container, false);
            setViewVisible(iv_pause, false);
        } else {
            setViewVisible(ll_loading_container, false);
            setViewVisible(iv_pause, true);
        }
    }

    public void setVolumeBrightnessVisible(boolean visible) {
        if (visible) {
            setViewVisible(ll_volume_brightness_container, true);
        } else {
            setViewVisible(ll_volume_brightness_container, false);
        }
    }

    public void setStreamListVisible(boolean visible) {
        setViewVisible(rl_stream_list_container, visible);
    }

    public void setTopFullscreenVisible(boolean visible) {
        setViewVisible(iv_top_fullscreen, visible);
    }

    public void setLockRotationVisible(boolean visible) {
        setViewVisible(iv_lock_rotation, visible);
    }

    public void setBottomFullscreenVisible(boolean visible) {
        setViewVisible(iv_bottom_fullscreen, visible);
    }

    public void setSeekBarVisible(boolean visible) {
        if (visible) {
            setViewVisible(sb_progress, true);
            setViewVisible(tv_current_time, true);
            setViewVisible(tv_end_time, true);
        } else {
            if (sb_progress != null) {
                sb_progress.setVisibility(View.INVISIBLE);
            }
            if (tv_current_time != null) {
                tv_current_time.setVisibility(View.INVISIBLE);
            }
            if (tv_end_time != null) {
                tv_end_time.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setErrorContainerVisible(boolean visible) {
        if (visible) {
            setViewVisible(ll_error_container, true);
            setViewVisible(ll_loading_container, false);
            setViewVisible(iv_pause, false);
        } else {
            setViewVisible(ll_error_container, false);
            setViewVisible(iv_pause, true);
        }
    }

    public void setLoadingDescriptionVisible(boolean visible) {
        setViewVisible(tv_loading_description, visible);
    }

    private void setViewVisible(View view, boolean visible) {
        if (view == null) {
            return;
        }
        if (visible) {
            view.setVisibility(VISIBLE);
        } else {
            view.setVisibility(GONE);
        }
    }

    public void setTitle(CharSequence title) {
        if (tv_title != null) {
            tv_title.setText(title);
        }
    }

    public void setOnlyFullScreen(boolean isOnlyFullScreen) {
        this.mOnlyFullScreen = isOnlyFullScreen;
    }

    public void setMediaControllerEnabled(boolean enabled) {
        iv_top_fullscreen.setEnabled(enabled);
        iv_bottom_fullscreen.setEnabled(enabled);
        iv_lock_rotation.setEnabled(enabled);
        iv_pause.setEnabled(enabled);
        sb_progress.setEnabled(enabled);
        mMediaControllerLayout.setEnabled(enabled);
    }

    public OrientationEventListener getOrientationEventListener() {
        return mOrientationEventListener;
    }

    public void setOrientationEventListener(OrientationEventListener orientationEventListener) {
        this.mOrientationEventListener = orientationEventListener;
        this.mOrientationEventListener.enable();
    }

    public OnOrientationChangedListener getOnOrientationChangedListener() {
        return mOnOrientationChangedListener;
    }

    public void setOnOrientationChangedListener(OnOrientationChangedListener onOrientationChangedListener) {
        this.mOnOrientationChangedListener = onOrientationChangedListener;
    }

    public interface OnOrientationChangedListener {
        void onOrientationChanged(int orientation);
    }
}
