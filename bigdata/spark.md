# Spark 

Raison d'être: manque de flexibilité de MapReduce pour la gestion de plusieurs actions d'affilée. 
Développé à partir de 2009 par l'université de Berkeley (USA), projet Apache en 2010, mis en avant dès 2013 par Apache. 

## Concepts et architecture 

Le cluster Spark est la partie du cluster qui fait tourner Spark. 
Il a besoin d'un gestionnaire de ressources en général (disons YARN), dont le modèle est:
* un _master_ qui connait les machines du cluster et peut négocier des ressources lorsqu'un job est soumis 
* des _workers_ qui utilisent leurs ressources pour traiter un job ( _workers do the work, master supervises it_ )

Spark est un moteur de calcul distribué qui va exécuter des jobs. 
* Ces jobs sont distribués et parallélisés au maximum. Pour chaque job, un _driver_ le supervise.
* La donnée manipulée peut ne pas tenir sur un worker. Elle est partitionnée et distribuée sur des workers. Du point de vue du programme, on manipule un _RDD_ unique (et pas les parties distribuées)

### Distribution des calculs 

Quand on soumet un job spark, le main est exécuté par le _driver_. 
Il n'a pas forcément les ressources ou la capacité d'exécuter tout le job. 
Aussi, il va: 
1. Lire le programme principal et le découper en _stages_ (on dira étape). Un stage est un traitement coté code en première approximation
2. pour le stage en cours, découper l'exécution en des taches tournant en parallèle. 
3. négocier avec le ressource manager des workers pouvant exécuter ces taches
4. envoyer à chaque worker sélectionné une demande de création d'un processus Spark pour traiter une tache. Le processus créé exécute cette partie, d'où son nom d'_executor_.
5. superviser ces taches et lancer les suivantes


ATTENTION: 
* le spark context est la responsabilité du driver 
* une tache est exécutée par un et un seul executor
 

