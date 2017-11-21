package fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xuyezhuangt5000.coolweather.R;
import com.example.xuyezhuangt5000.coolweather.db.City;
import com.example.xuyezhuangt5000.coolweather.db.Country;
import com.example.xuyezhuangt5000.coolweather.db.Province;
import com.example.xuyezhuangt5000.coolweather.util.HttpUtil;
import com.example.xuyezhuangt5000.coolweather.util.Utilty;

import org.litepal.crud.DataSupport;

import java.io.Closeable;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by xuyezhuangT5000 on 2017/11/20.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTRY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<Country> countryList;
    /**
     * 选中的省份
     */
    private Province selectdProvince;
    /**
     * 选中的城市
     */
    private City selectdCity;
    /**
     * 选中的县列表
     */
    private Country selectCountry;
    /**
     * 当前选中的列表
     */
    private int currentLevel;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view=View.inflate(getActivity(), R.layout.choose_area,null);
        titleText=(TextView)view.findViewById(R.id.title_text);
        backButton=(Button)view.findViewById(R.id.back_button);
        listView=(ListView)view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectdProvince = provinceList.get(i);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectdCity = cityList.get(i);
                    queryCountry();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel==LEVEL_COUNTRY){
                    queryCities();
                }else if (currentLevel==LEVEL_CITY){
                    queryProvince();
                }
            }
        });

        queryProvince();
    }
    /**
     * 查询全国所有的省，优先从数据库查询，如果没有再到服务器查询
     */
    private void queryProvince(){

        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            System.out.println(dataList.toString()+1);
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            System.out.println(dataList.toString());
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else {
            String adress="http://guolin.tech/api/china";
            queryFromServer(adress,"province");
            }
        }
    /**
     * 查询选中省内所有的城市，优先从数据库查询，如果查不到再到服务器查询
     */
    private void queryCities(){
        titleText.setText(selectdProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceId=?",String.valueOf(selectdProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else {
            int provinceCode=selectdProvince.getId();

            String address="http://guolin.tech/api/china"+"/"+provinceCode;

            queryFromServer(address,"city");
        }

    }
    /**
     * 查询选中市内的县，优先查询数据库，如果查不到再从服务器查询
     */
    private void queryCountry(){
        titleText.setText(selectdCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countryList=DataSupport.where("cityId=?",String.valueOf(selectdCity.getId())).find(Country.class);
        if (countryList.size()>0){
            dataList.clear();
            for (Country country:countryList){
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTRY;
        }else {
            int provinceCode=selectdProvince.getProvinceCode();
            int cityCode=selectdCity.getCityCode();
            String adress="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(adress,"country");
        }
    }
    /**
     * 根据传入的地址和类型从服务器上查询省市县的数据
     */
    private void queryFromServer(String adress,final String type){
        showProgressDiolog();

        HttpUtil.sendOkHttpRequest(adress, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closePregressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {

                String responseText=response.body().string();//okhttp3链接必须要的一步。转换成String，不能调用tostring方法,因为toString是String特有的方法，你还不是String类、
                boolean result=false;
                if ("province".equals(type)){
                    result= Utilty.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result=Utilty.handleCityResponse(responseText,selectdProvince.getId());
                }else if ("country".equals(type)){
                    result=Utilty.handleCountryResponse(responseText,selectdCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closePregressDialog();
                            if ("province".equals(type)){
                                queryProvince();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("country".equals(type)){
                                queryCountry();
                            }
                        }
                    });
                }
            }
        });
    }
    private void showProgressDiolog(){
        if (progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在拼了命地加载中...");
            progressDialog.setCancelable(false);//点击dialog以外区域返回键不给返回
        }
        progressDialog.show();
    }
    private void closePregressDialog(){
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
