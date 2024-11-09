from pyspark.sql.session import SparkSession
from pyspark.sql import SQLContext
from pyspark.sql import Row
from pyspark import SparkConf
from datetime import datetime
from pyspark.sql.window import Window
from pyspark.sql.functions import dense_rank
from pyspark.sql.types import IntegerType,StringType 
from pyspark.sql.functions import udf 
  
def role(code:int) -> str:
    match code: 
        case 0:
            return "Dev"
        case 1:
            return "CEO"
        case 2:
            return "CTO"
        case 3:
            return "Accountant"
        case 4:
            return "HR"  
        case _:
            return "" 

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    # register the udf
    spark.udf.register("role", role, StringType())
    role_udf = udf(role, StringType())

    # and then here starts the play
    salaries = spark.read\
        .option("inferSchema", True).option("header",True)\
        .csv("data/salaries.csv")
    
    # direct call to udf
    stats = salaries\
        .selectExpr("role(RID) as role", "AMOUNT")\
        .groupBy("role").avg("AMOUNT").withColumnRenamed("avg(AMOUNT)", "avgs")

    values = dict([ (row["role"],row["avgs"]) for row in stats.collect()])
    expected = {'CTO': 71000.0, 'HR': 61000.0, 'Dev': 56000.0, 'CEO': 76000.0, 'Accountant': 66000.0}
    assert values == expected
    