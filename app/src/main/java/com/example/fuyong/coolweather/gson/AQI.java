package com.example.fuyong.coolweather.gson;

/**
 * Created by fuyong on 2017/3/31.
 */

public class AQI {
    public AQICity city;

    public class AQICity {
        public String aqi;
        public String pm25;
    }
}