En fait, le _driver_ coordonne les _executors_. 
Plusieurs executors peuvent être portés par le même worker.
En fonction de la demande (affichage des N premières lignes d'un calcul par exemple), le driver peut collecter les données des executors pour les merger et les servir à l'utilisateur.
Le _spark context_ définit les propriétés du job spark. 

```
conf = SparkConf().setAppName(appName).setMaster(master)
sc = SparkContext(conf=conf)
```

A ne pas confondre avec la `SparkSession` qui est une session pour faire du spark SQL: 

```
from pyspark.sql import SparkSession

spark = SparkSession.builder.appName("test") \
    .config("spark.some.config.option", "some-value") \
    .getOrCreate()
	
df = spark.read.json("data.json")
df.show()
```

#### Application: configurer le lancement d'un job spark 

Pour les langages JVM (Scala, Java), on peut lancer un job programmatiquement. 
Sinon, pour tous les langages, la ligne de commande permet de lancer un job. 
Il faut pour cela définir: 
* la _class_ qui définit où est le main (le nom préfixé de la classe qui porte le main)
* le _master_ qui définit l'uri du driver: local (test), local_cluster (pour simuler un cluster en local), l'uri du client à k8s, ou yarn (auquel cas une variable d'environnement est nécessaire pour le localiser) 
* le _deployment-mode_ qui précise où est le driver: en local (_client_, qui est le mode par défaut) ou sur un worker (_cluster_). En général, le worker est plus puissant que le PC du développeur, donc... 
* une éventuelle _conf_ sous forme de clé valeur, avec un couple par `--conf`
* les paramètres spécifiques aux executors du cluster, par exemple `--executor-memory 20G`, `--num-executors 50` ou `--total-executor-cores 100` 
* le code ne tient pas sur un seul fichier. Que ce soit un fat jar en Java ou Scala, un fichier egg pour Python, il doit être présent (on le dépose sur hdfs ou sur chaque machine). Et on le met en paramètre 

On peut utiliser un fichier de configuration (par défaut, dans le répertoire spark à `conf/spark-defaults.conf`). 


### Distribution de la donnée 

Spark se base sur sa partie Core, qui définit les RDD. 
Les autres parties (MLLib, SparkSQL, GraphX, etc) se basent sur cette couche. 
Un RDD est de la donnée partitionnée et répartie. 
Plus précisément, un RDD est la donnée de: 
1. Une dépendance avec les RDD parents, afin de le reconstituer en cas de plantage 
2. Un ensemble de partitions dont la réunion forme l'ensemble de la donnée du RDD. Le découpage est un facteur clé de la performance de traitement 
3. La fonction qui permet de calculer la sortie de la prochaine étape 
4. (Optionnel) De la métadonnée sur le schéma de partitionnement 
5. (Optionnel) Où est la donnée sur le cluster 



Il y a deux types d'opérations sur les RDD: 
1. Les _transformations_ (filter, map, join) qui transforment les RDD en RDD 
2. Les _actions_ (reduce, take et first, count, saveAsTextFile) qui renvoient une valeur au driver 

#### Données réparties, variables partagées

Voici un exemple de piège si on se méprend:

```
# spark context is sc 
counter = 0
rdd = sc.parallelize(data)

# Wrong: Don't do this!!
def global_count(x):
    global counter
    counter += x
rdd.foreach(increment_counter)
```

Le problème est que plusieurs JVM vont exécuter le code, et counter ne sera que la version locale de la variable. 
La solution est d'utiliser des variables partagées: chacune calcule la valeur de sa partie des données. 
Les données sont ensuite rassemblées et agrégées dans un résultat final pertinent. 
C'est le travail d'un accumulateur. 

```
accum = sc.accumulator(0)
sc.parallelize([1, 2, 3, 4]).foreach(lambda x: accum.add(x))
print(accum.value)
```


Si une donnée est utilisée très souvent, on peut utiliser une variable _broadcast_. 
Le principe est de la donner à tous les workers. 
On s'en sert plus pour des variables de petite taille. 


On peut aussi mettre un RDD en cache. 
Par défaut, il est recalculé. 


#### Impact de la répartition sur les performances

La donnée des RDD est répartie sur les workers par partition. 
Les RDD sont soit le premier (partition dépendant du ressource manager), soit un RDD ayant un parent.
Sur la plupart des opérations, une tache traite une partition sur son workder. 
Mais...
Certaines opérations impliquent de parcourir toutes les partitions et les réorganiser toutes.
C'est en particulier le cas quand on travaille par `clé -> valeur` et qu'on change la clé. 

```
lines = sc.textFile("data.txt")
# lines is default partitioned 
pairs = lines.map(lambda s: (s, 1))
counts = pairs.reduceByKey(lambda a, b: a + b)
# data repartitioning: key is line value, value is number of appearences
```


On parle alors de _data shuffle_, et l'opération est hyper couteuse: 
* car elle va utiliser la mémoire des sources et les mémoires de travail (impact mémoire)
* quand les mémoires sont pleines, spark écrit sur disque  (impact IO disque)
* la donnée est forcément repartitionnée, donc échangée de worker en worker (impact réseau)
* le GC va tourner pour nettoyer la donnée temporaire, ce qui aussi a un impact. On peut changer la configuration des jobs pour changer quand le GC passe

  
Elle est traitée par un map reduce sur la donnée (au sens du paradigme). 
Les opérations qui provoquent un shuffle sont: 
* `repartition(int)`, `coalesce(n)` (qui réduit à n partitions) et `repartitionAndSortWithinPartitions(partitioner)` (qui trie par clé dans chaque partition)
* les `join` (et les `cogroup` (un group by même clé qui groupe deux rdd)
* toutes les opérations `...ByKey` (groupByKey, reduceByKey)

## Configuer les jobs Spark 

La configuration de Spark se fait à au moins trois endroits: 
1. (yarn en général) `yarn-site.xml` à vérifier si YARN est le ressource manager de Spark pour voir les allocations par défaut 
2. (spark) les propriétés générales dans le répertoire d'installation de Spark 
3. (spark job) les propriétés du job en dernier lieu 

Voici une partie de la configuration Spark par défaut: 

```
spark.master                     yarn
spark.submit.deployMode          client
spark.driver.memory              512m
spark.executor.memory            512m
spark.yarn.am.memory             1G
spark.eventLog.enabled           true
spark.eventLog.dir               hdfs://spark-yarn-master:8080/spark-logs
spark.history.provider           org.apache.spark.deploy.history.FsHistoryProvider
spark.history.fs.logDirectory    hdfs://spark-yarn-master:8080/spark-logs
spark.history.fs.update.interval 10s
spark.history.ui.port            18080
```


On peut voir [les propriétés](https://spark.apache.org/docs/3.5.1/configuration.html#spark-properties) du job sur son web ui à ` http://<driver>:4040` dans l'onglet _Environment_.  

### Les ressources matérielles

Spark détaille ces informations dans sa [documentation](https://spark.apache.org/docs/latest/hardware-provisioning.html). 

1. Au moins 8 Gb de mémoire, pas plus de 75% de la mémoire de l'hôte pour en laisser à l'OS
2. Maximum 200 Gb (au delà la JVM ne réagit pas très bien)
3. Idéalement 4 à 8 disques par noeud, pas de RAID
4. Idéalement des cartes réseau à 10Gb/s 
5. 8 à 16 cores par machine 

### La gestion des exécuteurs (le scheduling)

Quand plusieurs jobs sont lancés, l'allocation des ressources dépend du ressource manager par définition. 
On va se concentrer sur YARN. 
Quand un job est soumis, la méthode de gestion par défaut est d'allouer toutes les ressources possibles tout le temps du job. 
On peut régler les paramètres avec: `--num-executors` d'une part et `--executor-memory` , `--executor-cores` d'autre part

Après, évidemment, on peut définir la queue YARN dans laquelle le job va tourner, donc les propriétés YARN attachées. 
Par exemple: 

```
spark-submit \
  --class org.apache.spark.examples.SparkPi \
  --queue <queue_name> \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 1 \
  --driver-memory 512m \
  --executor-memory 512m \
  --executor-cores 1 \
  /usr/hdp/current/spark2-client/examples/jars/spark-examples_*.jar 10
```

Enfin, on peut définir des stratégies au niveau des stages. 

### Les propriétés de la JVM 

De manière générale, les propriétés java sont définies avec:
* spark.driver.extraJavaOptions 
* spark.executor.extraJavaOptions

On les rajoute au moment de la création de la conf spark. 

#### Statistiques sur le GC 
On aimerait voir comment le GC se comporte et s'il pénalise les jobs.
Il faut donc ajouter en options Java: `-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps`

#### Le GC recommandé

On prendra `-XX:+UseG1GC`

### Les sérialisations

La sérialisation permet à Spark de déplacer des objets d'un executor à l'autre. 
La valeur par défaut est le sérialiseur Java standard. 
D'autres alternatives sont bien plus rapides, telles que le KryoSerializer. 

`sc.setSystemProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")`

### Les propriétés des jobs 

Il en existe des dizaines dont le détail est [ici](https://spark.apache.org/docs/latest/configuration.html). 
On peut quasiment tout configurer. 

## Monitoring des jobs sparks 

### Les informations sur l'UI

Quand un spark context tourne sur une machine, il lance aussi une web ui, accessible sur le port 4040. 
Elle donne donc des informations sur le job en cours. 
L'autre cas est de comprendre après coup pourquoi un job est long. 
Garder ces informations après coup est la responsabilité du _spark history server_. 
Mais pour cela, il faut avoir activé certaines propriétés pour voir les logs et avoir défini où les stocker. 
Voir à ce sujet la page principale [ici](https://spark.apache.org/docs/latest/monitoring.html). 

### Les logs YARN 

On se place dans le cas d'un ressource manager qui est YARN.
Par définition, les applications sont accessibles à YARN, donc un 
`yarn logs -applicationId <app ID>` va donner des informations. 


## SPARK SQL 

Le driver permet de créer une `SparkSession`. 
Spark a fait un effort volontaire d'isoler ses executors. 
En conséquence, deux applications spark doivent échanger des données par une source externe (SGBD, HDFS, etc). 
Spark SQL est construit au dessus de Spark Core (et donc des RDD). 
Il offre l'usage du SQL sur des _DataFrame_, de la donnée distribuée et organisée en colonnes. 
En première approximation, on peut dire qu'un Dataframe est l'équivalent spark d'une table pour un SGBDR. 
Le SQL est optimisé via le Spark SQL Catalyst optimizer. 
Le développeur écrit peu de code, et le meilleur plan est trouvé par l'optimisateur. 
Pour y parvenir, il stocke et utilise des statistiques sur les données et ainsi trouver le meilleur algorithme de join, par exemple, ou déterminer le meilleur nombre de partitions. 
Par exemple, Spark SQL utilise le _dynamic partition pruning_ pour éviter de charger une partition de donnée non pertinente pour une requête. 

### Les Dataframes 

| Nom | Langage | Description |
|-----------------|------------|-----------|
| `Dataset<T>` | Scala / Java | abstraction de RDD typé |
| `DataFrame` | Python, Java, Scala | Dataset de Row |

Un dataframe est une collection immutable de données, organisée en rows, et chaque row est divisée en colonnes: 
```
Dataframe ---> Rows ---> Cols 
(schema)
```

Comme les RDD, l'api propose des transformations (évaluées en mode lazy) et des actions (évaluées par avance, donc eagerly). 

#### Principales transformations 

| Nom |  But |
|-------------|---------------|
| select |  select de colonnes |
| selectExpr  | select par expression SQL |
| filter / where  | filtre de colonnes |
| distinct  | enlève la donnée dupliquée |
| sort  | Tri par colonne(s) |
| limit  | Prendre les N premières lignes |
| union | Union de dataframes |
| withColumn | Ajoute une colonne |
| withColumnRenamed | Renomme une colonne |
| Drop | enlève une colonne |
| sample | sélection aléatoire de lignes |
| randomSplit | Par exemple test vs training set |
| join | Plusieurs types possibles |
| groupBy | group by colonnes |

Pour récupérer une colonne spécifique, on utilise _col_. 

#### Principales actions 

| Nom |  But |
|-------------|---------------|
| show | affiche les N premiers résultats |
| head / take / first | Renvoie les N premières lignes |
| takeAsList |  Prend les N premières lignes en liste |
| collect / collectAsList | Convertit le RDD en liste | 
| count | Nombre de lignes |
| describe | principales caractéristiques statistiques |



# Sources 

* Site officiel
* Beginning Apache Spark 3 : with dataframes, spark SQL, Structured Streamings and Spark ML Library. Luu, 2021