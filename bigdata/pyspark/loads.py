from pyspark import SparkContext
from pyspark.sql.session import SparkSession
from pyspark.sql import SQLContext
from pyspark.sql import Row
from pyspark import SparkConf
from datetime import datetime
from pyspark.sql.window import Window
from pyspark.sql.functions import dense_rank


conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")


with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    persons = spark.read.option("inferSchema", True).option("header",True).csv("data/persons.csv")
    persons.show()
    roles = spark.read.option("inferSchema", True).option("header",True).csv("data/roles.csv")
    roles.show()
    salaries = spark.read.option("inferSchema", True).option("header",True).csv("data/salaries.csv")
    salaries.show()