package com.example.jasonlss.smartdoorapp;

import android.app.Application;

import com.avos.avoscloud.AVOSCloud;


public class MyLeanCloudApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();


        AVOSCloud.initialize(this,"3x1teTj3EBqyr6s0UzG0nroP-gzGzoHsz","evT4PsqqdJzJvjxNXKeYXFYw");

        // 放在 SDK 初始化语句 AVOSCloud.initialize() 后面，只需要调用一次即可
        AVOSCloud.setDebugLogEnabled(true);//app发布后关闭

    }

}
