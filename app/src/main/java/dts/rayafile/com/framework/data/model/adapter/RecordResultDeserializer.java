package dts.rayafile.com.framework.data.model.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dts.rayafile.com.framework.data.model.sdoc.RecordResultModel;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class RecordResultDeserializer implements JsonDeserializer<RecordResultModel> {
    @Override
    public RecordResultModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        RecordResultModel result = new Gson().fromJson(json, RecordResultModel.class);
        JsonObject jsonObject = json.getAsJsonObject();

        Map<String, Object> dynamicFields = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();

            if (!isFixedField(key)) {
                dynamicFields.put(key, context.deserialize(entry.getValue(), Object.class));
            }
        }

        result.dynamicFields = dynamicFields;
        return result;
    }

    private boolean isFixedField(String key) {
        return key.startsWith("_");
    }
}
