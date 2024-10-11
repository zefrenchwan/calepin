# Hadoop 

Notes sur le système distribué d'Apache

## Structure et installation 

Le but est de faire des calculs sur des fichiers répartis et redondés au sein d'un FS dit HDFS.
Les fichiers sont découpés en blocs pour la redondance. 
YARN gère les ressources de calcul du cluster. 

### Les principales composantes

Deux machines _maitres_ : 
* Namenode (HDFS): la machine connait la répartition des blocs sur le cluster
* Resource Manager (YARN): le _master daemon_ de YARN qui va affecter les demandes de calcul aux noeuds pouvant la traiter. Quand une application se lance, le resource manager créé un premier conteneur pour traiter la demande, c'est l' _application master_ , et il va négocier avec le resource manager les noeuds (donc les node managers) qui vont exécuter l'application. A noter aussi le processus de WebAppProxy (YARN, sur le RM sauf si explicitement séparé): Un application manager maintient une webapp pour l'avancement et les détails. Il envoie au resource manager l'url de base. Le webapproxy gère cette app et sa sécurité. 


Un _secondary data node_ impacte tous les changements dans le HDFS (les edit logs) et met à jour l'image du FS (fsimage). 
Ce processus n'est pas instantané mais périodique, ce qui revient à des checkpoints, d'où son autre nom de _checkpoint node_. 

Le reste du cluster en mode *workers* : 
* Data node (HDFS): gère des blocs de donnée 
* Node manager (YARN): exécute une partie des calculs

Si MapReduce est activé, un serveur spécial gère l'historique des jobs MR terminés, le MapReduce Job History Server. 

### Soulager le namenode 

Le name node seul connait la structure du FS: pas de namenode, pas de cluster.
Il est donc un SPOF, mais ce problème est adressé par la haute disponibilité ( _high availability_ ). 
La structure devient:
* un name node principal, actif ( _active name node_ ) qui applique les edit logs 
* des name node de secours ( _stand by name node_ ) qui prendront le relais si le premier s'effondre. On peut en avoir un ou plusieurs. Tous appliquent aussi les edit logs qu'ils lisent d'un répertoire partagé avec le name node actif
* un _Quorum Journal Manager (QJM)_ qui va stocker au fur et à mesure les edit logs sur un quorum (au moins la moitié des noeuds) pour pouvoir relancer la construction du fsimage.

Derrière, on retrouve zookeeper : 
* qui stocke le statut chaque name node 
* qui permet l'élection d'un name node de secours en tant que principal le cas échéant

On a donc un processus supplémentaire, le _ZKFailoverController process_ (ZKFC).

### Configuration de YARN 

Quand un job se lance sur le cluster, il a besoin de ressources de calcul. 
C'est précisément la responsabilité du _scheduler_. 
YARN va allouer ces ressources pour traiter les nouvelles demandes en fonction des capacités restantes:
* par FIFO: le premier job à venir prend tout ce dont il a besoin 
* par Fair Scheduling: répartition égale au fur et à mesure des capacités pour les jobs acceptés
* par Capacity scheduler: Des queues avec des capacités, réparties par les jobs dans la queue. On peut avoir des sous queues. 

### Les principaux fichiers

* `etc/hadoop/core-site.xml` : fichier explicitant où est le name node
* `etc/hadoop/hdfs-site.xml` : pour le name node comme les data nodes, où stocker les données. Spécifiquement pour le name node, les URL des data nodes.   
* `etc/hadoop/yarn-site.xml` : configuration détaillée du resource manager ou des node manager. En particulier pour le RM, l' _history server_ contient les jobs finis
* `etc/hadoop/mapred-site.xml` : configuration map reduce (notamment préciser que YARN gère ses ressources)


### Démarrage du cluster 

Les commandes se trouvent dans `$HADOOP_HOME/bin/hdfs`.
Le grand plan est le suivant:

1. Démarrer les démons YARN et HDFS 
2. Formatter le FS 
3. Démarrer le namenode puis les datanodes
4. Démarrer le resource manager, puis les node manager, puis le WebAppProxy Server


## L'écosystème Hadoop 

La base reste le stockage et le framework de gestion des ressources pour le calcul. 
De nombreux outils se sont positionnés autour, pour adresser des problématiques de calcul, sécurité, visualisation, etc. 
Les présentations des outils sont extrêmement rapides et visent à servir de boussole plus que de guide exhaustif. 

### Sécuriser le cluster 

Voici les grandes lignes de Kerberos: 
* le _principal_ est un utilisateur ou un service identifiés. On l'est en s'identifiant auprès d'un KDC avec un fichier keytab (information sensible)
* le _realm_ est un groupe de ressources qui forment un tout cohérent capable de travailler ensemble 
* Le client s'authentifie auprès de Kerberos qui lui répond un ticket pour accéder à des ressources

Kerberos s'interface avec Hadoop. 
On s'identifie et on demande un ticket. 
Ce ticket nous permet d'accéder aux ressources (typiquement les noeuds d'un groupe). 

### Enchainer les jobs 


