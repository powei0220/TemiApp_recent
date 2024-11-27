package com.example.temiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.VideoView;
import android.widget.MediaController;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class VideoPlaybackActivity extends AppCompatActivity {

    private VideoView videoView;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_playback);

        videoView = findViewById(R.id.videoView); // 影片視圖

        // 初始化 Firebase Realtime Database 參考
        databaseReference = FirebaseDatabase.getInstance().getReference("12");

        // 加入控制器，提供播放控制
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // 加載影片並監聽 Firebase 變更
        loadVideoFromDatabase();


    }
    public void onBackButtonClicked1(View view) {
        returnToMainAndClearNode();
    }

    private void loadVideoFromDatabase() {
        // 監聽 Firebase "12/video" 節點的值變化
        databaseReference.child("video").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer videoConfig = dataSnapshot.getValue(Integer.class);

                    if (videoConfig != null) {
                        // 如果 video 值存在且有效，播放對應影片
                        String videoPath = getVideoPath(videoConfig);
                        if (!videoPath.isEmpty()) {
                            videoView.setVideoPath(videoPath);
                            videoView.start();
                        } else {
                            Toast.makeText(VideoPlaybackActivity.this, "無效的影片設置", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // 當 video 節點不存在或為空時
                    Toast.makeText(VideoPlaybackActivity.this, "等待新影片數據...", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 讀取資料失敗
                Toast.makeText(VideoPlaybackActivity.this, "資料讀取錯誤: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getVideoPath(int videoConfig) {
        switch (videoConfig) {
            case 1:
                return "android.resource://" + getPackageName() + "/" + R.raw.people_1;
            case 2:
                return "android.resource://" + getPackageName() + "/" + R.raw.people_2;
            case 3:
                return "android.resource://" + getPackageName() + "/" + R.raw.people_3;
            default:
                return ""; // 無效影片設定
        }
    }

    private void returnToMainAndClearNode() {
        // 刪除 "12" 節點的所有資料
        databaseReference.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 重新創建一個空的 "12" 節點
                databaseReference.setValue(null).addOnCompleteListener(innerTask -> {
                    if (innerTask.isSuccessful()) {
                        Toast.makeText(VideoPlaybackActivity.this, "節點已清空並保留", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VideoPlaybackActivity.this, "清空後重建節點失敗", Toast.LENGTH_SHORT).show();
                    }
                    // 返回主頁面
                    navigateToMainActivity();
                });
            } else {
                Toast.makeText(VideoPlaybackActivity.this, "清空資料失敗", Toast.LENGTH_SHORT).show();
                // 無論成功與否，都返回主頁面
                navigateToMainActivity();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(VideoPlaybackActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 清除當前活動堆疊並返回主頁面
        startActivity(intent);
        finish(); // 結束當前活動
    }
}
