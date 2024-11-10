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
* un executor exécute une tache et une seule, mais une tache active est exécutée par au moins un exécutor 
 

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

La pratique du Spark SQL montre qu'un traitement peut prendre du temps (de quelques secondes à quelques heures). 
Il n'est pas pertinent dans les environnements ayant besoin d'une très faible latence (OLTP par exemple). 


Sur le fonctionnement, le driver permet de créer une `SparkSession`. 
Spark a fait un effort volontaire d'isoler ses executors. 
En conséquence, deux applications spark doivent échanger des données par une source externe (SGBD, HDFS, etc). 
Spark SQL est construit au dessus de Spark Core (et donc des RDD). 
Il offre l'usage du SQL sur des _DataFrame_ (python) et _Dataset_ (java, scala), de la donnée distribuée et organisée en colonnes. 
En première approximation, on peut dire qu'un Dataframe est l'équivalent spark d'une table pour un SGBDR. 
Le SQL est optimisé via le Spark SQL Catalyst optimizer. 
Le développeur écrit peu de code, et le meilleur plan est trouvé par l'optimisateur. 
Pour y parvenir, il stocke et utilise des statistiques sur les données et ainsi trouver le meilleur algorithme de join, par exemple, ou déterminer le meilleur nombre de partitions. 
Par exemple, Spark SQL utilise le _dynamic partition pruning_ pour éviter de charger une partition de donnée non pertinente pour une requête. 

### Les Dataframes 

| Nom | Langage | Description |
|-----------------|------------|-----------|
| `Dataset<T>` | Scala / Java | abstraction de RDD typé |
| `DataFrame` | Python | Dataset de Row |

Un dataframe est une collection immutable de données, organisée en rows, et chaque row est divisée en colonnes: 
```
Dataframe ---> Rows ---> Cols 
(schema)
```

Spécifiquement sur Java et Scala, Dataframe n'est plus proposé sur Spark 3.5.1 et on manipule des _Dataset<Row>_.


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

### Passer des RDD aux Dataframes, aller et retour

#### En Java 
En Java, on manipule des datasets, et en python des dataframes. 
Les RDD en Java sont appelés _JavaRDD_ et sont typés, par exemple _JavaRDD<String>_. 
Par contre, la classe par défaut des Dataset est _Row_. 
Une fois le Dataset correctement typé, disons `Dataset<Person>`, passer à un `JavaRDD<Person>` est hyper simple: `df.toJavaRDD()`. 
Dans l'autre sens, c'est un peu moins intuitif. 
Le passage est réalisé ainsi : 

```
// first, make the rdd 
SparkConf conf = new SparkConf().setMaster("local[1]").setAppName("test RDD");
JavaSparkContext sc = new JavaSparkContext(conf);
JavaRDD<YearSalaryTuple> content = sc
	.textFile(path)
	.filter(v -> v != null && v.length() >= 10 && !v.equals("DATE,PID,AMOUNT"))
	.map(new Function<String,YearSalaryTuple>() {
		@Override
		public YearSalaryTuple call(String v) throws Exception {
			String[] values = v.split(",");
			int year = Integer.parseInt(values[0].substring(0, 4));
			float salary = Float.parseFloat(values[2] + ".0");
			YearSalaryTuple result = new YearSalaryTuple();
			result.setYear(year);
			result.setSalary(salary);
			return result;
		}
	});

// no show from rdd, use take and print
content.take(10).forEach(System.out::println);

// alright, then, map it to a dataset with a given schema 
SparkSession session = Sessions.getSession();
// CAUTION: it builds a row based dataset, not a dataset of yearsalarytuple 
Dataset<Row> rows = session.createDataFrame(content, YearSalaryTuple.class);
```

Il faudra un map supplémentaire pour passer au bon type: 

```
// to make it as a dataset of yearsalarytuple, use a map
Dataset<YearSalaryTuple> tuples = rows.map(new MapFunction<Row,YearSalaryTuple>() {

	@Override
	public YearSalaryTuple call(Row row) {
		final YearSalaryTuple result = new YearSalaryTuple();
		result.setSalary(row.getFloat(row.fieldIndex("salary")));
		result.setYear(row.getInt(row.fieldIndex("year")));
		return result;
	}
	
}, Encoders.bean(YearSalaryTuple.class));

tuples.show();
```

#### En python 

Pyspark va avoir la même logique. 

```
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

```

### Exécuter du code SQL 

