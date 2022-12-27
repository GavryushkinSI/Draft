package ru.app.draft.convertors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomLocalDateTimeSerializer extends StdSerializer<Date> {

    private static SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");

    public CustomLocalDateTimeSerializer() {
        super(Date.class);
    }

    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider arg2) throws IOException {
        gen.writeString(ft.format(value));
    }
}