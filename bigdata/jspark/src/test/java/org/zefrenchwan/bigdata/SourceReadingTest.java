package org.zefrenchwan.bigdata;

import java.io.Serializable;
import java.net.URL;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SourceReadingTest implements Serializable {
    private static final long serialVersionUID = 2L;

    @Test 
    public void testUnitaryLoad() throws Exception {

        URL url = SourceReadingTest.class.getClassLoader().getResource("persons.csv");
        Assertions.assertNotNull(url);
        SparkSession session = Sessions.getSession();
        Dataset<Row> rows = session.read()
            .option("header", true).option("inferSchema",true)
            .csv(url.getPath());

        Assertions.assertEquals(6L, rows.count());
    }

    
    @Test 
    public void testUnitaryLoadWithSchema() throws Exception {
        URL url = SourceReadingTest.class.getClassLoader().getResource("persons.csv");
        Assertions.assertNotNull(url);
        
        SparkSession session = Sessions.getSession();
        final StructType schema = new StructType(new StructField[]{
            new StructField("ID", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("FNAME", DataTypes.StringType, false, Metadata.empty()),
            new StructField("LNAME", DataTypes.StringType, false, Metadata.empty())
        });

        Dataset<Row> rows = session.read()
            .option("header", true)
            .schema(schema)
            .csv(url.getPath());

        Assertions.assertEquals(6L, rows.count());

        Dataset<Person> persons = rows.map(new MapFunction<Row,Person>() {
            @Override
            public Person call(Row row) {
                final Person result = new Person();
                result.setId(row.getInt(0));
                result.setFname(row.getString(1));
                result.setLname(row.getString(2));
                return result;
            }
        }, Encoders.bean(Person.class));

        persons.select("id", "fname", "lname").show();
        Assertions.assertEquals(6L, persons.count());
    }
}
