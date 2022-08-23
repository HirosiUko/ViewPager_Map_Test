package com.example.myapplicationmaptest2;

import static com.google.android.gms.maps.UiSettings.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import me.relex.circleindicator.CircleIndicator3;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MapView mMapView;

    private ViewPager2 mPager;
    private FragmentStateAdapter pagerAdapter;
    private int num_page = 4;
    private CircleIndicator3 mIndicator;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        // get data
        if(StoreInfoHandler.requestQueue == null){
            // requestQueue 생성
            StoreInfoHandler.requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        sendRequest();

        // Map Setting.
        mMapView = findViewById(R.id.mMapView);
        mMapView.onCreate(savedInstanceState);
        handler.post(runable);

        /**
         * 가로 슬라이드 뷰 Fragment
         */

        //ViewPager2
        mPager = findViewById(R.id.mPager);
        //Adapter
        pagerAdapter = new StoreViewPaperAdapter(this, num_page);
        mPager.setAdapter(pagerAdapter);
        //Indicator
        mIndicator = findViewById(R.id.mIndicator);
        mIndicator.setViewPager(mPager);
        mIndicator.createIndicators(num_page,0);
        //ViewPager Setting
        mPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        /**
         * 이 부분 조정하여 처음 시작하는 이미지 설정.
         * 2000장 생성하였으니 현재위치 1002로 설정하여
         * 좌 우로 슬라이딩 할 수 있게 함. 거의 무한대로
         */

        mPager.setCurrentItem(1000); //시작 지점
        mPager.setOffscreenPageLimit(10); //최대 이미지 수

        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (positionOffsetPixels == 0) {
                    mPager.setCurrentItem(position);
                }
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mIndicator.animatePageSelected(position%num_page);
            }
        });

    }

    private void sendRequest() {
        // 서버에 요청할 주소
        String url = "https://dokkydokky.herokuapp.com/getStoreByGPS?lat=35.1465533&lon=126.9222613&dis=1500";

        // 요청 문자열 저장
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            // 응답데이터를 받아오는 곳
            @Override
            public void onResponse(String response) {
                Log.v("resultValue",response);
                try {
                    JSONArray jsonArray = new JSONArray (response);
                    for(int i=0; i< jsonArray.length(); i++)
                    {
                        JSONObject jsonObject = (JSONObject) jsonArray.opt(i);

                        String[] strGps = jsonObject.optString("GPS").split(",");
                        StoreInfo store = new StoreInfo(
                                jsonObject.optString("업소명"),
                                Float.parseFloat(strGps[0]),
                                Float.parseFloat(strGps[1]),
                                jsonObject.optString("소재지"),
                                jsonObject.optString("별점")
                        );
                        StoreInfoHandler storeInfoHandler = StoreInfoHandler.getInstance();
                        storeInfoHandler.addStore(store);
                        Log.d("호준", "onResponse: "+ store.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            // 서버와의 연동 에러시 출력
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        })
        {
            @Override //response를 UTF8로 변경해주는 소스코드
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String utf8String = new String(response.data, "UTF-8");
                    return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    // log error
                    return Response.error(new ParseError(e));
                } catch (Exception e) {
                    // log error
                    return Response.error(new ParseError(e));
                }
            }
            // 보낼 데이터를 저장하는 곳
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
////                BreakIterator edt_id;
//                params.put("id",edt_join_id.getText().toString());
//                params.put("pw",edt_join_pw.getText().toString());
//                params.put("name", edt_join_name.getText().toString());
                return params;
            }
        };
        stringRequest.setTag("ai");
        StoreInfoHandler.requestQueue.add(stringRequest);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Set the map coordinates to Kyoto Japan.
        LatLng kyoto = new LatLng(35.00116, 135.7681);
        // Set the map type to Hybrid.
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // Add a marker on the map coordinates.
        googleMap.addMarker(new MarkerOptions()
                .position(kyoto)
                .title("Kyoto")).showInfoWindow();
        // Move the camera to the map coordinates and zoom in closer.
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(kyoto));
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        // Display traffic.
        googleMap.setTrafficEnabled(false);
        googleMap.setBuildingsEnabled(true);
    }

    public Runnable runable = new Runnable() {
        @Override
        public void run() {
            StoreInfoHandler storeInfoHandler = StoreInfoHandler.getInstance();
            if(storeInfoHandler.getCurrent_state() == StoreInfoHandler.State.NORMAL){
                mMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(@NonNull GoogleMap googleMap) {
                        mMap = googleMap;
                        // Set the map coordinates
                        StoreInfo storeInfo = storeInfoHandler.getStore_list().get(0);
                        LatLng kyoto = new LatLng(storeInfo.latitude, storeInfo.longitude);
                        // Set the map type to Hybrid.
                        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        // Add a marker on the map coordinates.
                        googleMap.addMarker(new MarkerOptions()
                                .position(kyoto)
                                .title(storeInfo.storeName)
                                .snippet(storeInfo.address + " : "+storeInfo.star_of_cleanliness)).showInfoWindow();
                        // Move the camera to the map coordinates and zoom in closer.
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(kyoto));
                        googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                        // Display traffic.
                        googleMap.setTrafficEnabled(false);
                        googleMap.setBuildingsEnabled(true);
                    }
                });
            }else{
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}