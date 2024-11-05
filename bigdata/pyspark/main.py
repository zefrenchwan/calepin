from pyspark import SparkContext
from pyspark.sql.session import SparkSession
from pyspark.sql import SQLContext
from pyspark.sql import Row
from pyspark import SparkConf

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")
spark = SparkSession.builder.config(conf = conf).getOrCreate()

rows = [
    Row(id = 'a', fname = 'John', lname = 'Doe'),
]

df = spark.createDataFrame(rows)

df.show()