package org.zefrenchwan.bigdata;

import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import static org.apache.spark.sql.functions.udf;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NewUdfTest {
    
    @Test
    public void testAddUdf() {
        // step one: create UDF 
        UserDefinedFunction normalize = udf(new UDF1<String,String>() {

            @Override
            public String call(String v) throws Exception {
                if (v == null) {
                    return null;
                } 

                final int size = v.length();
                if (size == 1) {
                    return v.toUpperCase();
                }

                return v.substring(0, 1).toUpperCase() + v.substring(1, size).toLowerCase();
            }
            
        }, DataTypes.StringType); //.withName("normalize");

        // step 2: add UDF to session
        SparkSession session = Sessions.getSession();
        session.udf().register("normalize", normalize);
        
        // and then, use it 
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

        Dataset<Row> df = session.createDataFrame(source, schema).selectExpr("normalize(role) as role");
        df.show();

        List<String> values = df.sort("role"). collectAsList().stream().map(row -> row.getString(0)).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("Ceo","Dev","Hr"), values);

    }

}
