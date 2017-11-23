package com.example.xuyezhuangt5000.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.xuyezhuangt5000.coolweather.gson.HeWeather;
import com.example.xuyezhuangt5000.coolweather.util.HttpUtil;
import com.example.xuyezhuangt5000.coolweather.util.Utilty;

import java.io.DataOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActiviy extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weahterInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        //修改系统状态栏
        if (Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);//去掉系统状态栏的空间
        }
        setContentView(R.layout.activity_weather_activiy);
        weatherLayout=(ScrollView)findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);
        titleUpdateTime=(TextView)findViewById(R.id.title_update_time);
        degreeText=(TextView)findViewById(R.id.degree_text);
        weahterInfoText=(TextView)findViewById(R.id.weather_info_text);
        forecastLayout=(LinearLayout)findViewById(R.id.forecast_layout);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic=prefs.getString("bing_pic",null);
        if (bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }
        String weatherString=prefs.getString("weather",null);
        if (weatherString!=null){
            HeWeather heWeather= Utilty.handleWeatherResponse(weatherString);
            showWeatherInfo(heWeather);
        }else {
            String weatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }
    public void requestWeather(final String weatherId){
        //http://guolin.tech/api/weather?cityid=CN101190101&key=ef457aa445d74713a247a03b716a5edc
        String weatherUrl="http://guolin.tech/api/weather?cityid=" + weatherId + "&key=ef457aa445d74713a247a03b716a5edc";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(),"获取天气信息失败0",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
              final  String responseText=response.body().string();

              final HeWeather heWeather=Utilty.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(),"联网成功",Toast.LENGTH_SHORT).show();
                        if (heWeather!=null){
                           SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(heWeather);
                        }else {
                            Toast.makeText(getBaseContext(),"获取天气信息失败1",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
    public void showWeatherInfo(HeWeather heWeather){
        String cityName=heWeather.getBasic().getCity();
        String updateTime=heWeather.getBasic().getUpdate().getLoc().split(" ")[1];//以空格键位目标切割成一个数组拿第二个参数出来
        String degreee= heWeather.getHourly_forecast().get(1).getTmp()+"℃";
        String weatherInfo=heWeather.getNow().getCond().getTxt();
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degreee);
        weahterInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (HeWeather.DailyForecastBean forecast:heWeather.getDaily_forecast()){
            View view= LayoutInflater.from(getBaseContext()).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText=(TextView)view.findViewById(R.id.data_text);//当前控件是在forecast_item中的控件，查找的话前面不加父控件的话会报空指针。
            // 这是运行时异常，预编译的时候不会报错，直接findViewById代码自动填充习惯了没注意会吃大亏
            TextView infoText=(TextView)view.findViewById(R.id.info_text);
            TextView maxText=(TextView)view.findViewById(R.id.max_text);
            TextView minText=(TextView)view.findViewById(R.id.min_text);
            dateText.setText(forecast.getDate());
            infoText.setText(forecast.getCond().getTxt_n());
            maxText.setText(forecast.getTmp().getMax());
            minText.setText(forecast.getTmp().getMin());
            forecastLayout.addView(view);
        }
        if (heWeather.getAqi()!=null&&"ok".equals(heWeather.getStatus())){
            aqiText.setText(heWeather.getAqi().getCity().getAqi());
            pm25Text.setText(heWeather.getAqi().getCity().getPm25());
        }
        String comfort="舒适度"+heWeather.getSuggestion().getComf().getTxt();
        String coarWash="洗车指数"+heWeather.getSuggestion().getCw().getTxt();
        String sport="运动建议"+heWeather.getSuggestion().getSport().getTxt();
        comfortText.setText(comfort);
        carWashText.setText(coarWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }
    //加载必应每日一图
    private void loadBingPic(){
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(getBaseContext()).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}
