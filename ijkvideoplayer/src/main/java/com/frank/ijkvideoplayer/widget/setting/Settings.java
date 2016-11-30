package com.frank.ijkvideoplayer.widget.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {

    public static final int PLAYER_ANDROID_MEDIA_PLAYER = 1;
    public static final int PLAYER_IJK_MEDIA_PLAYER = 2;
    public static final int PLAYER_IJK_EXO_MEDIA_PLAYER = 3;

    public static final int RENDER_SURFACE_VIEW = 0;
    public static final int RENDER_TEXTURE_VIEW = 1;

    public static final int LAYOUT_FIT_PARENT = 0;
    public static final int LAYOUT_FILL_PARENT = 1;
    public static final int LAYOUT_WRAP_CONTENT = 2;
    public static final int LAYOUT_MATCH_PARENT = 3;
    public static final int LAYOUT_16_9_FIT_PARENT = 4;
    public static final int LAYOUT_4_3_FIT_PARENT = 5;

    public static final String OVERLAY_FORMAT_RGBX_8888 = "fcc-rv32";
    public static final String OVERLAY_FORMAT_RGB_565 = "fcc-rv16";
    public static final String OVERLAY_FORMAT_RGB_888 = "fcc-rv24";
    public static final String OVERLAY_FORMAT_YV12 = "fcc-yv12";
    public static final String OVERLAY_FORMAT_OpenGL_ES2 = "fcc-_es2";

    public static final int ERROR_WIFI_DISCONNECTED = 20001;

    public static final String PLAYER_KEY = "ijk_video_player_player";
    public static final String RENDER_KEY = "ijk_video_player_render";
    public static final String USING_MEDIA_DATA_SOURCE_KEY = "ijk_video_player_using_media_data_source";
    public static final String USING_MEDIA_CODEC_KEY = "ijk_video_player_using_media_codec";
    public static final String USING_MEDIA_CODEC_AUTO_ROTATE_KEY = "ijk_video_player_using_media_codec_auto_rotate";
    public static final String MEDIA_CODEC_HANDLE_RESOLUTION_CHANGE_KEY = "ijk_video_player_media_codec_handle_resolution_change";
    public static final String USING_OPENSL_ES_KEY = "ijk_video_player_using_opensl_es";
    public static final String PIXEL_FORMAT_KEY = "ijk_video_player_pixel_format";
    public static final String ENABLE_DETACHED_SURFACE_TEXTURE_VIEW_KEY = "ijk_video_player_enable_detached_surface_texture_view";
    public static final String ENABLE_BACKGROUND_PLAY_KEY = "ijk_video_player_enable_background_play";

    private SharedPreferences mSharedPreferences;
    private boolean mEnableLogging = true;
    private boolean mEnableStorePlaybackProgress = false;
    private static String mCurrentActivityKey;

    public Settings(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * 获取当前使用的播放器类型
     *
     * @return Settings.PLAYER_ANDROID_MEDIA_PLAYER：AndroidMediaPlayer
     * Settings.PLAYER_IJK_MEDIA_PLAYER：IjkMediaPlayer
     * Settings.PLAYER_IJK_EXO_MEDIA_PLAYER：IjkExoMediaPlayer
     */
    public int getPlayer() {
        return mSharedPreferences.getInt(PLAYER_KEY, PLAYER_IJK_MEDIA_PLAYER);
    }

    /**
     * 设置播放器类型
     *
     * @param player Settings.PLAYER_ANDROID_MEDIA_PLAYER：AndroidMediaPlayer
     *               Settings.PLAYER_IJK_MEDIA_PLAYER：IjkMediaPlayer
     *               Settings.PLAYER_IJK_EXO_MEDIA_PLAYER：IjkExoMediaPlayer
     */
    public void setPlayer(int player) {
        mSharedPreferences.edit()
                .putInt(PLAYER_KEY, player)
                .apply();
    }

    /**
     * 获取当前渲染类型
     *
     * @return Settings.RENDER_NONE：none
     * Settings.RENDER_SURFACE_VIEW：Surface
     * Settings.RENDER_TEXTURE_VIEW：texture
     */
    public int getRender() {
        return mSharedPreferences.getInt(RENDER_KEY, RENDER_SURFACE_VIEW);
    }

    /**
     * 设置渲染类型
     *
     * @param render Settings.RENDER_NONE：none
     *               Settings.RENDER_SURFACE_VIEW：Surface
     *               Settings.RENDER_TEXTURE_VIEW：texture
     */
    public void setRender(int render) {
        mSharedPreferences.edit()
                .putInt(RENDER_KEY, render)
                .apply();
    }

    public boolean getUsingMediaDataSource() {
        return mSharedPreferences.getBoolean(USING_MEDIA_DATA_SOURCE_KEY, false);
    }

    public void setUsingMediaDataSource(boolean usingMediaDataSource) {
        mSharedPreferences.edit()
                .putBoolean(USING_MEDIA_DATA_SOURCE_KEY, usingMediaDataSource)
                .apply();
    }

    /**
     * 是否使用硬解码
     *
     * @return true硬解码，false软解码
     */
    public boolean getUsingMediaCodec() {
        return mSharedPreferences.getBoolean(USING_MEDIA_CODEC_KEY, false);
    }

    /**
     * 设置是否使用硬解码
     *
     * @param usingMediaCodec true硬解码，false软解码
     */
    public void setUsingMediaCodec(boolean usingMediaCodec) {
        mSharedPreferences.edit()
                .putBoolean(USING_MEDIA_CODEC_KEY, usingMediaCodec)
                .apply();
    }

    /**
     * 是否使用mediacodec-auto-rotate
     *
     * @return true使用，false未使用
     */
    public boolean getUsingMediaCodecAutoRotate() {
        return mSharedPreferences.getBoolean(USING_MEDIA_CODEC_AUTO_ROTATE_KEY, false);
    }

    /**
     * 设置是否使用mediacodec-auto-rotate
     *
     * @param usingMediaCodecAutoRotate true使用，false未使用
     */
    public void setUsingMediaCodecAutoRotate(boolean usingMediaCodecAutoRotate) {
        mSharedPreferences.edit()
                .putBoolean(USING_MEDIA_CODEC_AUTO_ROTATE_KEY, usingMediaCodecAutoRotate)
                .apply();
    }

    /**
     * 是否使用mediacodec-handle-resolution-change
     *
     * @return true使用，false未使用
     */
    public boolean getMediaCodecHandleResolutionChange() {
        return mSharedPreferences.getBoolean(MEDIA_CODEC_HANDLE_RESOLUTION_CHANGE_KEY, false);
    }

    /**
     * 设置是否使用mediacodec-handle-resolution-change
     *
     * @param mediaCodecHandleResolutionChange true使用，false未使用
     */
    public void setMediaCodecHandleResolutionChange(boolean mediaCodecHandleResolutionChange) {
        mSharedPreferences.edit()
                .putBoolean(MEDIA_CODEC_HANDLE_RESOLUTION_CHANGE_KEY, mediaCodecHandleResolutionChange)
                .apply();
    }

    /**
     * 是否使用OpenSL ES
     *
     * @return true使用，false未使用
     */
    public boolean getUsingOpenSLES() {
        return mSharedPreferences.getBoolean(USING_OPENSL_ES_KEY, false);
    }

    /**
     * 设置是否使用OpenSL ES
     *
     * @param usingOpenSLES true使用，false未使用
     */
    public void setUsingOpenSLES(boolean usingOpenSLES) {
        mSharedPreferences.edit()
                .putBoolean(USING_OPENSL_ES_KEY, usingOpenSLES)
                .apply();
    }

    /**
     * 获取当前Pixel Format
     *
     * @return 默认OVERLAY_FORMAT_RGBX_8888
     */
    public String getPixelFormat() {
        return mSharedPreferences.getString(PIXEL_FORMAT_KEY, OVERLAY_FORMAT_RGBX_8888);
    }

    /**
     * 设置Pixel Format
     *
     * @param pixelFormat OVERLAY_FORMAT_RGBX_8888,...
     */
    public void setPixelFormat(String pixelFormat) {
        mSharedPreferences.edit()
                .putString(PIXEL_FORMAT_KEY, pixelFormat)
                .apply();
    }

    /**
     * 是否使用TextureMediaPlayer
     *
     * @return true使用，false未使用
     */
    public boolean getEnableDetachedSurfaceTextureView() {
        return mSharedPreferences.getBoolean(ENABLE_DETACHED_SURFACE_TEXTURE_VIEW_KEY, false);
    }

    /**
     * 设置是否使用TextureMediaPlayer
     *
     * @param enableDetachedSurfaceTextureView true使用，false未使用
     */
    public void setEnableDetachedSurfaceTextureView(boolean enableDetachedSurfaceTextureView) {
        mSharedPreferences.edit()
                .putBoolean(ENABLE_DETACHED_SURFACE_TEXTURE_VIEW_KEY, enableDetachedSurfaceTextureView)
                .apply();
    }

    /**
     * 是否使用后台播放
     *
     * @return true使用，false未使用
     */
    public boolean getEnableBackgroundPlay() {
        return mSharedPreferences.getBoolean(ENABLE_BACKGROUND_PLAY_KEY, false);
    }

    /**
     * 设置是否使用后台播放
     *
     * @param enableBackgroundPlay true使用，false未使用
     */
    public void setEnableBackgroundPlay(boolean enableBackgroundPlay) {
        mSharedPreferences.edit()
                .putBoolean(ENABLE_BACKGROUND_PLAY_KEY, enableBackgroundPlay)
                .apply();
    }

    /**
     * 是否启用调试Log日志
     *
     * @return true使用，false未使用
     */
    public boolean getEnableLogging() {
        return mEnableLogging;
    }

    /**
     * 设置是否启用调试Log日志
     *
     * @param enableLogging true使用，false未使用
     */
    public void setEnableLogging(boolean enableLogging) {
        this.mEnableLogging = enableLogging;
    }

    /**
     * 是否保存播放进度
     *
     * @return true保存，false不保存
     */
    public boolean getEnableStorePlaybackProgress() {
        return mEnableStorePlaybackProgress;
    }

    /**
     * 设置是否保存播放进度
     *
     * @param enableStorePlaybackProgress true保存，false不保存
     */
    public void setEnableStorePlaybackProgress(boolean enableStorePlaybackProgress) {
        this.mEnableStorePlaybackProgress = enableStorePlaybackProgress;
    }

    /**
     * 查找上次播放进度
     *
     * @param cacheKey Cache key
     * @return 上一次播放进度
     */
    public long getLastPosition(String cacheKey) {
        return mSharedPreferences.getLong(cacheKey, 0L);
    }

    /**
     * 保存播放进度
     *
     * @param cacheKey     Cache key
     * @param lastPosition 上一次播放进度
     */
    public void setLastPosition(String cacheKey, long lastPosition) {
        mSharedPreferences.edit()
                .putLong(cacheKey, lastPosition)
                .apply();
    }

    public static String getCurrentActivityKey() {
        return mCurrentActivityKey;
    }

    public static void setCurrentActivityKey(String currentActivityKey) {
        Settings.mCurrentActivityKey = currentActivityKey;
    }
}
