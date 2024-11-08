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
    roles = spark.read.option("inferSchema", True).option("header",True).csv("data/roles.csv")
    salaries = spark.read.option("inferSchema", True).option("header",True).csv("data/salaries.csv")
    # load all tables and join 
    base_data = salaries\
        .join(roles, roles["ID"] == salaries["RID"]).withColumnRenamed("NAME", "ROLE") \
        .join(persons, persons["ID"] == salaries["PID"]) \
        .select("DATE", "AMOUNT", "ROLE","NAME") 
    
    base_data.repartition(1).write.mode('overwrite').option("header", True).csv("storage/base.csv")
    # load it to be sure 
    core_data = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv/")
    assert core_data.count() >= 250