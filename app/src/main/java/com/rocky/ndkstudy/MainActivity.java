package com.rocky.ndkstudy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.rocky.ndkstudy.live.LivePusher;


public class MainActivity extends AppCompatActivity {
    //H264码流
    private LivePusher livePusher;
    private TextureView textureView;
    private String url = "rtmp://ip/rtmplive/1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        textureView = findViewById(R.id.textureView);
        livePusher = new LivePusher(this, textureView);
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, 1);
        }
        return false;
    }

    public void switchCamera(View view) {
        livePusher.switchCamera();
    }

    public void startLive(View view) {
        livePusher.startLive(url);
    }

    public void stopLive(View view) {
        livePusher.stopLive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        livePusher.native_release();
    }
}
