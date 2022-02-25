package com.rocky.ndkstudy.live;

import android.view.TextureView;

import androidx.camera.core.CameraX;
import androidx.lifecycle.LifecycleOwner;

import com.rocky.ndkstudy.live.channel.VideoChannel;

/**
 * <pre>
 *     author : rocky
 *     time   : 2022/02/22
 *     des    ：这里是推流工具类
 * </pre>
 */
public class LivePusher {
    static {
        System.loadLibrary("ndkstudy");
    }

    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;

    private final VideoChannel videoChannel;


    public LivePusher(LifecycleOwner lifecycleOwner, TextureView textureView) {
        //初始化编码器 x264 native层
        native_init();
        //初始化java层的VideoChannel
        videoChannel = new VideoChannel(lifecycleOwner, textureView, this, currentFacing);


    }

    private native void native_init();

    public native void native_setVideoEncInfo(int height, int width, int fps, int bitrate);

    public native void native_pushVideo(byte[] data);

    public void startLive(String url) {
        native_start(url);
        if (videoChannel == null) {
            return;
        }
        videoChannel.startLive();
    }

    public void switchCamera() {
        //在这里切换摄像头 注意 切换后 surface 和 服务器需要重连
    }

    public void stopLive() {
    }

    public native void native_release();

    public native void native_start(String path);

}
