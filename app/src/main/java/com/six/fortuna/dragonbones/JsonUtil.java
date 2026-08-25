package com.six.fortuna.dragonbones;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DragonBonesJAVA 的 ObjectDataParser 通过 com.dragonbones.util.Dynamic 读取数据，
 * Dynamic.get() 只认识标准的 java.util.Map / java.util.List，
 * 不认识 Android 自带的 org.json.JSONObject / JSONArray。
 * 这里把 JSON 文本递归转换成 Map/List 结构，供 parseDragonBonesData / parseTextureAtlasData 使用。
 */
public class JsonUtil {

    public static Object parse(String jsonText) throws org.json.JSONException {
        jsonText = jsonText.trim();
        if (jsonText.startsWith("{")) {
            return toMap(new JSONObject(jsonText));
        } else if (jsonText.startsWith("[")) {
            return toList(new JSONArray(jsonText));
        } else {
            throw new org.json.JSONException("Not a JSON object or array");
        }
    }

    private static Map<String, Object> toMap(JSONObject obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, convert(obj.opt(key)));
        }
        return map;
    }

    private static List<Object> toList(JSONArray arr) {
        List<Object> list = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            list.add(convert(arr.opt(i)));
        }
        return list;
    }

    private static Object convert(Object value) {
        if (value instanceof JSONObject) {
            return toMap((JSONObject) value);
        } else if (value instanceof JSONArray) {
            return toList((JSONArray) value);
        } else if (value == JSONObject.NULL) {
            return null;
        } else {
            return value; // String, Number (Double/Integer/Long), Boolean already fine
        }
    }
}
