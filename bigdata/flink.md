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

## Sources

* Stream processing with Apache Flink (Huesk, Kalavri)