package com.example.xuyezhuangt5000.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.xuyezhuangt5000.coolweather.gson.HeWeather;
import com.example.xuyezhuangt5000.coolweather.util.HttpUtil;
import com.example.xuyezhuangt5000.coolweather.util.Utilty;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
//后台服务，每过8个小时更新天气更背景图片等数据
public class AutoUpdateService extends Service {
    public AutoUpdateService() {

    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        updateWeather();
        updaeBingPic();
        AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);//获取时间管理器
        int hour=8*60*60*1000;//8小时
        long triggerAtTime= SystemClock.elapsedRealtime()+hour;//系统时间加8个小时
        Intent i=new Intent(this,AutoUpdateService.class);
        PendingIntent pi=PendingIntent.getService(this,0,i,0);//创建一个延迟意图
        alarmManager.cancel(pi);//把延迟意图给时间计算管理者
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);//1：设置从系统开机时间开始计算会唤醒cpu，2：过多久执行，
        // 3：延迟意图。当时间到了之后具体去做什么，所谓的系统开机时间应该service被创建的时间，要不怎么保证没过8个小时都执行一次，但是第一个参数说明是系统开机时间
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    private void updateWeather(){
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);
        if (weatherString!=null){
            //有缓存时直接解析天气数据
            final HeWeather heWeather= Utilty.handleWeatherResponse(weatherString);
            String weatherId=heWeather.getBasic().getId();
            String weatherUrl="http://guolin.tech/api/weather?cityid=" + weatherId + "&key=ef457aa445d74713a247a03b716a5edc";
            HttpUtil.sendOkHttpRequest(weatherId, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(getBaseContext(),"后台服务更新数据联网失败",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respnseText=response.body().string();
                    HeWeather heWeather1=Utilty.handleWeatherResponse(respnseText);
                    if (heWeather1!=null&&"ok".equals(heWeather.getStatus())){
                        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        prefs.edit().putString("weather",respnseText).apply();
                    }
                }
            });
        }

    }
    private void updaeBingPic(){
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getBaseContext(),"联网失败",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final  String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();

            }
        });
    }
}
