package org.zefrenchwan.bigdata;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SessionTest {

    @Test
    public void testSession() {
        SparkSession session = Sessions.getSession();
        StructType schema = new StructType(new StructField[]{
            new StructField("id", DataTypes.IntegerType, false, Metadata.empty()), 
            new StructField("role", DataTypes.StringType, false, Metadata.empty()),
            new StructField("pid", DataTypes.IntegerType, false, Metadata.empty()),
        });

        List<Row> source = Arrays.asList(
            RowFactory.create(0, "ceo",17),
            RowFactory.create(1, "hr",18),
            RowFactory.create(2, "dev",19)
        );

        Dataset<Row> df = session.createDataFrame(source, schema);
        df.show();

        Assertions.assertEquals(3L, df.count()); 
    }
}
