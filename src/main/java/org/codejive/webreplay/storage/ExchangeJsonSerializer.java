package org.codejive.webreplay.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codejive.tproxy.Headers;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;
import org.codejive.webreplay.RecordedExchange;

/** JSON serialization utilities for converting RecordedExchange objects to/from JSON using Gson. */
class ExchangeJsonSerializer {
    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(RecordedExchange.class, new RecordedExchangeAdapter())
                    .registerTypeAdapter(ProxyRequest.class, new ProxyRequestAdapter())
                    .registerTypeAdapter(ProxyResponse.class, new ProxyResponseAdapter())
                    .registerTypeAdapter(Headers.class, new HeadersAdapter())
                    .registerTypeAdapter(Instant.class, new InstantAdapter())
                    .create();

    /**
     * Serializes a RecordedExchange to JSON string.
     *
     * @param exchange the exchange to serialize
     * @return JSON string representation
     */
    static String toJson(RecordedExchange exchange) {
        return GSON.toJson(exchange);
    }

    /**
     * Deserializes a RecordedExchange from JSON string.
     *
     * @param json the JSON string
     * @return the deserialized RecordedExchange
     */
    static RecordedExchange fromJson(String json) {
        return GSON.fromJson(json, RecordedExchange.class);
    }

    /** Gson adapter for RecordedExchange. */
    private static class RecordedExchangeAdapter
            implements JsonSerializer<RecordedExchange>, JsonDeserializer<RecordedExchange> {
        @Override
        public JsonElement serialize(
                RecordedExchange src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.add("request", context.serialize(src.request()));
            obj.add("response", context.serialize(src.response()));
            obj.add("recordedAt", context.serialize(src.recordedAt()));
            return obj;
        }

        @Override
        public RecordedExchange deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            ProxyRequest request = context.deserialize(obj.get("request"), ProxyRequest.class);
            ProxyResponse response = context.deserialize(obj.get("response"), ProxyResponse.class);
            Instant recordedAt = context.deserialize(obj.get("recordedAt"), Instant.class);
            return new RecordedExchange(request, response, recordedAt);
        }
    }

    /** Gson adapter for ProxyRequest. */
    private static class ProxyRequestAdapter
            implements JsonSerializer<ProxyRequest>, JsonDeserializer<ProxyRequest> {
        @Override
        public JsonElement serialize(
                ProxyRequest src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("method", src.method());
            obj.addProperty("uri", src.uri().toString());
            obj.add("headers", context.serialize(src.headers()));
            obj.addProperty("body", Base64.getEncoder().encodeToString(src.body()));
            return obj;
        }

        @Override
        public ProxyRequest deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String method = obj.get("method").getAsString();
            URI uri = URI.create(obj.get("uri").getAsString());
            Headers headers = context.deserialize(obj.get("headers"), Headers.class);
            byte[] body = Base64.getDecoder().decode(obj.get("body").getAsString());
            return ProxyRequest.fromBytes(method, uri, headers, body);
        }
    }

    /** Gson adapter for ProxyResponse. */
    private static class ProxyResponseAdapter
            implements JsonSerializer<ProxyResponse>, JsonDeserializer<ProxyResponse> {
        @Override
        public JsonElement serialize(
                ProxyResponse src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("statusCode", src.statusCode());
            obj.add("headers", context.serialize(src.headers()));
            obj.addProperty("body", Base64.getEncoder().encodeToString(src.body()));
            return obj;
        }

        @Override
        public ProxyResponse deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int statusCode = obj.get("statusCode").getAsInt();
            Headers headers = context.deserialize(obj.get("headers"), Headers.class);
            byte[] body = Base64.getDecoder().decode(obj.get("body").getAsString());
            return ProxyResponse.fromBytes(statusCode, headers, body);
        }
    }

    /** Gson adapter for Headers. */
    private static class HeadersAdapter
            implements JsonSerializer<Headers>, JsonDeserializer<Headers> {
        @Override
        public JsonElement serialize(
                Headers src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            Map<String, List<String>> headerMap = src.toMap();
            for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
                JsonArray array = new JsonArray();
                for (String value : entry.getValue()) {
                    array.add(value);
                }
                obj.add(entry.getKey(), array);
            }
            return obj;
        }

        @Override
        public Headers deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Map<String, List<String>> headerMap = new HashMap<>();
            for (String key : obj.keySet()) {
                JsonArray array = obj.getAsJsonArray(key);
                List<String> values = new java.util.ArrayList<>();
                for (JsonElement elem : array) {
                    values.add(elem.getAsString());
                }
                headerMap.put(key, values);
            }
            return Headers.of(headerMap);
        }
    }

    /** Gson adapter for Instant. */
    private static class InstantAdapter
            implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(
                Instant src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("epochSecond", src.getEpochSecond());
            obj.addProperty("nano", src.getNano());
            return obj;
        }

        @Override
        public Instant deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            long epochSecond = obj.get("epochSecond").getAsLong();
            int nano = obj.get("nano").getAsInt();
            return Instant.ofEpochSecond(epochSecond, nano);
        }
    }
}
