package ru.improve.place;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class CreateDto {

    private static ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static Object create(Response response, Class clazz) throws IOException {
        JSONObject jsonObject = new JSONObject(response.body().string());
        return objectMapper.readValue(jsonObject.toString(), clazz);
    }
}
