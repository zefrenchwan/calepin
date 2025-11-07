# Why would I need that tool ? 

It is an **open source batch workflow platform** that may be deployed from a laptop to a distributed system. To interact with it, options are a web UI and use its python framework. Idea is **coding over clicking**. 


Its purpose is to organize actions, to orchestrate them. For instance, run a given task every other hour. Separating tasks with an orchestrator may help to rerun parts of a pipeline after a failure: we fix the error and rerun what failed and what was expected after. When using it a long time, we may then check it and get run statistics. Its force is to be open source and then interact with a lot of other systems (database, file systems). **Though, it is not a good solution for all the cases. We may not use it for streaming, event driven workflows, not infinite sources**. 


For instance, a given data provider updates its content each morning at 4AM. Idea is to define tasks linked together: 
1. One to connect to the provider API and load content 
2. One to transform that content into a suitable representation 
3. A final one to store that content into a database

And then be able to automate launches. 
Airflow will also provide stats, status, etc. 
More to come later, but the web UI will help see those stats and tasks links. 


# Installation 

The simplest installation tool is using pip. 
But... It does not work on windows OS, claiming that it needs a posix compliant system. 
Options are docker or WSL2.
Let us focus on docker and install docker desktop. 

## Step one: install docker desktop

* Nothing complex for the install process per se. 
* Next action is to add docker in path. Location (for default install) is `C:\Program Files\Docker\Docker\resources\bin`

## Step two: launch airflow

**You just do not run airflow from its image, you need to use a predefined docker compose file**. 
Installation is not a click and run process, it needs to follow the [guide](https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html) by the book. 

0. Ensure docker desktop is up (otherwise loading images would just crash)
1. Get docker compose file from the official guide page. I made some small changes to use a less old version of postgresql
2. `docker compose up airflow-init` will do the magic for the default installation
3. `docker compose up` will start airflow 

To be sure it works, open the `localhost:8080` webpage. 
Default login and password are `airflow` (source: [here](https://airflow.apache.org/docs/apache-airflow/stable/tutorial/pipeline.html)).


## Step three: install environment to develop 

Many possible solutions for deal with the python installation. 
Solution in here will be the classical virtual environment with an install: `pip install apache-airflow`. 



# Sources 

* *Apache Airflow Fundamentals*, by Alfredo Deza
* *Official documentation* : https://airflow.apache.org/docs/