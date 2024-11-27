package com.example.temiapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.temilib.PatrolHelper;

public class PatrolActivity extends AppCompatActivity {

    private static final String TAG = "PatrolActivity";
    private PatrolHelper patrolHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patrol);

        // 初始化 PatrolHelper
        patrolHelper = new PatrolHelper(this);
        patrolHelper.initPatrol();
    }
    // 開始巡邏按鈕的點擊事件
    public void onCourseRecomButtonClicked(View view) {
        patrolHelper.startPatrolling();  // 開始巡邏
        Toast.makeText(this, "開始巡邏", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "開始巡邏按鈕被按下");
    }

    // 返回主頁面按鈕的點擊事件
    public void onBackButtonClicked1(View view) {
        patrolHelper.stopPatrolling();  // 停止巡邏
        Intent intent = new Intent(PatrolActivity.this, MainActivity.class);
        startActivity(intent);
        finish();  // 關閉當前活動
        Log.d(TAG, "返回主頁面按鈕被按下");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 銷毀 PatrolHelper 資源
        patrolHelper.destroyPatrol();
        Log.d(TAG, "PatrolActivity 銷毀完成");
    }
}
