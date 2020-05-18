package edu.nju.seg.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtil {

    private static final Gson g = new GsonBuilder().setPrettyPrinting().create();

    /**
     * convert json string to object
     * @param json json string
     * @param type the type information
     * @param <T> generic type variable
     * @return the structural object
     */
    public static <T> T fromJson(String json, Class<T> type)
    {
        return g.fromJson(json, type);
    }

    /**
     * convert object to json string
     * @param o object
     * @return json string
     */
    public static String toJson(Object o)
    {
        return g.toJson(o);
    }

}
