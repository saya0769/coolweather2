package com.example.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.R;
import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.io.PipedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.internal.Util;

// 碎片 choose_area.xml 碎片
public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    // 省列表
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    // 选中的省
    private Province selectedProvince;
    private City selectedCity;
    // 当前选中的级别
    private int currentLevel;

    // 标题
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        Log.d("onCreateView!!!!!!!!", "111111111111111");

        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("ActivityCreated!!!!!!!", "111111111111111");
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    Log.d("ActivityCreated!!!!!!!", "22222222");
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    Log.d("ActivityCreated!!!!!!!", "3333333333");
                    selectedCity = cityList.get(position);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    Log.d("ActivityCreated!!!!!!!", "44444444");
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    Log.d("ActivityCreated!!!!!!!", "5555555555");
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }


    // 查询全国所有的省，优先从数据库查询，没有就到网络查询。
    private void queryProvinces() {
        Log.d("queryProvinces!!!!!!!", "111111111");

        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            Log.d("queryProvinces!!!!!!!", "2222");

            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            Log.d("queryProvinces!!!!!!!", "333333");
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    // 查询选中省内所有的市。
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    // 市内所有的县。
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE); // hide back
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    // 根据传入的地址和类型从服务器上查询省市县数据!!!!!!!!!!!!!!!!
    private void queryFromServer(String address, final String type) {
        showProressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
               String responseText = response.body().string();
               boolean result = false;
                Log.d("okhttp!!!!!!!!", "1111111111");
                if ("province".equals(type)) {
                    Log.d("okhttp!!!!!!!!", "province");
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    Log.d("okhttp!!!!!!!!", "city");
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    Log.d("okhttp!!!!!!!!", "county");
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("okhttp!!!!!!!!", "getActivity1111");
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                Log.d("okhttp!!!!!!!!", "getActivity2222");
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                Log.d("okhttp!!!!!!!!", "getActivity333333");
                                queryCities();
                            } else if ("county".equals(type)) {
                                Log.d("okhttp!!!!!!!!", "getActivity44444");
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 通过 runOnUiThread() 方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 显示进度对话框
    private void showProressDialog() {
        if (progressDialog == null) {
            Log.d("showDialog!!!!!!!", "11111111");
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        Log.d("showDialog!!!!!!!", "22222222222");

        progressDialog.show();
    }

    // 关闭进度对话框
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}




























