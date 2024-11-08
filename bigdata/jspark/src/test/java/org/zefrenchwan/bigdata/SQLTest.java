package org.zefrenchwan.bigdata;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SQLTest {
    
    @Test
    public void testSQL() throws Exception {
        SparkSession session = Sessions.getSession();

        Dataset<Row> persons = session
            .read()
            .option("header",true).option("inferSchema", true)
            .csv(MapTest.class.getClassLoader().getResource("persons.csv").getPath())
            .select("ID","FNAME","LNAME");

        Dataset<Row> salaries = session
            .read()
            .option("header",true).option("inferSchema", true)
            .csv(MapTest.class.getClassLoader().getResource("salaries.csv").getPath());

        Dataset<Row> base = salaries
            .join(persons, persons.col("ID").equalTo(salaries.col("PID")))
            .select("PID","FNAME","LNAME","DATE","AMOUNT").cache();

        
        // no registered tables 
        Assertions.assertTrue(session.catalog().listTables().count() == 0L);

        // register table 
        base.createOrReplaceTempView("PSAL");

        // check registration
        List<Row> dbContent = session.catalog().listTables().select("name","isTemporary").collectAsList();
        Assertions.assertEquals(1, dbContent.size());
        final Row rowTable = dbContent.get(0);
        Assertions.assertEquals("PSAL", rowTable.getString(0));
        Assertions.assertEquals(true, rowTable.getBoolean(1));

        // use table 
        long count = session.sql("select 1 from PSAL").count();
        Assertions.assertEquals(72L, count);

        // clear 
        session.catalog().dropTempView("PSAL");
        Assertions.assertTrue(session.catalog().listTables().count() == 0L);
    }
}
