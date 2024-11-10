from pyspark import SparkContext
from pyspark.sql.session import SparkSession
from pyspark.sql import SQLContext
from pyspark.sql import Row
from pyspark import SparkConf

class Person:
    """
    Basic class to deal with RDD
    """
    def __init__(self, id: int, name:str):
        self.id = id
        self.name = name 

    def __str__(self):
        return "Person: {id} -> {name}".format(id = self.id, name = self.name)
    
    def __repr__(self):
        return str(self)


conf = SparkConf().setMaster("local[1]").setAppName("test sql")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    # sc = SparkContext(master = "local[1]", appName="test rdd") would crash, multiple spark contexts not allowed, and spark already uses one 
    sc = spark.sparkContext

    values = [ Person(0, "John Doe"), Person(1, "Jane Doe")]

    # rdd_persons is indeed a rdd of persons 
    rdd_persons = sc.parallelize(values)
    for v in rdd_persons.take(2):
        print(v)

    # createDataFrame will create a dataframe of rows
    df = spark.createDataFrame(rdd_persons)
    df.show()
    # note that id is typed as long, with nullable set to true, and name as a nullable string 
    df.printSchema()
    # we may force the schema when creating the dataframe, though

    # get the rdd. No matter underlying type, it returns a dataframe of rows
    rdd_rows = df.rdd
    for v in rdd_rows.take(2):
        print(v)

    # but to deal with underlying type, just map
    rdd_persons_back = rdd_rows.map(lambda row: Person(row["id"], row["name"]))
    for v in rdd_persons_back.take(2):
        print(v)
