package org.zefrenchwan.bigdata;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapTest implements Serializable{

    @Test 
    public void testMapCustom() {
        Dataset<Row> rows = Sessions.getSession()
            .read()
            .option("header",true).option("inferSchema", true)
            .csv(MapTest.class.getClassLoader().getResource("persons.csv").getPath())
            .select("ID","FNAME","LNAME");

        StructType newSchema = new StructType(new StructField[]{
            new StructField("ID",DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("NAME",DataTypes.StringType, false, Metadata.empty())
        });

        Dataset<Row> names = rows.map(new MapFunction<Row,Row>() {
            @Override
            public Row call(Row value) throws Exception {
                return RowFactory.create(value.getInt(0), value.getString(1) + ":" + value.getString(2));
            }
        }, Encoders.row(newSchema));

        names.show();

        List<String> values = 
        names
            .orderBy("ID")
            .select("NAME")
            .collectAsList()
            .stream()
            .map(x -> x.getString(0))
            .collect(Collectors.toList());

        List<String> expected = Arrays.asList(
            "John:Doe", "Jane:Doe", "Robert:Durand", "François:Dupont", "Nicolas:Fabre", "Adrian:Smith"
        );

        Assertions.assertEquals(expected, values);
    }

}
