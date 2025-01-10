For french readers: **Comme la certification se passe en anglais, le texte sera lui aussi en anglais** 

# Introduction 

Example of architecture is: 
1. external apps produce incoming events, processed with pub/sub (pub sub ingestion). 
2. some files are stored, and then produce a storage event, also processed with pub sub. 
3. Then events launch a pipeline, using dataflow
4. It ends into Big Query, seen as a sink
5. This BQ storage allows to do analytics (big query analytics) or data viz (using dashboards)

Then, let us dig into details. 

# Storage

## Databases 

Is data structured ? 
If not: then general option is *cloud storage*, and *firebase storage* is dedicated to storage for mobiles and webapps. 
Firebase uses GCS under the hood, but it deals with security and transfers stops and go. 
 
Let us discuss now structured data only. 
If we intend to use it as analytics material, options are: 
* **big query** if latency is not an issue 
* for very low latency, **big table** is the best option 


For non analytical solutions: 
* for SQL, **cloud SQL** will provide managed databases (postgresql, mysql, etc). If horizontal scaling matters, **cloud spanner** is multi region.
* for NO SQL, it may be mobile based apps (**firebase realtime db**), as a cache (**memorystore**) or general purpose (**firestore**)

### SQL Databases 

First of all, SQL databases are excellent to deal with strict schemas, complex queries, and vertical scaling. 

**Cloud SQL** is basically managed databases instances of Mysql, postgresql, and SQL Server. 
Managed means automated instances creation, backups, updates. 
Other elements are points in time recovery and vertical scaling (up to 128 cores and 824 Gb). 
High availability is dealt within the same region but different zones (failover and primary instance). 


**Cloud Spanner** is a proprietary SQL database, with a strong typed data management. 
Its key feature is horizontal scaling, for instance to access it worldwide. 
Applications may be gaming with realtime play data, and usual use cases (retail, fintech). 

### NoSQL 

NoSQL solutions deal with flexible schemas, simple queries, and horizontal scaling. 

**Firestore** is a fully managed nosql document database. 
Usecase is large collection of small documents. 
Its predecessor was Firebase. 
It offers strong consistency and provides references (sort of primary keys). 
It is fast due to auto indexing for single fields, and may create compound indexes for performance. 

**Memorystore** is a managed redis with high availability, automatic replication and failover.
It is vertically scalable, and three tiers are possible: basic (default) standard (with replication), and standard tier with read replica. 
Typical use cases are pub sub message queue, persistent session management. 


## GCS 

It is a fully managed object storage and has a low cost. 
Each object is stored into a bucket (logical group of objects, with a globally unique name), and each object is atomic and immutable. 
There are multiple storage classes (standard, nearline, coldline and archive). 
Security is based on IAM (identity access management) and ACL (access control layer). 
Storage may be regional, dual regional or multi regional. 

## For the exam 

Questions usually test: 
* which solution is the best, based on technical or business requirements
* how to transfer data from an on premise solution to the cloud, back and forth, and what cost would it mean



# Big data ecosystem 