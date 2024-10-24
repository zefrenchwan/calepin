# Apache Flink

* Copyright thefrenchwan, 2024
* MIT License 
* Aucun conflit d'intérêt, je ne suis pas payé ou contacté par Flink ou Apache ou qui que ce soit

## Pourquoi Flink ? 

Reprenons les fondamentaux. 
Les problématiques de données d'entreprise sont notamment:
* _les traitements locaux_:  le _transactional processing_ (basiquement la gestion d'une base de données transactionnelle). Les applications de CRM, back end de passage d'ordres, web app remplissent cette source de donnée commune à partir d'événements déclenchant des calculs
* _la centralisation pour l'aide à la décision_, par exemple le _data analytics_. On copie la donnée source depuis les systèmes de gestion courante vers un data warehouse structuré différemment (orienté requêtes client). On parle d'ETL (_extract, transform, load_). Ce sont des requêtes pour calculer des indicateurs en fonction du temps, ou pour afficher de l'informtion spécifique donnant une représentation de l'activité de l'entreprise

Le _transactional processing_ a une architecture typique.
L'architecture de base reste une base de données centrale lue et écrite par plusieurs systèmes. 
La difficulté vient du fait que chaque système client va avoir besoin de telle ou telle modification de la base et va impacter les autres. 
Les microservices ont été pensés pour résoudre ce problème. 

### Flink dans une architecture event driven 

Sur la partie _analytics processing_, la donnée est préparée spéciquement pour les rapports qu'on va demander. 
En particulier, on applique le pattern ETL (_extract, transform, load_) pour lire les données sources venant des bases locales et agréger l'information côté entrepôt de données. 
Que ce soit des ordres financiers, des logs venant des machines, des actions sur le front du client, on doit gérer un flux continu d'événements des systèmes sources. 
Le système de transformation doit être distribué. 
Les transformations des flux sources ne sont pas toujours une simple transformation sans état. 
Il faut alors stocker et gérer des états intermédiaires, lire depuis d'autres sources donc joindre des données, en prenant en compte des défaillances possibles des machines du cluster de traitement.
C'est ce que propose Flink depuis 2014. 
Il s'inscrit dans la catégorie des _stateful stream processing applications_ (SSPA dans la suite). 
Pour y parvenir, il utilise un mécanisme de checkpoint qui permet de sauver régulièrement l'état courant dans un stockage distant et durable. 
On parle alors de _periodic checkpoint_. 
C'est une solution extrêmement pratique quand elle est couplée à du commit logs (ou events log), comme par exemple Kafka. 
En fait, s'il plante, Flink repart de son dernier checkpoint et relit de manière déterministe les offsets qu'il n'a pas réussi à traiter.  


Cette capacité en général des SSPA permet d'avoir une architecture dite _events driven_:
* Les sources émettent les messages, ils suivent un pipeline de transformation avec éventuellement plusieurs SSPA. 
* En utilisant à chaque étape un modèle producteur consommateur, le système peut gérer sa charge avec bien plus de souplesse. 
* En étant stateful, chaque SSPA n'a plus besoin d'utiliser des systèmes tiers de stockage et peut être beaucoup plus rapide et efficace. 
* En couplant events logs et checkpoints, un plantage est géré en reprenant depuis un checkpoint. Avec la sémantique exactly once de l'event logs, on garantit que le traitement ne duplique rien et ne perd rien. 


Ce problème se retrouve aussi sur des questions de dashboards analytics. 
Il y a des années, il était acceptable de réaliser une mise à jour de leurs données à une fréquence fixée et longue. 
Mais en utilisant des systèmes d'IA qui basent les décisions sur les dernières données, c'est devenu problématique. 
Là encore, l'idée est la même: une architecture basée sur des event logs et un SSPA règle le problème. 


### Flink comme solution pour gérer la duplication de la donnée 

Flink permet aussi de traiter de la donnée par batch. 
Dans un gros système, la donnée est souvent dupliquée. 
Par exemple, un site marchand va mettre en cache de l'information sur ses produits qu'il a par ailleurs dans sa base centrale.
Il va peut être également la mettre dans un moteur d'indexation.  
Ainsi, il faut rafraichir souvent et garantir que la donnée est à peu près à jour, avec un délai contrôlé. 
Cependant, ce mode batch n'est pas toujours la meilleure solution. 
Dans une architecture event driven, on peut les mettre à jour dynamiquement, avec une latence bien moindre. 


### Au delà des lambda architectures 

Au début des années 2010, les solutions de traitement de flux (_stream processing_) privilégiaient la rapidité. 
Elles appliquaient alors une solution _at least once_ pour les messages, rendant la donnée approximative. 
Pour y remédier, les solutions architecturales proposées se basaient sur le concept de lamdba architecture: 
* une couche rapide utilise un moteur de streams (ou stream processor) et stocke dans une base les données fraiches. Du fait du "at least once", la donnée est approximative
* un batch traite de manière fiable l'historique et propose de la donnée juste dans un autre système de stockage 
* l'application cible va requêter les deux systèmes (_speed tables_ et _batch tables_) et réaliser un merge de la donnée 

En 2013, les moteurs de streams implémentent une gestion _exactly once_ des messages, mais la latence passe de millisecondes à quelques secondes. 
Ce problème est résolu dès 2015. 
Les moteurs de streams ajoutent également au fur et à mesure une intégration à YARN et à Kubernetes. 

### Que propose Flink ? 

Sur ce point en particulier, la source est "Stream processing with Apache Flink" (Huesk, Kalavri). 
Flink s'inscrit logiquement dans une architecture event driven et offre: 
* une faible latence
* avec Kafka, une gestion des messages en _exaclty once_
* les connecteurs pour les sources et sinks (JDBC, Kafka, etc)
* la possibilité de scale dynamique 
* la migration ou le redéploiement sans perte de l'état 
* Flink gère aussi les batchs 

## Les bases sur les moteurs de stream

C'est la partie théorique. 

### Dataflows graphs 

Les flux de données se théorisent comme des graphes acycliques orientés: 
* les noeuds initiaux sont les _sources_ de données 
* les noeuds finaux sont les destinations (_sinks_) de données 
* les noeuds intermédiaires sont les _opérateurs_ sur les données 
* les flêches sont les dépendances pour les opérations. Leur valeur est la donnée transportée

Par exemple: 
```
SOURCE      DONNEES         OPERATIONS              DONNEES       SINK 
[Kafka] === "axj54dsf" ==> [Avro deserilization] == "Bonjour" ==> [Base de données]
```


Ce diagramme logique est ensuite interprété par le moteur de stream qui va créer les taches idoines pour les traitemens: 
* les opérateurs donnent lieu à des taches. Pour traiter de gros volumes, plusieurs taches peuvent réaliser une opération (notion de _task parallelism_)
* chaque tache traite donc une sous partie des données de la source suivant un critère de partition (notion de _data partition_) 

Plusieurs stratégies existent pour l'échange de données entre les workers: 
* _forward strategy_: localité d'abord, le worker qui traite une tâche gère préférentiellement la suivante pour minimiser le déplacement de donnée 
* _broadcast strategy_: le worker duplique la donnée et l'envoie à tous les workers qui gèrent l'opération suivante. Evidemment très couteux
* _key based strategy_: la même clé est traitée par la même tache. 
* _random strategy_: répartition aléatoire de la donnée 


Comment mesurer qu'on a bien configuré notre outil ? 
Traiter un flux non borné d'événements (_unbounded data stream_, parfois juste _data stream_) est une autre façon de penser que par batchs. 
* au niveau des métriques, plus de notion de temps total de traitement, mais de _latence_ (le temps mis à traiter une donnée) et de capacité d'ingestion en volume (par unité de temps), dit _throughput_. Les deux sont liés: plus la latence est faible, meilleur est le _throughput_.
* les mesures de performance des sources sont l'_ingress_ (capacité à ingérer un volume par unité de temps) et pour les sinks, l'_egress_.

### Gestion des opérations 

Attention, en général, une opération prend en entrée un itérateur d'événements et renvoie un itérateur d'événements. 
Dans le cas particulier d'un entrant qui donne un sortant, on parle de transformation. 

Toutes ces opérations peuvent être: 
* _stateless_: sans état, elles gèrent événement par évenement. Elles sont facilement parallélisables et leur redémarrage est facile 
* _stateful_: les transformations dépendent des messages reçus précédemment, et potentiellement des suivants aussi. La gestion de leur état les rend plus difficile à paralléliser et à répartir. Par exemple, un calcul de somme, de moyenne ou toute agrégation en général nécessite un état. On parle alors de _rolling agregation_.


En général, comme on gère un flux potentiellement non borné, il faut le découper par blocs. 
Les transformations et les rolling agregations lisent une seule entrée et produisent une seule sortie, avec éventuellement la mise à jour d'un état. 
D'autres doivent gérer tout un buffer de données pour produire leur résultat. 
Par exemple, sur une tranche de 5 minutes, on peut vouloir avoir le nombre d'incidents sur tel type de données. 
Les _windows operations_ coupent ce flux de données en _buckets_ sur lesquels on réalise l'opération. 
On doit définir comment ce découpage est réalisé, et ce que doit faire l'opérateur avec son bucket. 
En interne, on parle de _trigger_, c'est à dire la condition qui définit que le bucket est complet et doit être passé en paramètre de la fonction d'évaluation.
Par exemple, on peut définir des buckets limités à N éléments, ou aux dix dernières secondes. 
On applique alors comme fonction d'évaluation un count, une somme, une moyenne, etc. 
Les règles de définition des fenêtres peuvent être: 
* _tumbling_: nombre fixe d'éléments qui sont dans un seul bucket. Ce nombre est fixé soit en taille (N éléments), soit en temps (1 minute)
* _sliding_: nombre fixe d'éléments, mais les buckets peuvent se chevaucher. Par exemple, une taille fixe de N éléments, avec les N/2 derniers éléments à chaque fois.
* _sessions_: on définit un TTL pour la session, et tous les éléments perçus dans la durée de vie de la session sont groupés en un bucket. Bien sûr, on aura une sorte d'ID de session avant de faire le regroupement ! Il y aura donc traitement en parallèle de données de plusieurs sessions

### Gestion du temps 

Imaginons un jeu mobile. 
L'utlisateur joue et la donnée arrive régulièrement. 
Il traverse subitement une zone sans réseau mais continue de jouer. 
A la sortie du tunnel, toute la donnée est envoyée d'un coup. 
Il y a donc deux temps: 
1. Le temps perçu par le moteur de stream, ou _processing time_: quand l'opérateur perçoit l'événement 
2. Le temps de création de l'événement à sa source, ou _event time_: quand l'événement est créé par le système source 

Comme rien ne garantit qu'on finisse par avoir toute la donnée qu'on attend au bout d'un temps fixé, il faut définir arbitrairement une limite de temps après laquelle on estime que c'est perdu. 
On parle de _watermark_: on attend un délai maximum. Si la donnée arrive avant, elle est lue. Après, elle est perdue. 
Le compromis est donc: 
* Pour la rapidité, on prend un petit watermark et on considère le processing time dans les windows
* Pour la précision, on prend un watermark plus long basé sur l'event time 


### Gestion des états 

Avant l'arrivée de moteurs de streams, on réalisait des batchs qui chargeaient des blocs de données. 
Par exemple, prendre l'activité de la dernière heure et calculer des statistiques ou indicateurs dessus. 
Cette méthode de batch a aussi comme inconvénient de ne pas réutiliser l'état de la précédente exécution. 
Quand on lance toutes les 10 minutes un calcul portant sur la dernière heure, c'est une perte d'efficacité. 


Les exemples d'opérations avec état sont nombreux: 
* savoir la température moyenne sur les dix dernières minutes d'un système 
* déterminer le temps qu'il faut pour avoir 1000 erreurs sur un système 
* toute opération d'agrégation qu'on découpe en temps ou en nombre 


Par contre, gérer un état pose question: 
* on ne peut pas attendre indéfiniment et laisser l'état trop grossir quitte à saturer la mémoire ou le stockage
* il faut trouver une façon de gérer les accès concurrents à un même état 
* il faut le distibuer. Une bonne façon est d'avoir une partition de l'état par clé d'événements 
* il faut gérer les pannes des taches, voire des workers et savoir repartir d'un état donné 

#### Les pannes 

En général, une tâche réalise son travail en trois étapes: 
1. lecture et stockage dans un buffer interne 
2. mise à jour éventuelle de l'état 
3. calcul et envoi de la sortie de l'opération 

Le problème principal devient ce qu'on appelle les _results guarantees_, c'est à dire maintenir la cohérence de l'état interne. 


Quand une tâche plante, il y a trois méthodes: 
1. _at most once_: la donnée source n'est pas relue, elle est perdue. On ne fait rien, on reprend quand on peut 
2. _at least once_: la donnée est relue si la tâche plante. Elle a déjà pu être traitée. On prend le risque de la traiter plusieurs fois. Par contre, ce n'est pas que le moteur de streams qui gère ce problème: relire la source depuis un offset fixé doit être possible, ce n'est pas toujours le cas ! 
3. _exactly once_: la donnée n'est pas perdue, et si elle est relue, l'état interne ne la prend pas en compte. On la traite donc bien une seule fois exactement 


Au niveau global, on définit un système qui garantit que la donnée venant de la source est traitée par le sink en mode _exactly once_. 
Ce n'est pas juste au niveau de la tâche, mais une propriété globale du système. 
On parle alors de _end to end exactly once_. 


## Prendre Flink en main 

Déjà, donnons un [exemple tronqué](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/learn-flink/overview/): 

```
final StreamExecutionEnvironment env =
		StreamExecutionEnvironment.getExecutionEnvironment();
// source
DataStream<String> lines = env.addSource(new FlinkKafkaConsumer<>(...));
// transformation sans état 
DataStream<Event> events = lines.map((line) -> parse(line));
// avec état 
DataStream<Statistics> stats = events
	.keyBy(event -> event.id)
	.timeWindow(Time.seconds(10))
	.apply(new MyCustomAggregation());
// sink 
stats.addSink(new MySink(...));
```

Très concrètement, on écrit un programme Flink en définissant un environnement d'exécution (par exemple, un `StreamExecutionEnvironment `). 
On ajoute une source à cet environnement, puis, depuis cette source, on ajoute transitivement les opérations et les puits. 
Ces [sources peuvent être](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/connectors/datastream/overview/) un fichier, un flux réseau, un Kafka, une base de données, un pub sub GCP, etc. 
On termine le code par un `env.execute()` qui va lancer le découpage en taches et leur exécution. 
Pour exécuter le programme Flink, on package un jar et on l'envoie à Flink: 

`./bin/flink run examples/streaming/WordCount.jar`

La version longue est [ici](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/overview/). 

### La partie ETL 

Citons d'emblée les grandes fonctions: 
* (Stateless) map pour une transformation, flatMap pour lire une valeur et renvoyer un ensemble, filter pour filter de la donnée (habile)
* (Stateless) keyBy qui envoie dans la même partition les données ayant la même clé (utile pour les agrégations ensuite). On manipule alors des [KeyedStream](https://nightlies.apache.org/flink/flink-docs-release-1.20/api/java/)
 
### Les problématiques d'exécution

Citons le cas du [backpressure](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/monitoring/back_pressure/). 
Fondamentalement, il s'agit d'un opérateur qui va beaucoup plus lentement que ses prédécesseurs. 
Les données s'accumulent, ce qui met le cluster en risque. 

## Architecture de Flink 


### Les principaux éléments 

Il se base sur: 
* Zookeeper pour la gestion de la haute disponibilité 
* un support de stockage durable (S3, HDFS) 
* Un gestionnaire de ressources (YARN, K8S) bien qu'il puisse aussi tourner sur une machine 



Flink est organisé en modèle _master - workers_:
* le master gère le dispatcher qui interagit avec le client quand un job est soumis, le ressource manager, et un JobManager par tache. 
* les workers portent un _TaskManager_. Ils ont des slots pré-réservés qui font tourner les taches. Entre deux workers, des streams échangent de la donnée sérialisée. 

| Nom | Hôte  | Rôle | 
|-------------|-------------|-------------|
| Dispatcher | Flink Master | serveur des jobs, UI |
| Ressource Manager | Flink Master | Lien avec le RM du cluster |
| Job Manager | Flink Master | Un par job, sheduler et coordonne les checkpoints |
| Task Manager | Flink Worker | Fait tourner des taches sur ses slots | 


A toute fin utile, Flink propose un [glossaire](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/concepts/glossary/). 


### Partitionnement des streams 

Quand Flink reçoit un job, il le découpe en sources, opérations et sinks. 
Le stream de donnée entre les opérations sera partitionné.
Une tache est un noeud du graphe physique représentant le moyen d'exécuter les opérations. 
Un sous-tache est une partie de la tache qui gère une partition indépendamment de ce que font les autres. 
Leur nombre est appelé _parallelism_ pour un opérateur donné, et il dépend de l'opérateur. 
Il existe deux types d'opérations: 
* les _one to one streams_ qui préservent l'ordre et restent sur la même partition 
* les _redistributing streams_ qui vont changer les partitions. Par exemple un `keyBy`. Attention, l'ordre des éléments n'est plus garanti au niveau de l'opération 

### Traitement des flux avec état (_stateful stream processing_)

Toute opération qui ne dépend pas exclusivement d'un élément mais de l'état des éléments autour est dite avec état (_stateful_). 
Par exemple: quand on réalise une agrégation sur une window donnée, l'état des valeurs en cours de traitement forme l'état de l'opération. 
La donnée est traitée localement, par partition. 
Le thread Flink est choisi par la clé la donnée. 
Pour garantir un traitement rapide, l'état est toujours lu localement, et il est aussi partitionné par clé. 
Il peut physiquement aussi bien résider en mémoire que sur disque. 


### Checkpoints et savepoints

#### Savepoints: l'utilisateur sauvegarde l'état pour une manipulation du job

Le [savepoint](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/ops/state/savepoints/) est une sorte de sauvegarde de l'état d'un job. 
On peut alors: 
* Réaliser un savepoint en cours d'exécution du job 
* Arrêter un job en persistant un savepoint
* Configurer ce savepoint, dans la configuration ou programmatiquement (`env.setDefaultSavepointDir("hdfs:///flink/savepoints");`)
* Relancer un job en partant de ce savepoint 


#### Checkpoints: pour que Flink gère seul ses plantages 

Flink stocke ses états et ses positions dans les différents streams sous forme de _checkpoint_.  
Quand une tache plante, il revient à son dernier checkpoint et recommence le traitement. 
Il a donc besoin régulièrement de stocker cette donnée de manière durable. 
Concrètement, c'est une propriété de l'environnement, désactivée par défaut.
Le détail de l'usage est [ici](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/fault-tolerance/checkpointing/)

## Sources

* Stream processing with Apache Flink (Huesk, Kalavri)