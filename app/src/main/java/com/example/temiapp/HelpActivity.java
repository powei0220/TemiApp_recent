package com.example.temiapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.temilib.PatrolHelper;

public class HelpActivity  extends AppCompatActivity  {
    private static final String TAG = "HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_help);  // 確保有 activity_help.xml
        Log.d(TAG, "HelpActivity 已啟動");
    }

    // 返回主頁面按鈕的點擊事件
    public void onBackButtonClicked1(View view) {
        Intent intent = new Intent(HelpActivity.this, MainActivity.class);
        startActivity(intent);
        finish();  // 關閉當前活動
        Log.d(TAG, "返回主頁面按鈕被按下");
    }

}
