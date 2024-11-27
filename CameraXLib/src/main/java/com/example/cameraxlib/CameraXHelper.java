package com.example.cameraxlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraXHelper {
    private Context context;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;

    public interface CameraStartListener {
        void onCameraStarted();

        void onCameraError(Exception e);
    }

    public interface PhotoCaptureListener {
        void onPhotoCaptured(String filePath);

        void onPhotoCaptureError(Exception e);
    }

    public CameraXHelper(Context context) {
        this.context = context;
    }

    public void setPreviewView(PreviewView previewView) {
        this.previewView = previewView;
    }

    public void startCamera(CameraStartListener listener) {
        if (cameraProvider != null) {
            Log.d("CameraXHelper", "相機已經啟動過了");
            return; // 相機已經啟動，避免重複啟動
        }
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                if (listener != null) {
                    listener.onCameraStarted();
                }
            } catch (ExecutionException | InterruptedException e) {
                if (listener != null) {
                    listener.onCameraError(e);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCameraUseCases() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 創建預覽用例並設置 SurfaceProvider
        Preview preview = new Preview.Builder().build();
        if (previewView != null) {
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } else {
            Log.e("CameraXHelper", "PreviewView 未初始化，無法顯示預覽");
        }

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1920, 1080))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // 取消綁定所有先前的用例
        cameraProvider.unbindAll();

        try {
            // 綁定預覽和拍照用例到 CameraX 的生命週期
            cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview, imageCapture);
            Log.d("CameraXHelper", "相機已啟動並綁定");
        } catch (Exception e) {
            Log.e("CameraXHelper", "綁定相機使用案例失敗: " + e.getMessage());
        }
    }

    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            Log.d("CameraXHelper", "相機已停止");
        }
    }

    public void releaseResources() {
        stopCamera();
        imageCapture = null;
        cameraProvider = null;
        Log.d("CameraXHelper", "相機資源已完全釋放");
    }

    public void capturePhoto(PhotoCaptureListener listener) {
        if (imageCapture != null) {
            Log.d("CameraXHelper", "正在進行拍照...");
            imageCapture.takePicture(ContextCompat.getMainExecutor(context), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    Log.d("CameraXHelper", "拍照成功，開始處理圖片");
                    Bitmap bitmap = imageProxyToBitmap(image);
                    String fileName = generateImageName();
                    uploadImageToFirebase(bitmap, fileName, listener);
                    image.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("CameraXHelper", "拍照失敗: " + exception.getMessage());
                    if (listener != null) {
                        listener.onPhotoCaptureError(exception);
                    }
                }
            });
        } else {
            Log.e("CameraXHelper", "ImageCapture 尚未初始化，無法拍照");
            if (listener != null) {
                listener.onPhotoCaptureError(new IllegalStateException("ImageCapture 尚未初始化"));
            }
        }
    }

    private String generateImageName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "IMG_" + sdf.format(new Date()) + ".jpg";
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        if (image == null) {
            Log.e("CameraXHelper", "收到的 image 為空");
            return null;
        }

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            Log.e("CameraXHelper", "圖片解碼失敗");
        }
        return bitmap;
    }

    private void uploadImageToFirebase(Bitmap bitmap, String fileName, PhotoCaptureListener listener) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child(fileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storageRef.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Log.d("CameraXHelper", "圖片上傳成功: " + fileName);
            if (listener != null) {
                listener.onPhotoCaptured(fileName);
            }
        }).addOnFailureListener(e -> {
            Log.e("CameraXHelper", "圖片上傳失敗: " + e.getMessage());
            if (listener != null) {
                listener.onPhotoCaptureError(e);
            }
        });
    }
}
