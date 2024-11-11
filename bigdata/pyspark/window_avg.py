from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import avg,dense_rank,col
from pyspark.sql.window import Window
from pyspark.sql.types import StringType

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv")
    # add year, note that there is no UDF 
    year_base = base.selectExpr("*", "substring(DATE,0,4) as YEAR")
    # include avg for each year and compare with current salary
    window = Window.partitionBy('YEAR')
    diff_per_role = year_base\
        .withColumn("avg", avg("AMOUNT").over(window))\
        .selectExpr("*", "(AMOUNT - avg) as diff")\
        .select("DATE", "ROLE", "NAME", "AMOUNT", "diff")
    diff_per_role.show()
    # now, include rank to find, each year, who gets most of the money
    window = Window.partitionBy('YEAR').orderBy(col("avg").desc())
    year_base\
        .groupBy("YEAR","ROLE").avg("AMOUNT")\
        .withColumnRenamed("avg(AMOUNT)", "avg")\
        .withColumn("rank", dense_rank().over(window))\
        .orderBy("YEAR","rank")\
        .show()