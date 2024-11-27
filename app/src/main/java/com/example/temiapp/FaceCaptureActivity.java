package com.example.temiapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cameraxlib.CameraXHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FaceCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private CameraXHelper cameraXHelper;
    private PreviewView previewView;
    private boolean hasProceeded = false; // 確保只處理一次

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);

        previewView = findViewById(R.id.previewView); // 對應 XML 中的 PreviewView

        // 確保相機權限已獲取
        if (checkAndRequestCameraPermission()) {
            initializeCamera();
        }
    }

    /**
     * 初始化 CameraX
     */
    private void initializeCamera() {
        // 初始化 CameraXHelper
        cameraXHelper = new CameraXHelper(this);
        cameraXHelper.setPreviewView(previewView);

        // 啟動相機
        cameraXHelper.startCamera(new CameraXHelper.CameraStartListener() {
            @Override
            public void onCameraStarted() {
                Log.d("FaceCaptureActivity", "相機啟動成功");
                // 延遲 2 秒後自動拍照
                previewView.postDelayed(() -> {
                    cameraXHelper.capturePhoto(new CameraXHelper.PhotoCaptureListener() {
                        @Override
                        public void onPhotoCaptured(String fileName) {
                            Log.d("FaceCaptureActivity", "照片拍攝成功，文件名: " + fileName);
                            monitorIdentifyValue();
                        }

                        @Override
                        public void onPhotoCaptureError(Exception e) {
                            Log.e("FaceCaptureActivity", "照片拍攝失敗: " + e.getMessage());
                            Toast.makeText(FaceCaptureActivity.this, "拍照失敗，請重試", Toast.LENGTH_SHORT).show();
                        }
                    });
                }, 2000);
            }

            @Override
            public void onCameraError(Exception e) {
                Log.e("FaceCaptureActivity", "相機啟動失敗: " + e.getMessage());
                Toast.makeText(FaceCaptureActivity.this, "相機啟動失敗", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 檢查並請求相機權限
     */
    private boolean checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true; // 權限已授予
        }

        // 若未授予權限，請求權限
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        return false;
    }

    /**
     * 處理權限請求結果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("FaceCaptureActivity", "相機權限授予成功");
                initializeCamera();
            } else {
                Log.e("FaceCaptureActivity", "相機權限被拒絕");
                Toast.makeText(this, "未授予相機權限，無法啟動相機", Toast.LENGTH_SHORT).show();
                finish(); // 結束當前活動
            }
        }
    }

    private void monitorIdentifyValue() {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("12");
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String identifyValue = dataSnapshot.child("identify").getValue(String.class);
                Log.d("FaceCaptureActivity", "Identify value: " + identifyValue);

                if (identifyValue != null && !hasProceeded) {
                    if ("success".equals(identifyValue)) {
                        Toast.makeText(FaceCaptureActivity.this, "辨識成功！", Toast.LENGTH_SHORT).show();
                        navigateToNextPage();
                        hasProceeded = true;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FaceCaptureActivity", "Failed to read value.", databaseError.toException());
            }
        });
    }

    private void navigateToNextPage() {
        Intent intent = new Intent(FaceCaptureActivity.this, VideoPlaybackActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraXHelper != null) {
            cameraXHelper.stopCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraXHelper != null) {
            cameraXHelper.startCamera(new CameraXHelper.CameraStartListener() {
                @Override
                public void onCameraStarted() {
                    Log.d("FaceCaptureActivity", "相機已重新啟動");
                }

                @Override
                public void onCameraError(Exception e) {
                    Log.e("FaceCaptureActivity", "相機重新啟動失敗: " + e.getMessage());
                }
            });
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraXHelper != null) {
            cameraXHelper.releaseResources();
        }

    }
}
