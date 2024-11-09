from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import avg,stddev,skewness,kurtosis


conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")


with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv")
    stats = base.select("ROLE", "AMOUNT").groupBy("ROLE").agg(\
        avg("AMOUNT"),\
        stddev("AMOUNT"),\
        skewness("AMOUNT"),\
        kurtosis("AMOUNT")\
    ).withColumnRenamed("avg(AMOUNT)","avg")

    sumup = dict([ (row["ROLE"], row["avg"]) for row in stats.select("ROLE", "avg").collect()])
    expected = {'CTO': 71000.0, 'HR': 61000.0, 'Dev': 56000.0, 'CEO': 76000.0, 'Accountant': 66000.0}
    assert expected == sumup