#### Apache Airflow 

Airflow est un gestionnaire de taches en général. 
Il marche par DAG (graphe orienté acyclique) codé en Python: 
1. on construit un DAG avec ses paramètres 
2. on ajoute des taches (lance un opérateur bash, lance un opérateur truc)
3. On lit les taches dans le DAG, par exemple `t1 >> [t2, t3]` qui est que quand t1 est finie en succès, on lance t2 et t3 en parallèle. De même `t2 << t1` signifie que t2 se lance quand t1 est finie en succès.

Le principe est d'ajouter des _providers_ qui acceptent des déclenchements de taches. 
On peut alors utiliser les principales briques de l'écosystème Hadoop: 
* Apache HDFS pour détecter par exemple qu'un fichier est dans un répertoire 
* Apache Spark pour lancer des traitements
* Apache Hive pour des requêtes 

Pour exécuter Airflow sur un cluster, on peut le faire gérer par YARN.

#### Apache Oozie

Apache Oozie est un gestionnaire de taches spécifique à Hadoop. 
Sa configuration s'ajoute au fichier `core-site.xml`.  
Il permet de lancer des taches (nommées actions) de type Spark, Hive, Shell, Sqoop, ssh, etc. 
Le principe est d'écrire un fichier XML, avec des noeuds nommés `<action name=${node name}>`
On gère ensuite avec des `<ok to="${node name}">` ou `<error to="${node name}">`. 

### Interface pour les opérations 

#### Apache Ambari 

Outil qu'on utilise comme un site web qui: 
* donne des informations générales sur le cluster (disk usage, noeuds, etc)
* permet de réaliser des opérations via une interface sans avoir à taper les commandes 
* permet de voir les logs des noeuds 


### Stocker de la donnée sous forme de fichiers 

Le principe général est de stocker la donnée en fichiers sur HDFS et d'exécuter des requêtes via des outils qui lisent ces fichiers.

Des formats de fichier sont plus avantageux en fonction des cas d'usage: 
* Le format _Parquet_ est compressé (moins d'espace), organisé en colonnes (plus intéressant que par lignes pour les requêtes analytiques).
* Le format _ORC_ suit la même logique et a été développé indépendamment de Parquet à peu près au même moment 
* Le format _RCFile_ utilise une méthode de stockage optimisée pour les tables en ligne puis colonne. 
* Le format _Avro_ est un format de fichier. On décrit ce qu'on veut stocker dans ces fichiers, et on peut générer de quoi lire et écrire de la donnée dans un langage (C, C#, Golang) à partir de cette description


### Requêter les données du cluster

#### Apache Druid 

Le principe est de mettre Druid au dessus d'HDFS pour ingérer de la donnée et exécuter des requêtes. 
Pour l'ingestion, il accepte du batch (des fichiers Hadoop) ou du streaming (depuis Kafka). 
Il créé des gros fichiers de données appelés segments. 
Ces données sont visibles comme des datasources. 
Pour l'exécution de requêtes, il accepte un format natif (requête HTTP en JSON) ou en Druid SQL (très proche du SQL). 
Les requêtes sont un `select dimension, op(metrics) from datasource where ... group by ...`.

#### Apache Hive

Utiliser Hive consiste:
* A définir des tables externes qui pointent vers des fichiers contenant des données de même type. `create external table ... stored as ... location`
* A lancer des requêtes en HiveQL (très proche du SQL) `select ... from la table externe where ... group by ...` 

Hive traduit les requêtes en un graphe acyclique de jobs, ceux ci pouvant être soit du map reduce, soit du Spark, soit du Tez. 
Ces jobs sont ensuite exécutés sur le cluster et regroupés en le résultat de la requête.

#### Apache HBase 

Base de données colonne, c'est à dire qu'on met de la donnée sous la forme: `clé --> colonne:valeur`. 
Si on définit une table, les données sont regroupées en column families dont le nom est fixé. 
Mais on peut mettre dans une même column family les données qu'on veut. 
Les trois opérations sont: 
1. Put: mettre une clé dans une table, dans une column family et une colonne une certaine valeur: `put clé cf:colonne value`
2. Get: récupérer dans une table les valeurs associées à une clé donnée. 
3. Scan: entre deux clés possibles, récupérer toutes les valeurs (cf:col:value)


### Transformer la donnée 

#### Apache Spark

Le propos ici est un rapide survol. 
C'est un peu le couteau suisse de la manipulation des données. 
Il permet de lire des données de fichiers, de bases (jdbc). 
Ensuite, en code ou en SQL, on les transforme. 
Finalement, on peut écrire les données transformées. 
YARN gère les ressources dont il a besoin. 

#### Apache PIG 

Moins utilisé, mentionnons le. 
Sa dernière release date de 2017. 
Le principe est d'avoir un langage de script qui transforme la donnée. 

```
A = load 'passwd' using PigStorage(':');  -- load the passwd file 
B = foreach A generate $0 as id;  -- extract the user IDs 
store B into 'id.out';  -- write the results to a file name id.out
```