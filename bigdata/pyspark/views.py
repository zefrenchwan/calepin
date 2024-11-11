from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from os.path import join 
from os import getcwd

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv")
    # we may make it using sql
    spark.sql(""" 
    CREATE OR REPLACE TEMPORARY VIEW v_base USING csv
    OPTIONS (
      path {custom_path},
      header "true",
      inferSchema "true",
      mode "FAILFAST"
    )
    """, custom_path = join(getcwd(), "storage","base.csv").replace("\\","/"))

    spark.sql("select * from v_base").show()