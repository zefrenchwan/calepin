package org.zefrenchwan.bigdata;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateMapperTest {

    @Test
    public void testNullValue() throws Exception {
        Assertions.assertNull(new DateMapper().call(null));
    }

    @Test 
    public void testMalformedValue() throws Exception {
        Assertions.assertNull(new DateMapper().call("popopopopopopopo"));
    }

    @Test 
    public void testDate() throws Exception {
        LocalDate localDate = LocalDate.of(2024, 1, 1);
        Date expected = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date date = new DateMapper().call("2024-01-01");
        Assertions.assertEquals(expected, date);
    }
}