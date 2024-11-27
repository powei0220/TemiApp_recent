package com.example.temilib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.List;

import com.example.cameraxlib.CameraXHelper;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import org.jetbrains.annotations.NotNull;

public class PatrolHelper implements
        OnRobotReadyListener,
        OnGoToLocationStatusChangedListener,
        OnCurrentPositionChangedListener {

    private static final String TAG = "PatrolHelper";

    private Robot robot;
    private CameraXHelper cameraXHelper;
    private List<String> patrolPoints;
    private int currentPointIndex = 0;
    private boolean isPatrolling = false;

    public PatrolHelper(Context context) {
        this.robot = Robot.getInstance();
        this.cameraXHelper = new CameraXHelper(context); // 初始化 CameraXHelper
    }

    public void initPatrol() {
        robot.addOnRobotReadyListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addOnCurrentPositionChangedListener(this);
    }

    public void startPatrolling() {
        if (!isPatrolling) {
            patrolPoints = robot.getLocations();
            if (patrolPoints != null && !patrolPoints.isEmpty()) {
                isPatrolling = true;
                currentPointIndex = 0;
                goToNextPoint();
                Log.d(TAG, "開始巡邏。");
            } else {
                Log.d(TAG, "沒有已保存的地點。");
            }
        } else {
            Log.d(TAG, "已經在巡邏中。");
        }
    }

    public void stopPatrolling() {
        isPatrolling = false;
        Log.d(TAG, "停止巡邏。");
    }

    public void destroyPatrol() {
        robot.removeOnRobotReadyListener(this);
        robot.removeOnGoToLocationStatusChangedListener(this);
        robot.removeOnCurrentPositionChangedListener(this);
    }

    private void goToNextPoint() {
        if (patrolPoints != null && !patrolPoints.isEmpty() && isPatrolling) {
            String destination = patrolPoints.get(currentPointIndex);
            Log.d(TAG, "前往地點：" + destination);
            robot.goTo(destination, false, null, SpeedLevel.SLOW);
        }
    }

    public void tiltHead(int degrees, float speed) {
        if (degrees >= -25 && degrees <= 55 && speed >= 0 && speed <= 1) {
            robot.tiltAngle(degrees, speed);
        } else {
            Log.e(TAG, "請設定有效的角度 (-25 ~ 55) 和速度 (0 ~ 1)。");
        }
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            Log.d(TAG, "機器人已準備就緒。");
            robot.hideTopBar();
        }
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int id, @NotNull String desc) {
        Log.d(TAG, "地點：" + location + ", 狀態：" + status);
        if ("complete".equalsIgnoreCase(status)) {
            if ("home base".equalsIgnoreCase(location)) {
                Log.d(TAG, "機器人到達充電桩，跳過動作執行，繼續巡邏至下一地點。");
                moveToNextPoint();
                return;
            }
            handleLocationActions(location);
        } else if ("abort".equalsIgnoreCase(status)) {
            isPatrolling = false;
            Log.d(TAG, "導航中止，停止巡邏。");
        }
    }

    @Override
    public void onCurrentPositionChanged(Position position) {
        float x = position.getX();
        float y = position.getY();
        Log.d(TAG, "當前位置：x=" + x + ", y=" + y);
    }

    private void handleLocationActions(String location) {
        Log.d(TAG, "到達地點：" + location + "，執行動作。");

        // 延遲執行旋轉拍照
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            rotateAndCapturePhotos(() -> {
                Log.d(TAG, "完成所有動作，繼續巡邏。");
                moveToNextPoint();
            });
        }, 10000); // 停留 10 秒，根據需求調整
    }

    private void rotateAndCapturePhotos(Runnable onComplete) {
        final int[] turnCount = {0};
        final int maxTurns = 8; // 360 度分為 8 次，每次 45 度

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (turnCount[0] < maxTurns) {
                    turnAndCapture(45);
                    turnCount[0]++;
                    new Handler(Looper.getMainLooper()).postDelayed(this, 1000); // 每次延遲 1 秒
                } else {
                    onComplete.run(); // 所有旋轉完成後回調
                }
            }
        });
    }

    private void turnAndCapture(int degrees) {
        robot.turnBy(degrees, 1.0f);
        cameraXHelper.capturePhoto(new CameraXHelper.PhotoCaptureListener() {
            @Override
            public void onPhotoCaptured(String filePath) {
                Log.d(TAG, "拍照成功，照片儲存於：" + filePath);
            }

            @Override
            public void onPhotoCaptureError(Exception e) {
                Log.e(TAG, "拍照失敗：" + e.getMessage());
            }
        });
    }

    private void moveToNextPoint() {
        if (!patrolPoints.isEmpty()) {
            currentPointIndex = (currentPointIndex + 1) % patrolPoints.size();
            goToNextPoint();
        }
    }
}
