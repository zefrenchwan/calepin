from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import avg,stddev,skewness,kurtosis
from pyspark.sql.types import IntegerType,StringType 
from pyspark.sql.functions import udf 


conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")


with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv")
    year_udf = udf(lambda v:v[0:4], StringType())
    salaries = base.withColumn("YEAR", year_udf(base["DATE"])).select("ROLE","AMOUNT","YEAR")
    salaries.show()
    agg_salaries = salaries.groupBy("ROLE").pivot("YEAR").avg("AMOUNT")
    agg_salaries.show()
    values = dict([ (row["ROLE"], row["2020"]) for row in agg_salaries.select("ROLE","2020").collect()])
    print(values)