Le principe est le suivant: 
1. Créer sa session 
2. Charger les dataframes 
3. Les enregistrer avec un nom de table, soit pour la session (par défaut), soit globalement (pour toutes les sessions, avec `pyspark.sql.DataFrame.createGlobalTempView`)
4. Faire du SQL sur ces données
5. Eventuellement persister le résultat 


Un catalogue permet de retrouver les dataframes enregistrées, leurs colonnes, etc. 
En fait, on peut voir le catalogue comme la méta-donnée sur les tables enregistrées. 

#### Agrégations

Parmi les agrégations possibles, on peut signaler la présence de fonctions utiles: 
* countDistinct, sumDistinct
* collect_list, collect_set
* groupBy (qui peut avoir plusieurs arguments)
* agg qui permet de prendre plusieurs agrégations pour le même groupBy

Par exemple: 

```
from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import avg,stddev,skewness,kurtosis


conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")


with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv")
    stats = base.select("ROLE", "AMOUNT").groupBy("ROLE").agg(\
        avg("AMOUNT"),\
        stddev("AMOUNT"),\
        skewness("AMOUNT"),\
        kurtosis("AMOUNT")\
    )

    stats.show()
```

#### Pivot 

Si une colonne C prend les valeurs _a,b,c_, on peut retourner la table en prenant ces valeurs comme des valeurs différentes. 
Par exemple: 

```
+----------+------+----+
|      ROLE|AMOUNT|YEAR|
+----------+------+----+
|       Dev| 50000|2020|
|       CEO| 70000|2020|
|       CTO| 65000|2020|
|Accountant| 60000|2020|
|        HR| 55000|2020|
|       Dev| 50000|2020|
```


Le pivot permet d'avoir les années comme des colonnes, et ainsi réaliser une agrégation par une autre colonne. 
Le résultat sera: 


```
+----------+-------+-------+-------+-------+-------+
|      ROLE|   2020|   2021|   2022|   2023|   2024|
+----------+-------+-------+-------+-------+-------+
|       CTO|65000.0|68000.0|71000.0|74000.0|77000.0|
|        HR|55000.0|58000.0|61000.0|64000.0|67000.0|
|       Dev|50000.0|53000.0|56000.0|59000.0|62000.0|
|       CEO|70000.0|73000.0|76000.0|79000.0|82000.0|
|Accountant|60000.0|63000.0|66000.0|69000.0|72000.0|
+----------+-------+-------+-------+-------+-------+
```


Qu'on obtient avec: 

```
from pyspark.sql.session import SparkSession
from pyspark import SparkConf
from pyspark.sql.functions import avg,stddev,skewness,kurtosis
from pyspark.sql.types import IntegerType,StringType 
from pyspark.sql.functions import udf 


conf = SparkConf().setMaster("local[1]").setAppName("test")
conf.set("spark.executor.heartbeatInterval","300s")
conf.set("spark.network.timeout", "600s")


with SparkSession.builder.config(conf = conf).getOrCreate() as spark:
    base = spark.read.option("inferSchema", True).option("header",True).csv("storage/base.csv")
    year_udf = udf(lambda v:v[0:4], StringType())
    salaries = base.withColumn("YEAR", year_udf(base["DATE"])).select("ROLE","AMOUNT","YEAR")
    salaries.show()
    agg_salaries = salaries.groupBy("ROLE").pivot("YEAR").avg("AMOUNT")
    agg_salaries.show()
```


Et dans sa version SQL:

```
agg_sql_salaries = spark.sql("select * from salaries pivot ( avg(AMOUNT) as avg for year in (2020,2021, 2022, 2023, 2024))")
```

#### Joins 

Si on prend du recul sur un join, on a basiquement: 
* une table à gauche, on va dire G 
* une table à droite, disons D 
* une condition de jointure, disons COND 

| Type de JOIN | Résultat | Nom spark |
|--------------|----------|------------|
| INNER | (a,b) quand COND |  inner |
| LEFT OUTER | (a,b) si COND sinon (a,null) | left_outer |
| RIGHT OUTER | (a,b) si COND sinon (null,b) | right_outer |
| FULL OUTER | (a,b) si COND ou (a,null) ou (null,b) | outer |
| LEFT ANTI | (a,null) si NON COND | left_anti |
| LEFT SEMI | (a,null) si COND | left_semi | 
| CROSS | tous les couples (a,b) possibles | on appelle df.crossJoin(...) |


Au niveau de l'implémentation Spark des joins, il y a deux algorithmes: 
* _Shuffle hash join_ (quand les deux tables sont grandes): on met dans la même partition les éléments ayant même hash. Donc il y a shuffle
* _Broadcast hash join_ (quand une table tient en mémoire): on hash les valeurs de la petite table et on l'envoie à la partition de la grande. Pas de shuffle, mais du broadcast

