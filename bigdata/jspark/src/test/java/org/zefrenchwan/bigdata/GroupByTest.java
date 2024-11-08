package org.zefrenchwan.bigdata;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GroupByTest implements Serializable {
    
    @Test
    public void testGroupBy() throws Exception {
        SparkSession session = Sessions.getSession();
        Dataset<Row> salaries = session.read()
            .option("header", true)
            .option("inferSchema", true)
            .csv(GroupByTest.class.getClassLoader().getResource("salaries.csv").getPath());
        Dataset<Row> persons = session.read()
            .option("header", true)
            .csv(GroupByTest.class.getClassLoader().getResource("persons.csv").getPath());

        Dataset<Row> values = 
            salaries
                .join(persons, salaries.col("PID").equalTo(persons.col("ID")))
                .select(
                    salaries.col("DATE"),
                    salaries.col("PID"),
                    persons.col("FNAME"), 
                    persons.col("LNAME"),
                    salaries.col("AMOUNT")
                );
        Dataset<Row> averages = values
            .groupBy("PID","FNAME","LNAME")
            .avg("AMOUNT")
            .orderBy("PID")
            .withColumnRenamed("avg(AMOUNT)", "AVG")
            .select("PID","AVG");
        averages.show();

        List<String> result = averages.collectAsList()
            .stream()
            .map(row -> Integer.toString(row.getInt(0)) + ":" + Double.toString(row.getDouble(1)))
            .collect(Collectors.toList());
        
        List<String> expected = Arrays.asList(
            "0:56500.0", "1:66500.0", "2:56500.0", "3:66500.0", "4:56500.0", "5:66500.0"
        );

        Assertions.assertEquals(expected, result);
    }
}
