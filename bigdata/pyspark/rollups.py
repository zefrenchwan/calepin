from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import udf


conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")


with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    data = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv/")
    year_udf = udf(lambda d:d[0:4])
    salaries = data\
        .withColumn("YEAR", year_udf(data["date"]))\
        .select("YEAR","DATE","ROLE","AMOUNT")
    
    # amounts by ROLE and YEAR using a simple group by  
    # salaries.groupBy("ROLE","YEAR").avg("AMOUNT").orderBy("YEAR","ROLE").show()
    # to roll up per year    
    # salaries.rollup("YEAR","DATE").avg("AMOUNT").orderBy("YEAR","DATE").show()
    # to cube it all 
    # data.cube("DATE","ROLE","NAME").avg("AMOUNT").orderBy("DATE","ROLE","NAME").show()

    salaries.createOrReplaceTempView("salaries")

    spark.sql("""
        SELECT YEAR, DATE, ROLE, avg(AMOUNT) as avg
        FROM salaries
        GROUP BY 
            GROUPING SETS (
                (),
                (YEAR),
                (ROLE),
                (DATE),
                (YEAR, ROLE),
                (DATE, ROLE)
            )
        ORDER BY YEAR, DATE, ROLE
    """).show()

    spark.catalog.dropTempView("salaries")