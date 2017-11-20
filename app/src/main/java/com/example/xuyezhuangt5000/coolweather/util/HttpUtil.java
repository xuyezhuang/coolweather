package com.example.xuyezhuangt5000.coolweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by xuyezhuangT5000 on 2017/11/20.
 */

public class HttpUtil {
    public static void sendOkHttpRequest(String adress,okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(adress).build();
        client.newCall(request).enqueue(callback);
    }
}
