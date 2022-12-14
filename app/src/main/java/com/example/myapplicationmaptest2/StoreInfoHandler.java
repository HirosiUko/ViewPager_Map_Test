package com.example.myapplicationmaptest2;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StoreInfoHandler {
    public enum State{
        NEVER_RECEIVED,
        SYNCING_DB,
        NORMAL
    }

    private ArrayList<StoreInfo> store_list = new ArrayList<>();

    public ArrayList<StoreInfo> getStore_list() {
        return store_list;
    }

    public void setStore_list(ArrayList<StoreInfo> store_list) {
        this.store_list = store_list;
    }

    public State getCurrent_state() {
        return current_state;
    }

    public void setCurrent_state(State current_state) {
        this.current_state = current_state;
    }

    private State current_state = State.NEVER_RECEIVED;
    public static RequestQueue requestQueue;

    // private construct
    private StoreInfoHandler() {
    }

    private static class InnerInstanceClazz {
        private static final StoreInfoHandler instance = new StoreInfoHandler();
    }

    public static StoreInfoHandler getInstance() {
        return InnerInstanceClazz.instance;
    }

    public void addStore(StoreInfo storeInfo){
        store_list.add(storeInfo);
        current_state = State.NORMAL;
    }
}
