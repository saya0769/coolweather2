package com.example.coolweather.db;

import org.litepal.crud.DataSupport;

// 省           LitePal 类都要 继承 DataSupport 类的
public class Province extends DataSupport {
    private int id;
    private String provinceName; // 省名
    private int provinceCode;    // 代号
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}









































