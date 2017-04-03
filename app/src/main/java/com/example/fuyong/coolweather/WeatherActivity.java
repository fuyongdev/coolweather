package com.example.fuyong.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.fuyong.coolweather.gson.Forecast;
import com.example.fuyong.coolweather.gson.Weather;
import com.example.fuyong.coolweather.service.AutoUpdateService;
import com.example.fuyong.coolweather.util.HttpUtil;
import com.example.fuyong.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView mWeatherSv;

    private TextView mTitleCityTv;

    private TextView mTitleUpdateTimeTv;

    private TextView mDegreeTv;

    private TextView mWeatherInfoTv;

    private LinearLayout mForeCastLly;

    private TextView mAqiTv;
    private TextView mPm25Tv;
    private TextView mComfortTv;
    private TextView mCarWashTv;
    private TextView mSportTv;
    private ImageView mBingPic;
    public SwipeRefreshLayout mSwipeRefresh;
    private String mWeatherId;
    public DrawerLayout mDrawerLayout;
    private Button mNavBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_weather);
        mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        mWeatherSv = (ScrollView) findViewById(R.id.scroll_weather);
        mWeatherInfoTv = (TextView) findViewById(R.id.tv_weather_info);
        mTitleCityTv = (TextView) findViewById(R.id.tv_title_city);
        mTitleUpdateTimeTv = (TextView) findViewById(R.id.tv_title_update_time);
        mDegreeTv = (TextView) findViewById(R.id.tv_degree);
        mWeatherInfoTv = (TextView) findViewById(R.id.tv_weather_info);
        mForeCastLly = (LinearLayout) findViewById(R.id.lly_forecast);
        mAqiTv = (TextView) findViewById(R.id.tv_aqi);
        mPm25Tv = (TextView) findViewById(R.id.tv_pm25);
        mComfortTv = (TextView) findViewById(R.id.tv_comfort);
        mCarWashTv = (TextView) findViewById(R.id.tv_car_wash);
        mSportTv = (TextView) findViewById(R.id.tv_sport);
        mBingPic = (ImageView) findViewById(R.id.iv_bing_pic);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_nav);
        mNavBtn = (Button) findViewById(R.id.btn_nav);

        mNavBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        String bingPicStr = prefs.getString("bing_pic", null);
        if (bingPicStr != null) {
            Glide.with(this).load(bingPicStr).into(mBingPic);
        } else {
            loadBingPic();
        }
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            mWeatherId = getIntent().getStringExtra("weather_id");
            mWeatherSv.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);

        }
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
    }

    private void loadBingPic() {
        String requestBingPicStr = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPicStr, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPicStr = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                        (WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPicStr);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPicStr).into(mBingPic);
                    }
                });
            }
        });
    }

    public void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId +
                "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        mSwipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                            loadBingPic();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT)
                                    .show();
                        }
                        mSwipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        mTitleCityTv.setText(cityName);
        mTitleUpdateTimeTv.setText(updateTime);
        mDegreeTv.setText(degree);
        mWeatherInfoTv.setText(weatherInfo);

        mForeCastLly.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForeCastLly,
                    false);
            TextView dateTv = (TextView) view.findViewById(R.id.tv_data);
            TextView infoTv = (TextView) view.findViewById(R.id.tv_info);
            TextView maxTv = (TextView) view.findViewById(R.id.tv_max);
            TextView minTv = (TextView) view.findViewById(R.id.tv_min);
            dateTv.setText(forecast.date);
            infoTv.setText(forecast.more.info);
            maxTv.setText(forecast.temperature.max);
            minTv.setText(forecast.temperature.min);
            mForeCastLly.addView(view);
        }
        if (weather.aqi != null) {
            mAqiTv.setText(weather.aqi.city.aqi);
            mPm25Tv.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        mComfortTv.setText(comfort);
        mCarWashTv.setText(carWash);
        mSportTv.setText(sport);
        mWeatherSv.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
}
