package com.example.myapplicationmaptest2;

import static com.google.android.gms.maps.UiSettings.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import me.relex.circleindicator.CircleIndicator3;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;
    private MapView mMapView;

    private ViewPager2 mPager;
    private FragmentStateAdapter pagerAdapter;
    private int num_page = 10;
    private CircleIndicator3 mIndicator;
    private Handler handler;
    private Boolean isInitial;

    private TextView tv_current_gps;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isInitial = true;
        handler = new Handler();

        // get data
        if (StoreInfoHandler.requestQueue == null) {
            // requestQueue ??????
            StoreInfoHandler.requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        sendRequest();

        // Map Setting.
        mMapView = findViewById(R.id.mMapView);
        mMapView.onCreate(savedInstanceState);
        handler.post(runable);

        tv_current_gps = findViewById(R.id.tv_CUR_GPS);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("??????", "onCreate: ???????????????");
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        });

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        Log.d("??????", "onSuccess: ");
                        if (location != null) {
                            Log.d("??????", "GPS??????" + location.getLatitude() + ":" + location.getLongitude());
                            tv_current_gps.setText("?????? GPS : "+location.getLatitude() + ":" + location.getLongitude());
                        }else
                        {
                            Log.d("??????", "GPS?????????");
                            tv_current_gps.setText("GPS?????????");
                        }
                    }
                });
    }

    private void initMap()
    {
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;
                // Set the map coordinates
                StoreInfoHandler storeInfoHandler = StoreInfoHandler.getInstance();
                StoreInfo storeInfo = storeInfoHandler.getStore_list().get(0);
                moveMap(storeInfo);
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                googleMap.setTrafficEnabled(false);
                googleMap.setBuildingsEnabled(true);
            }
        });
    }

    private void initContents()
    {
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

        mPager.setCurrentItem(1); //?????? ??????
        mPager.setOffscreenPageLimit(StoreInfoHandler.getInstance().getStore_list().size()); //?????? ????????? ???

        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }

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
                Log.d("??????", String.format("onPageSelected: %d", position));
                if(isInitial != true){
                    moveMap(StoreInfoHandler.getInstance().getStore_list().get(position));
                }
                isInitial = false;
            }
        });
    }

    private static void moveMap(StoreInfo storeInfo)
    {
        Log.d("??????", "moveMap: "+storeInfo.toString());
        LatLng loc = new LatLng(storeInfo.latitude, storeInfo.longitude);
        //  Add a marker on the map coordinates.
        mMap.addMarker(new MarkerOptions()
                .position(loc)
                .title(storeInfo.storeName)
                .snippet(storeInfo.address + " : "+storeInfo.star_of_cleanliness)).showInfoWindow();
        // Move the camera to the map coordinates and zoom in closer.
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
//        mMap.moveCamera(CameraUpdateFactory.zoomTo(19));
        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                loc, 19);
        mMap.animateCamera(location);
    }

    private void sendRequest() {
        // ????????? ????????? ??????
        String url = "https://dokkydokky.herokuapp.com/getStoreByGPS?lat=35.1465533&lon=126.9222613&dis=1500";

        // ?????? ????????? ??????
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            // ?????????????????? ???????????? ???
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
                                jsonObject.optString("?????????"),
                                Float.parseFloat(strGps[0]),
                                Float.parseFloat(strGps[1]),
                                jsonObject.optString("?????????"),
                                jsonObject.optString("??????")
                        );
                        StoreInfoHandler storeInfoHandler = StoreInfoHandler.getInstance();
                        storeInfoHandler.addStore(store);
                        Log.d("??????", "onResponse: "+ store.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            // ???????????? ?????? ????????? ??????
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        })
        {
            @Override //response??? UTF8??? ??????????????? ????????????
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
            // ?????? ???????????? ???????????? ???
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
//        // Set the map coordinates to Kyoto Japan.
//        LatLng kyoto = new LatLng(35.00116, 135.7681);
//        // Set the map type to Hybrid.
//        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//        // Add a marker on the map coordinates.
//        googleMap.addMarker(new MarkerOptions()
//                .position(kyoto)
//                .title("Kyoto")).showInfoWindow();
//        // Move the camera to the map coordinates and zoom in closer.
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(kyoto));
//        googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
//        // Display traffic.
//        googleMap.setTrafficEnabled(false);
//        googleMap.setBuildingsEnabled(true);
//        mMapView.setCameraDistance(1.5F);
    }

    public Runnable runable = new Runnable() {
        @Override
        public void run() {
            StoreInfoHandler storeInfoHandler = StoreInfoHandler.getInstance();
            if(storeInfoHandler.getCurrent_state() == StoreInfoHandler.State.NORMAL){
                initMap();
                initContents();
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