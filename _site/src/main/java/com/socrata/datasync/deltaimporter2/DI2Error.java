package com.socrata.datasync.deltaimporter2;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DI2Error {
    public ErrorType type;
    public Map<String, Object> data;

    @JsonDeserialize(using=ErrorTypeDeserializer.class)
    public enum ErrorType {
        NOT_A_COMMIT_DESCRIPTION("not-a-commit-description"),
        FILENAME_BAD_CHARACTERS("filename-bad-characters"),
        FILENAME_TOO_LONG("filename-too-long"),
        IMPOSSIBLE_RELATIVE_TO("impossible-relative-to"),
        UNDECODABLE_CONTROL("undecodable-control"),
        NON_UNIFORM_CHUNK("non-uniform-chunk"),
        NONEXISTANT_CHUNK("non-existant-chunk"),
        SIZE_MISMATCH("size-mismatch");

        public final String tag;

        ErrorType(String tag) {
            this.tag = tag;
        }
    }

    public static class ErrorTypeDeserializer extends JsonDeserializer<ErrorType> {
        private static final Map<String, ErrorType> errorTypes;

        static {
            Map<String, ErrorType> map = new HashMap<>();
            for(ErrorType e : ErrorType.values()) {
                map.put(e.tag, e);
            }
            errorTypes = Collections.unmodifiableMap(map);
        }

        @Override
        public ErrorType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            if(jsonParser.getCurrentToken() != JsonToken.VALUE_STRING) throw deserializationContext.wrongTokenException(jsonParser, JsonToken.VALUE_STRING, "Expected string");
            String code = jsonParser.getText();
            ErrorType result = errorTypes.get(code);
            if(result == null) throw deserializationContext.weirdStringException(ErrorType.class, "Unknown error code " + code);
            return result;
        }
    }
}
