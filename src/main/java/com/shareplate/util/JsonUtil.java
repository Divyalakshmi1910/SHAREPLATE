package com.shareplate.util;

import com.google.gson.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for JSON serialization, deserialization, and standard HTTP API responses.
 * Includes flexible date/time deserialization for HTML5 datetime-local inputs.
 */
public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Timestamp.class, new JsonDeserializer<Timestamp>() {
                @Override
                public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    if (json == null || json.isJsonNull()) return null;
                    String val = json.getAsString().trim();
                    if (val.isEmpty()) return null;

                    try {
                        if (val.length() == 16) { // "2026-11-02T18:00" from HTML5 datetime-local
                            return Timestamp.valueOf(LocalDateTime.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
                        } else if (val.length() == 19 && val.contains("T")) { // "2026-11-02T18:00:00"
                            return Timestamp.valueOf(LocalDateTime.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                        } else if (val.contains(" ")) {
                            return Timestamp.valueOf(val);
                        } else {
                            String cleaned = val.replace("Z", "");
                            if (cleaned.contains(".")) {
                                cleaned = cleaned.substring(0, cleaned.indexOf('.'));
                            }
                            return Timestamp.valueOf(LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        }
                    } catch (Exception e) {
                        try {
                            return new Timestamp(json.getAsLong());
                        } catch (Exception ex) {
                            throw new JsonParseException("Unable to parse datetime: " + val, e);
                        }
                    }
                }
            })
            .registerTypeAdapter(Timestamp.class, new JsonSerializer<Timestamp>() {
                @Override
                public JsonElement serialize(Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == null) return JsonNull.INSTANCE;
                    return new JsonPrimitive(src.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                }
            })
            .serializeNulls()
            .create();

    public static Gson getGson() {
        return gson;
    }

    /**
     * Reads JSON body from HttpServletRequest and converts it to the specified class.
     */
    public static <T> T fromJson(HttpServletRequest request, Class<T> classOfT) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString().trim();
        if (body.isEmpty()) {
            return null;
        }
        return gson.fromJson(body, classOfT);
    }

    /**
     * Writes object as JSON to HttpServletResponse with UTF-8 encoding.
     */
    public static void sendJson(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
            out.flush();
        }
    }

    /**
     * Sends a standardized JSON success response.
     */
    public static void sendSuccess(HttpServletResponse response, String message, Object payload) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", message);
        if (payload != null) {
            map.put("data", payload);
        }
        sendJson(response, HttpServletResponse.SC_OK, map);
    }

    /**
     * Sends a standardized JSON error response.
     */
    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        sendJson(response, statusCode, map);
    }
}
