package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    // 注解得方式来让 JSON 字段 和  Java 字段之间建立映射关系。
    @SerializedName("city")
    public String cityName;
    @SerializedName("id")
    public String weatherId;
    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;

    }

}




















