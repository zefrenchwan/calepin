package org.zefrenchwan.bigdata;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;


public class Sessions {

    public static SparkSession getSession() {
        // spark configuration with mandatory values 
        SparkConf configuration = new SparkConf().setMaster("local[1]").setAppName("spark sql session tests");
        
        return SparkSession
            .builder()
            .appName("Java Spark SQL session")
            .config(configuration)
            .getOrCreate();
    }

}
