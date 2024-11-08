# pyspark

A project to play with pyspark

## Installation 

I use windows so here are general steps:


1. Install hadoop and set `HADOOP_HOME`
2. Get `winutils.exe` and `hadoop.dll` and copy it into `HADOOP_HOME\bin`
3. Install Python and set `PYSPARK_PYTHON` to `python`
4. Install Java and set `JAVA_HOME`
5. Add HADOOP_HOME\bin and JAVA_HOME\bin to path


Pyspark may face weird errors, here are the versions that work for me:


| Part | Version |
|-------|--------|
| Hadoop | 3.3.6 |
| Winutils.exe and Hadoop.dll | for 3.3.6 |
| Java | 11 |
| Python | 3.11.8 |