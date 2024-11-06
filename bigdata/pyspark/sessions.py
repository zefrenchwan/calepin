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

    events = spark.createDataFrame([
        Row(id = 'a', pid = 0, ),
        Row(id = 'b', pid = 1),
        Row(id = 'c', pid = 1),
        Row(id = 'd', pid = 2),
    ])

    assert 3 == persons.count()
    assert 4 == events.count()
    # select distinct events.pid from events order by events.pid 
    cols = events.select(events.pid).distinct().sort(events.pid)
    rows = cols.collect()
    values = [value[0] for value in rows ]
    assert values == [0,1,2]