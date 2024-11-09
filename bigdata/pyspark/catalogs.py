from pyspark import SparkContext
from pyspark.sql.session import SparkSession
from pyspark.sql import SQLContext
from pyspark.sql import Row
from pyspark import SparkConf

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    persons = spark.createDataFrame([
        Row(id = 0, fname = 'John', lname = 'Doe'),
        Row(id = 1, fname = 'Jane', lname = 'Dilala'),
        Row(id = 2, fname = 'Paul', lname = 'Potato'),
    ])

    tables = spark.catalog.listTables()
    assert len(tables) == 0
    persons.createOrReplaceTempView("persons")
    tables = spark.catalog.listTables()
    assert len(tables) == 1
    assert tables[0].name == 'persons' and tables[0].isTemporary is True
    spark.catalog.dropTempView("persons")
    tables = spark.catalog.listTables()
    assert len(tables) == 0