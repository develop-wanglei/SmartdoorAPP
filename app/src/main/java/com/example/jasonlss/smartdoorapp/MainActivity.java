package com.example.jasonlss.smartdoorapp;



import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.GetCallback;

public class MainActivity extends AppCompatActivity {
    private AVObject door = AVObject.createWithoutData("SmartDoor", "5de89281dd3c13007fcfc537");
    private AVObject door_update = AVObject.createWithoutData("SmartDoor", "5de89281dd3c13007fcfc537");
    private AVObject door_image = AVObject.createWithoutData("DoorImage", "5de8947fd4b56c008b15baa0");
    private NotificationManager mManager;
    private long TimeTicket = 0;
    //private boolean DoorState=false;
    private boolean ButtonState = false;
    private Handler LoopHandler = new Handler();
    private ImageView Imcamera;
    private boolean ImageFlag = false;
    private boolean Imshow = false;
    private boolean imageState = false;
    private boolean PeopleCome = false;
    Bitmap bitmap;
    Button OPENbutton;
    TextView BellState;
    private boolean toaststate=false;
    private byte[] imageBytes;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OPENbutton = findViewById(R.id.button);
        BellState = findViewById(R.id.textView);
        Imcamera = findViewById(R.id.imageView);
        door_update.put("DOOR_STATE", false);
        door_update.saveInBackground();
        LoopHandler.post(looper);

        //通知消息在一个activity只打开一次
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        OPENbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                door_update.put("DOOR_STATE", true);
                door_update.saveInBackground();
            }
        });
        mManager.cancel(1);
    }

    private Runnable looper = new Runnable() {//主任务循环（1ms
        @Override
        public void run() {
            TimeTicket++;
            if (TimeTicket > 9999) {
                TimeTicket = 0;
            }
            if (TimeTicket % 500 == 0) {
                door.fetchInBackground(new GetCallback<AVObject>() {
                    @Override
                    public void done(AVObject avObject, AVException e) {
                        if(avObject!=null)
                        {
                            ButtonState = avObject.getBoolean("BUTTON_STATE");
                            toaststate=false;
                        }
                        else
                        {
                            if(!toaststate)
                            {
                                Toast ts = Toast.makeText(MainActivity.this,"网络连接失败，请联网后重试！", Toast.LENGTH_LONG);
                                ts.show();
                                toaststate=true;
                            }
                        }
                    }
                });
                door.fetchInBackground(new GetCallback<AVObject>() {
                    @Override
                    public void done(AVObject avObject, AVException e) {
                        if(avObject!=null)
                            imageState = avObject.getBoolean("IMAGE_STATE");
                    }
                });
                if (ButtonState) {
                    BellState.setText("有人来了");


                } else {
                    BellState.setText("没有人");
                    Imcamera.setImageBitmap(null);
                    ImageFlag = false;
                    PeopleCome=false;
                }
                Imshow = false;
            }
            if (imageState && !ImageFlag) {
                door_image.fetchInBackground(new GetCallback<AVObject>() {
                    @Override
                    public void done(AVObject avObject, AVException e) {
                        if(avObject!=null)
                            imageBytes = avObject.getBytes("IMAGE");
                    }
                });
                ImageFlag = true;
                imageState = false;
                door.put("IMAGE_STATE", false);
                door.saveInBackground();
            }

            if (imageBytes != null) {
                if (!Imshow && ButtonState) {
                    bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    Imcamera.setImageBitmap(bitmap);
                    if(!PeopleCome)
                        sendNotification();
                    PeopleCome=true;
                }
                Imshow = true;
            }
            LoopHandler.postDelayed(looper, 1);
        }
    };

    private void sendNotification() {
        String channelId = "channelId";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "notification";
            createNotificationChannel(channelId, channelName);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(bitmap);
        builder.setContentTitle("SmartDoor");
        builder.setContentText("有人来了");
        builder.setNumber(1);
        //builder.setTimeoutAfter(10000)
        //大图标展开
        android.support.v4.app.NotificationCompat.BigPictureStyle style = new android.support.v4.app.NotificationCompat.BigPictureStyle();
        style.setBigContentTitle("摄像头已拍摄");
        //style.setSummaryText("SummaryText");
        style.bigPicture(bitmap);
        builder.setStyle(style);
        builder.setAutoCancel(true);
        //通知点击返回应用
        Intent msgIntent = getPackageManager().getLaunchIntentForPackage("com.example.jasonlss.smartdoorapp");//获取启动Activity
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                1,
                msgIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        Notification notification = builder.build();
        mManager.notify(1, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(true);
        channel.enableLights(true);//是否在桌面icon右上角展示小红点
        channel.setLightColor(Color.RED);//小红点颜色
        mManager.createNotificationChannel(channel);
    }

    protected void onDestroy() {
        super.onDestroy();
        LoopHandler.removeCallbacks(looper);

    }
}
