package org.zefrenchwan.bigdata;

import java.util.Date;
import java.text.SimpleDateFormat;

import org.apache.spark.api.java.function.MapFunction;

public class DateMapper implements MapFunction<String,Date> {

    @Override
    public Date call(String value) throws Exception {
        if (value == null || "yyyy-MM-dd".length() != value.length()) {
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.parse(value);
    }
    
}
