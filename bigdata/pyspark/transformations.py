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

    events = spark.createDataFrame([
        Row(id= 'a', value = 'a', author='x', creation = datetime(2024, 1, 30, 12)),
        Row(id= 'b', value = 'bb', author='y', creation = datetime(2024, 1, 30, 13)),
        Row(id= 'c', value = 'ccc', author='x', creation = datetime(2024, 1, 30, 14)),
        Row(id= 'd', value = 'dddd', author='x', creation = datetime(2024, 1, 30, 15)),
    ])

    owners = spark.createDataFrame([
        Row(id= 'x', name = 'a', category='CAT_A'),
        Row(id= 'y', name = 'bb', category='CAT_B'),
        Row(id= 'z', name = 'ccc', category='CAT_C'),
    ])

    # joins, select  

    # select events.creation, owners.name, owners.cateogy 
    # from events join owners on events.author == owners.id
    events_per_author =  events.join(owners, events.author == owners.id).select(events.creation, owners.name, owners.category)

    # count, group by 
    # select category, count(*) from events_per_author  
    category_counts = events_per_author.groupBy("category").count()
    # then, collect
    category_values = dict([ (row[0],row[1]) for row in category_counts.collect()])
    assert len(category_values) == 2
    assert category_values['CAT_A'] == 3
    assert category_values['CAT_B'] == 1

    # then, perform a window to find two most recent events per author name 
    window_name_per_creation = Window.partitionBy("name").orderBy("creation")
    rank_result = events_per_author.withColumn("rank", dense_rank().over(window_name_per_creation)).where("rank <= 2").select("name", "creation")
    rank_values = [(row[0],row[1]) for row in rank_result.collect()]
    expected = [('a', datetime(2024, 1, 30, 12, 0)), ('a', datetime(2024, 1, 30, 14, 0)), ('bb', datetime(2024, 1, 30, 13, 0))]
    assert rank_values == expected
 