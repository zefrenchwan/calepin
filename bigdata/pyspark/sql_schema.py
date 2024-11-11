from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.types import StructField,StructType,StringType,IntegerType,DoubleType
from pyspark.sql import Row

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("data/sales.csv")
    base.printSchema()
    schema = "ID INT, DATE STRING, AMOUNT DOUBLE"
    base = spark.read.option("header",True).schema(schema).csv("data/sales.csv")
    base.printSchema()
    schema = StructType([
        StructField("ID",IntegerType()),
        StructField("DATE",StringType()),
        StructField("AMOUNT",DoubleType()),
    ])
    base = spark.read.option("header",True).schema(schema).csv("data/sales.csv")
    base.printSchema()

    schema = StructType([
        StructField("id",IntegerType(), False),
        StructField("name",StringType(), False ),
        StructField("age",IntegerType(), False),
    ])
    simple = spark.createDataFrame([Row(id = 0, name = 'John Doe', age = 21)], schema = schema)
    simple.printSchema()
    