#### Fonctions 

Il existe plus de 200 fonctions définies dans l'API. 
Mais on peut définir les siennes, on parle d'user defined functions. 
Il en existe de deux sortes: 
1. Scalaires (UDF): elles transforment, pour une ligne donnée, une valeur en une autre. On peut prendre, __à ligne donnée__, des valeurs et réaliser un map local. Cas d'usage: désérialiser un tableau de string d'un CSV pour en faire une instance d'un type qu'on a défini
2. au niveau table (UDTF): elles transforment une table en table. On les retrouve aussi sous le nom de [UDAF](https://spark.apache.org/docs/latest/sql-ref-functions-udf-aggregate.html) (pour Aggregated). 
Dans ce cas, la session contient de quoi nommer les fonctions et les utiliser ensuite. 



#### group by, roll up, cube, et finalement les grouping sets  

Ce sont des opérations propres aux data-warehouses. 
Pour la théorie générale, voir [ici](https://github.com/zefrenchwan/calepin/blob/main/bi/dwh.md). 
Fondamentalement, une dimension peut avoir des sous dimensions: 
* pays, région, ville, etc 
* année, mois dans l'année, jour 
* gamme, sous gamme, produit


Le principe du _rollup_ est d'agréger de la donnée d'une dimension (ou plusieurs) pour avoir les valeurs globales.  
Dans le cas de Spark, imaginons qu'on ait ceci: 

| DATE | ROLE | AMOUNT |
|------|------|--------|
| 2020-01-01 | CEO | 10000 |
| 2020-01-01 | DEV | 4000 |
| 2020-01-01 | CTO | 70000 |
| 2020-02-01 | CEO | 10000 |
| 2020-02-01 | DEV | 4000 |
| 2020-02-01 | CTO | 70000 |


On veut comprendre les salaires par rôle et par an. 
Le principe est de couper la donnée par an (avec une udf), de réaliser les moyennes par an sur le salaire à rôle donné, etc. 
Voici le cadre général: 

```
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
```


Pour avoir la moyenne par an et par rôle, il suffit d'un group by. 

```
# amounts by ROLE and YEAR 
salaries.groupBy("ROLE","YEAR").avg("AMOUNT").orderBy("YEAR","ROLE").show()
  
```


Ce qui donne par exemple: 

```
+----------+----+-----------+
|      ROLE|YEAR|avg(AMOUNT)|
+----------+----+-----------+
|Accountant|2020|    60000.0|
|       CEO|2020|    70000.0|
|       CTO|2020|    65000.0|
|       Dev|2020|    50000.0|
|        HR|2020|    55000.0|
```



On a exactement ce qu'on demande, donc pas les moyennes tous rôles confondus et toute année confondue. 
Il faudrait alors deux requêtes pour les avoir et un union du résultat. 
L'usage de `cube` permet de répondre à ce besoin. 


Le rollup adresse ici le regroupement des dimensions par rapport à leurs sous dimensions. 
Imaginons qu'on a `YEAR --> DATE`, on veut réaliser un rollup sur la période. 
__ATTENTION: le rollup ne renvoie pas toute la table, mais les dimensions et les agrégations__.
Autrement dit, le rollup prend toutes les lignes, agrège par les dimensions qu'on lui a fixé, et dans le sens du rollup. 

```
salaries.rollup("YEAR","DATE").avg("AMOUNT").orderBy("YEAR","DATE").show()
```

Donnera: 

```
+----+----------+-----------+
|YEAR|      DATE|avg(AMOUNT)|
+----+----------+-----------+
|NULL|      NULL|    61000.0|
|2020|      NULL|    55000.0|
|2020|2020/01/01|    55000.0|
|2020|2020/02/01|    55000.0|
|2020|2020/03/01|    55000.0|
|2020|2020/04/01|    55000.0|
|2020|2020/05/01|    55000.0|
|2020|2020/06/01|    55000.0|
|2020|2020/07/01|    55000.0|
|2020|2020/08/01|    55000.0|
|2020|2020/09/01|    55000.0|
|2020|2020/10/01|    55000.0|
|2020|2020/11/01|    55000.0|
|2021|      NULL|    58000.0|
```


On voit d'abord les valeurs null, null, qui sont la moyenne globale, tout confondu. 
On voit aussi, ensuite, à YEAR donné, la valeur NULL pour la date qui signifie la moyenne sur un an. 
Et à la ligne suivante, la moyenne de toutes les lignes à YEAR et DATE donnés. 


En général, on peut réaliser le rollup de dimensions quelconques, disons A et B, pour une agrégation AGG. 
Ce que va présenter le rollup devient: 
1. null, null, AGG(toutes les lignes)
2. a, null, AGG(à a donné pour A, toutes les valeurs)
3. a,b, AGG(toutes les lignes à a et b donnés pour A et B)


La méthode `cube` le fait dans les deux sens: du point de vue de A et B. 
Ainsi, un cube sur les dimensions A et B va donner: 
1. null, null, AGG(toutes les lignes)
2. a, null, AGG (toutes les lignes ayant a comme valeur de A) 
3. null, b, AGG(toutes les lignes dont la valeur est b pour B)
4. a,b,AGG(toutes les lignes dont la valeur pour A est a et b pour B) 

Par exemple:

```
data.cube("DATE","ROLE","NAME").avg("AMOUNT").orderBy("DATE","ROLE","NAME").show()
```

Donnera: 

```
+----+----------+---------------+-----------+
|DATE|ROLE      |NAME           |avg(AMOUNT)|
+----+----------+---------------+-----------+
|NULL|NULL      |NULL           |61000.0    |
|NULL|NULL      |Claire Doe     |56000.0    |
...
|NULL|Accountant|NULL           |66000.0    |
|NULL|Accountant|Marie Smith    |66000.0    |
|NULL|CEO       |NULL           |76000.0    |
|NULL|CEO       |John Reese     |76000.0    |
|NULL|CTO       |NULL           |71000.0    |
```



Leur point commun est qu'ils sont des cas particuliers de la notion de [grouping set](https://stackoverflow.com/questions/37975227/what-is-the-difference-between-cube-rollup-and-groupby-operators).
Reprenons l'exemple. 
Certains regroupements notn pas trop de sens (YEAR, DATE par exemple). 
On aimerait faire la liste exhaustive des dimensions qu'on aimerait récupérer. 
Et bien, c'est possible avec les grouping sets: 

```
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
```

Ce qui va donner: 

```
+----+----------+----------+-------+
|YEAR|      DATE|      ROLE|    avg|
+----+----------+----------+-------+
|NULL|      NULL|      NULL|61000.0|
|NULL|      NULL|Accountant|66000.0|
|NULL|      NULL|       CEO|76000.0|
|NULL|      NULL|       CTO|71000.0|
|NULL|      NULL|       Dev|56000.0|
|NULL|      NULL|        HR|61000.0|
|NULL|2020/01/01|      NULL|55000.0|
|NULL|2020/01/01|Accountant|60000.0|
|NULL|2020/01/01|       CEO|70000.0|
|NULL|2020/01/01|       CTO|65000.0|
|NULL|2020/01/01|       Dev|50000.0|
|NULL|2020/01/01|        HR|55000.0|
|NULL|2020/02/01|      NULL|55000.0|
|NULL|2020/02/01|Accountant|60000.0|
|NULL|2020/02/01|       CEO|70000.0|
|NULL|2020/02/01|       CTO|65000.0|
|NULL|2020/02/01|       Dev|50000.0|
|NULL|2020/02/01|        HR|55000.0|
|NULL|2020/03/01|      NULL|55000.0|
|NULL|2020/03/01|Accountant|60000.0|
+----+----------+----------+-------+
```

### Les window functions 


### Voir comment Spark SQL optimise les jobs 

Que ce soit du code SQL ou de la manipulation de dataframes, Spark SQL gère toujours de la même manière:
1. Il fabrique un plan d'exécution logique (logical plan) à partir du code. Il l'optimise par règles (filter avant join) ou calculs de coût (optimisation par taille, stats sur les colonnes, etc). 
2. A partir du plan logique, il fabrique un plan physique qu'il optimise suivant la même logique 
3. A partir de ce plan physique, il génère le bytecode qu'il va faire exécuter aux workers 


Si cet algorithme est la responsabilité de Catalyst, le projet Tungstene apporte une gestion fine des ressources matérielles (mémoire, CPU essentiellement, le réseau étant de moins en moins le facteur bloquant). 


Etant donné un dataframe df, on peut voir le plan d'exécution pour le fabriquer: 

```
df.explain(true) # both logical and physical plan 
```

On peut encore [affiner avec des valeurs différentes](https://spark.apache.org/docs/latest/api/python/reference/pyspark.sql/api/pyspark.sql.DataFrame.explain.html) pour voir uniquement le plan logique, tout, etc. 
 

# Sources 

* Site officiel
* Beginning Apache Spark 3 : with dataframes, spark SQL, Structured Streamings and Spark ML Library. Luu, 2021