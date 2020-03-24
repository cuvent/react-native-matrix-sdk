package com.reactlibrary;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class RNJson {

    public static WritableMap convertJsonToMap(JsonObject jsonObject) {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JsonObject) value));
            } else if (value instanceof  JSONArray) {
                map.putArray(key, convertJsonToArray((JsonArray) value));
            } else if (value instanceof  Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof  Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof  Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String)  {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }


    public static WritableArray convertJsonToArray(JsonArray jsonArray) {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JsonObject) value));
            } else if (value instanceof  JSONArray) {
                array.pushArray(convertJsonToArray((JsonArray) value));
            } else if (value instanceof  Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof  Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof  Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String)  {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    public static JsonObject convertMapToJson(ReadableMap readableMap) {
        JsonObject object = new JsonObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.add(key, JsonNull.INSTANCE);
                    break;
                case Boolean:
                    object.addProperty(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.addProperty(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.addProperty(key, readableMap.getString(key));
                    break;
                case Map:
                    object.add(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.add(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    public static JsonArray convertArrayToJson(ReadableArray readableArray) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.add(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.add(readableArray.getDouble(i));
                    break;
                case String:
                    array.add(readableArray.getString(i));
                    break;
                case Map:
                    array.add(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.add(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }
}
