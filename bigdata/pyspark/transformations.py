from pyspark import SparkContext
from pyspark.sql.session import SparkSession
from pyspark.sql import SQLContext
from pyspark.sql import Row
from pyspark import SparkConf
from datetime import datetime

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:

    events = spark.createDataFrame([
        Row(id= 'a', value = 'a', author='x', creation = datetime(2024, 01, 30, 12)),
        Row(id= 'b', value = 'bb', author='y', creation = datetime(2024, 01, 30, 13)),
        Row(id= 'c', value = 'ccc', author='x', creation = datetime(2024, 01, 30, 14)),
        Row(id= 'd', value = 'dddd', author='x', creation = datetime(2024, 01, 30, 15)),
    ])

    owners = spark.createDataFrame([
        Row(id= 'x', name = 'a', category='CAT_A'),
        Row(id= 'y', name = 'bb', author='CAT_B'),
        Row(id= 'z', name = 'ccc', author='CAT_C'),
    ])
