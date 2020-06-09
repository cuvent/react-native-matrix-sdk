package de.hannojg;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class RNJson {

    public static WritableMap convertJsonToMap(JsonObject jsonObject) {
        WritableMap map = new WritableNativeMap();

        for (String key : jsonObject.keySet()) {
            JsonElement value = jsonObject.get(key);
            if (value.isJsonObject()) {
                map.putMap(key, convertJsonToMap((JsonObject) value));
            } else if (value.isJsonArray()) {
                map.putArray(key, convertJsonToArray((JsonArray) value));
            } else if (value.isJsonPrimitive()) {
                if(value.getAsJsonPrimitive().isBoolean()) {
                    map.putBoolean(key, value.getAsBoolean());
                } else if(value.getAsJsonPrimitive().isString()) {
                    map.putString(key, value.getAsString());
                } else if(value.getAsJsonPrimitive().isNumber()) {
                    if (isJsonElementInteger(value)) {
                        map.putInt(key, value.getAsInt());
                    } else {
                        map.putDouble(key, value.getAsDouble());
                    }
                }
            } else if(value.isJsonNull()) {
                map.putNull(key);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }


    public static WritableArray convertJsonToArray(JsonArray jsonArray) {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement value = jsonArray.get(i);
            if (value.isJsonObject()) {
                array.pushMap(convertJsonToMap((JsonObject) value));
            } else if (value.isJsonArray()) {
                array.pushArray(convertJsonToArray((JsonArray) value));
            } else if (value.getAsJsonPrimitive().isBoolean()) {
                array.pushBoolean(value.getAsBoolean());
            } else if (value.getAsJsonPrimitive().isNumber()) {
                if (isJsonElementInteger(value)) {
                    array.pushInt(value.getAsInt());
                } else {
                    array.pushDouble(value.getAsDouble());
                }
            } else if (value.getAsJsonPrimitive().isString())  {
                array.pushString(value.getAsString());
            } else if (value.isJsonNull()) {
                array.pushNull();
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

    private static boolean isJsonElementInteger(JsonElement element) {
        if(!element.getAsJsonPrimitive().isNumber()) {
            return false;
        }

        return ((element.getAsDouble() == Math.floor(element.getAsDouble())) && !Double.isInfinite(element.getAsDouble()));
    }
}
