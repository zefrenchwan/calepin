from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from os import remove,getcwd
from os.path import join 
from shutil import rmtree

conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")
conf.set("spark.sql.warehouse.dir","spark_md")
conf.set("spark.sql.catalogImplementation","hive")

with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    spark.sql("create database db")
    spark.sql("use db")

    # UNMANAGED table 
    path = join(getcwd(), "data", "sales.csv").replace("\\", "/")
    spark.sql("create table t_sales(ID INT,DATE STRING,AMOUNT DOUBLE) USING CSV OPTIONS (PATH='{path}', HEADER=True)".format(path = path))
    spark.table("t_sales").show()
    
    # MANAGED TABLE
    spark.sql("create table t_roles (ID INT, ROLE STRING)")
    spark.sql("insert into t_roles values (0, 'TEST')")
    spark.sql("select * from t_roles").write.option("path", join(getcwd(), "tables", "t_roles")).saveAsTable("t_roles")

    spark.sql("drop table t_sales")
    spark.sql("drop database db CASCADE")


# clean content for a next run 
remove("derby.log")
rmtree("tables")
rmtree("spark_md")
rmtree("metastore_db")
