package org.zefrenchwan.bigdata;

import java.io.Serializable;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;

public class RddBackAndForthTest implements Serializable {

    private final static long serialVersionUID = -1L;
    
    @Test
    public void testRDDDF() {
        final String path = RddBackAndForthTest.class.getClassLoader().getResource("salaries.csv").getPath();

        // first, make the rdd 
        SparkConf conf = new SparkConf().setMaster("local[1]").setAppName("test RDD");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<YearSalaryTuple> content = sc
            .textFile(path)
            .filter(v -> v != null && v.length() >= 10 && !v.equals("DATE,PID,AMOUNT"))
            .map(new Function<String,YearSalaryTuple>() {
                @Override
                public YearSalaryTuple call(String v) throws Exception {
                    String[] values = v.split(",");
                    int year = Integer.parseInt(values[0].substring(0, 4));
                    float salary = Float.parseFloat(values[2] + ".0");
                    YearSalaryTuple result = new YearSalaryTuple();
                    result.setYear(year);
                    result.setSalary(salary);
                    return result;
                }
            });

        // no show from rdd, use take and print
        content.take(10).forEach(System.out::println);

        // alright, then, map it to a dataset with a given schema 
        SparkSession session = Sessions.getSession();
        // CAUTION: it builds a row based dataset, not a dataset of yearsalarytuple 
        Dataset<Row> rows = session.createDataFrame(content, YearSalaryTuple.class);
        rows.show();            
        // to make it as a dataset of yearsalarytuple, use a map
        Dataset<YearSalaryTuple> tuples = rows.map(new MapFunction<Row,YearSalaryTuple>() {

            @Override
            public YearSalaryTuple call(Row row) {
                final YearSalaryTuple result = new YearSalaryTuple();
                result.setSalary(row.getFloat(row.fieldIndex("salary")));
                result.setYear(row.getInt(row.fieldIndex("year")));
                return result;
            }
            
        }, Encoders.bean(YearSalaryTuple.class));

        tuples.show();

        // coming back to rdd is quite easy ! 
        // just a tuples.toJavaRDD();
        

        sc.close();
        session.close();
    }

}
