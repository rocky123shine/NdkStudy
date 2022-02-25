package com.rocky.ndkstudy.live.channel;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.core.CameraX;

import com.rocky.ndkstudy.live.LivePusher;
import com.rocky.ndkstudy.live.utils.ImageUtil;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <pre>
 *     author : rocky
 *     time   : 2022/02/22
 *     des    ： 这里使用camerax捕获数据 输出yuv
 * </pre>
 */
public class VideoChannel implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {
    private TextureView textureView;
    private LivePusher livePusher;
    private HandlerThread handlerThread;
    private static final String TAG = "rtmp";
    //    直播中  480 640
    int width = 480;
    int height = 640;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    public VideoChannel(LifecycleOwner lifecycleOwner, TextureView textureView, LivePusher livePusher,CameraX.LensFacing currentFacing) {
        this.textureView = textureView;
        this.livePusher = livePusher;
        this.currentFacing = currentFacing;
        //子线程中回调
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        //使用camerax
        //1.绑定生命周期
        //2. UseCase
        //      ImageAnalysis   A use case providing CPU accessible images for an app to perform image analysis on.
        //      Preview         A use case that provides a camera preview stream for displaying on-screen.
        //      ImageCapture    A use case for taking a picture.
        //      VideoCapture    use case for taking a video.
        //这里我们使用的是图片分析 和图像预览
        CameraX.bindToLifecycle(lifecycleOwner, getPreview(), getImageAnalysis());

    }

    //创建一个Preview
    private Preview getPreview() {
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(width, height))//从此配置中设置预期目标的分辨率
                .setLensFacing(currentFacing)//设置前后摄像头
                .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);//设置预览回掉
        return preview;
    }

    //创建ImageAnalysis
    private ImageAnalysis getImageAnalysis() {
        ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))//设置线程
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)//获取最新的image数据
                .setTargetResolution(new Size(width, height))//设置分辨率
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(analysisConfig);
        imageAnalysis.setAnalyzer(this);//获取分析数据
        return imageAnalysis;
    }


    //Preview 的回掉监听
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView.getSurfaceTexture() != surfaceTexture) {
            if (textureView.isAvailable()) {
                //此时 需要重新初始化surface相关   surface 切换了
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);//移除当前
                parent.addView(textureView, 0);//把当前至于顶部
                parent.requestLayout();
            }
            textureView.setSurfaceTexture(surfaceTexture);//TextureView 和 SurfaceTexture绑定
        }

    }

    private boolean isLiving;

    public void startLive() {
        isLiving = true;
    }

    public void stopLive() {
        isLiving = false;

    }

    private final ReentrantLock lock = new ReentrantLock();
    private byte[] y;
    private byte[] u;
    private byte[] v;

    private byte[] nv21;
    byte[] nv21_rotated;
    //byte[] nv12;

    //获取图像数据
    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        if (!isLiving) {// 直播结束 数据就不再处理
            return;
        }
        //因为数据是在子线程处理的  所以需要一把锁
        lock.lock();
        //camera2 获取数据是从planes 中 camerax封装的camera2
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        //分别回去 yuv
// 重复使用同一批byte数组，减少gc频率
        if (y == null) {
//            初始化y v  u
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];//y 的实际长度
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            livePusher.native_setVideoEncInfo( image.getWidth(), image.getHeight(),10, 640_000);
        }

        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            //进来之后 更新yuv赋值
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
            int stride = planes[0].getRowStride();
            Size size = new Size(image.getWidth(), image.getHeight());
            int width = size.getHeight();
            int heigth = image.getWidth();
//            Log.i(TAG, "analyze: "+width+"  heigth "+heigth);
            if (nv21 == null) {
                nv21 = new byte[heigth * width * 3 / 2];
                nv21_rotated = new byte[heigth * width * 3 / 2];
            }
            ImageUtil.yuvToNv21(y, u, v, nv21, heigth, width);
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, heigth, width);
            this.livePusher.native_pushVideo(nv21_rotated);
        }
        lock.unlock();


    }
}
