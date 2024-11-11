from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import col,sum
from pyspark.sql.window import Window,WindowSpec
from pyspark.sql.types import StringType

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("data/sales.csv")
    base.printSchema()
    window_since_year = Window.partitionBy("YEAR").orderBy("DATE").rowsBetween(Window.unboundedPreceding,Window.currentRow)
    sales_since_year = base\
        .selectExpr("*", "substring(DATE, 0,4) as YEAR")\
        .withColumn("year_sales", sum("AMOUNT").over(window_since_year))\
        .orderBy("DATE")
    
    window_all =  Window.orderBy("DATE").rowsBetween(Window.unboundedPreceding,Window.currentRow)
    sales_since_start =  base\
        .withColumn("global_sales", sum("AMOUNT").over(window_all))\
        .orderBy("DATE")\
        .show()
     
    