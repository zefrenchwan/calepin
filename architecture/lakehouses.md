* Copyright zefrenchwan, 2024
* MIT License
* Aucun partenariat, personne ne m'a contacté, je n'ai contacté personne, pas de publicité ni de rémunération 

L'explosion de l'IA telle qu'on la connait implique des volumes énormes de données. 
Les mettre à disposition implique des solutions spécifiques, dont les lakehouses. 

# L'architecture d'un lakehouse 

La donnée est une bonne réponse aux questions qu'on pose, et toute la subtilité est de poser les bonnes questions.
Pour cela, il faut explorer facilement les données dont on dispose pour avoir des pistes nouvelles. 
D'où l'importance de la définition de la bonne architecture de données. 

## Méthode et composants de base 

La méthode en général consiste: 
1. A trouver les composants de base pour l'ingestion, le stocjage, la transformation, la consommation de la donnée. Vont ensuite se poser les questions de sécurité, gestion de la donnée, la gouvernance et l'infrastructure 
2. A lier ces composants, en particulier les producteurs et les consommateurs de données. Vont alors se poser les questions et les contraintes d'ingestion. A noter qu'on reste à ce stade là à haut niveau 
3. Définir les principes architecuraux au sens large, dont la synchronisation entre les équipes et la définition des logiciels utilisés. Par exemple, si l'entreprise utilise plusieurs outils de BI, lequel choisir ? 
4. Trouver la meilleure solution technique, bloc à bloc. C'est la partie étude, avec la mise en place de POC au besoin 
5. Une fois la mise en place des blocs terminée, il s'agit de revenir à la vision globale et mettre en place les outils et bonnes pratiques de gouvernance. Donc la mise en place de soutions de sécurité, de présentation de la méta donnée, les catalogues de donnée, la conformité légale  

Les composants de base sont: 
* __les systèmes source__ : Les traitements se font par batch, streaming ou quasi temps réel. La donnée est structurée (tables, structures fixes), non structurée (image, son, texte), ou semi structurée (json, xml)
* __l'ingestion__ suivant la même classification: quasi temps réel (micro batch de quelques secondes ou minutes d'intervalle), streaming (flux permanent, le plus proche du temps réel), ou batch (volumes conséquents, traités à faible fréquence, par exemple tous les jours)
* __Le traitement et la transformation de la donnée__ : validation et nettoyage, transformation, curation (niveau maximum de qualité, préparer à l'emploi) et servir la donnée (_curation and serving_). 
* __le stockage__ dont les solutions sont soit à visée générale (fichiers, stockage dans un S3 ou sur HDFS) soit avec des outils dédiés spécifiquement à un usage précis (SGBDR, DW, solution NoSQL). Le stockage général est le moins cher, le stockage spécifique répond à des contraintes d'accès, de latence, de faciliter à retrouver l'information
* __la livraison pour la consommation de la donnée__ avec de la BI, de l'IA (ML notamment), mise en place d'API, outils d'analyse et d'exploration (une interface donc un stockage utilisant du SQL pour permettre d'explorer la donnée)
* __les services partagés__ qui sont la gestion de la méta donnée, la gouvernance et la sécurité, l'infrastructure (_data operations__). 

## Le concept du lakehouse 

En général, voici les fonctionnalités d'un data-lake et d'un data-warehouse: 
* __lake__: accessible et à faible cout, stockage durable et extensible (scalable) pour tous les types de données. Support des charges de traitement pour le ML et l'IA 
* __warehouse__: excellentes performances, support ACID, contrôles d'accès facilités et très granulaires, gestion du SQL et des charges de traitement de la BI 


La maturité de certaines solutions open source et les offres cloud ont permis des évolutions d'architecture.
Ce sont notamment: 
* mettre en place un lake (pour le ML et l'IA) et en même temps un DW (pour les besoins analytiques et la BI)
* la fusion des systèmes OLAP et OLTP dans une solution dite _hybrid transactional / analytical processing_ ou HTAP
* améliorer la lecture de tous les types de données pour les requêter avec un DW 
* mettre en place des formats propriétaires ou des stockages cloud dédiés pour avoir les mêmes performances qu'un DW 
* avoir des outils de BI et la puissance de calcul liée pour lire la donnée comme de la BI mais depuis un data lake 

Il est alors devenu possible de les combiner en un lakehouse, fournissant les avantages suivants: 
* un format unique pour traiter tous les types de données 
* le support ACID, une performance suffisante pour faire de la BI sur tous ces types de données 
* tout en gardant les avantages d'un data lake: scalabilité, maitrise des couts et flexibilité 

__On peut alors définir un lakehouse comme un data lake avec d'énormes capacités de calcul permettant la mise en place d'une surcouche avec des fonctionnalités de type ACID__ : 
* au niveau stockage: comme un datalake, des fichiers stockés (le plus souvent dans un cloud) et un format de stockage spécifique, dit _open table format_ rendant possible la partie lakehouse 
* au niveau du calcul: un _processing engine_ ajoutant la couche transactionnelle 

Il y a déjà eu des tentatives avant Databricks en 2021: Hive comme outil de requêtage SQL sur un stockage HDFS. 

## Comprendre l'architecture d'un lakehouse 

Ce qui fait la spécificité du lakehouse reste la mise en place, au dessus du stockage, d'une énorme puissance de calcul permettant d'amener des fonctionnalités de data-warehouse. 
A ce titre, les principales différences avec les autres architectures sont: 
* le stockage est le même pour la partie analytique et la partie ML/AI. Il n'y a pas de stockage séparé en fonction des cas d'usage 
* bien implémenté, un lakehouse fournit des performances comparables à un data-warehouse 
* __un lakehouse sépare le stockage du calcul__ bien que le lien soit le format open table. Ce qui permet des offres telles que Snowflake qui s'ajoute à un support cloud donné, ou bien de gérer et mettre à l'échelle l'un ou l'autre des composants 
* le format open table est lisible avec plusieurs solutions techniques, et le calcul peut utiliser telle ou telle solution

Ses bénéfices sont donc:
* la réduction du nombre de traitements: une fois la donnée transformée, elle est accessible par l'engine. Plus besoin de mettre en place un lien lake -> DW avec sa logique d'ingestion. Plus besoin donc de synchroniser la donnée d'un lake et celle d'un warehouse 
* avec l'usage d'un protocole, à savoir le __delta sharing protocol__, il existe un moyen de partager la donnée avec ses consommateurs. Celui ci est indépendant de la solution technique choisie. 
* la donnée entrante peut ne pas être structurée, mais un lakehouse permet une validation de la donnée produite et structurée. En particulier, on peut déjà tester la conformité de la donnée 
* la gestion du temps (dite _time travel_) consiste à dater la donnée, à la marquer comme supprimée avec la possibilité d'y revenir. On a en effet un numéro de version, ce qui permet de requêter `select * from ... as of version ...`

### synchronisation stockage et calcul 

#### Au niveau du stockage 

Les trois éléments de base du stockage du lakehouse sont: 
1. __Le stockage en soi__ : Ce n'est pas une obligation d'utiliser un fournisseur cloud, on peut le faire on-premise. Pour la facilité de gestion de la charge, le cloud reste intéressant. Les solutions commerciales de stockage sont GCS pour GCP, ADLS pour Microsoft Azure, et Amazon S3 pour AWS
2. __Les formats de stockage source__ : en général des formats ouverts. Pour le stockage en soi, on peut prendre du CSV, du JSON, du XML, etc. Mais pour le stockage à des fins analytiques, les trois formats principaux sont parquet, avro ou ORC
3. __Les formats de stockage spécifiques__ avec l'open table format

Les deux premiers sont partagés avec les lakes, mais seul le format spécifique open table créé la fonctionnalité de lakehouse. 
Les trois formats actuellement disponibles sont: 
* __Apache iceberg__ avec la gestion du temps, l'évolution des schémas, le support du SQL
* __Apache Hudi__ qui permet d'avoir des fonctionnalités ACID, l'évolution du schéma, la gestion du temps
* __Linux foundation delta lake__ initialement porté par Databricks. On peut alors mettre en place la version commerciale de databricks, ou la version communauté portée par la fondation linux 


#### Au niveau du calcul 

Le but est d'avoir un système de requêtage au dessus des fonctionnalités de stockage. 
Des solutions de traitement open source existent: Spark, [Presto](https://prestodb.io/), [Trino](https://trino.io/) et Hive. 
Il y a aussi des offres commerciales: Snowflake, Databricks, Dremio et Starbrust. 
 

# Les modern data platforms 

Basiquement, les architectures sont passées d'une approche unique (DW puis lake) à une approche combinant les deux solutions. 

## Data-warehouse ou data-lake
 
Historiquement, la première méthode d'amélioration de la décision au niveau de l'entrprise a été la mise en place d'un data-warehouse. 
La solution était de collecter la donnée de chaque source dans l'entreprise, de regrouper et nettoyer la donnée pour finalement l'intégrer dans un DW central pour la prise de décision stratégique. 
Les avantages ont été la facilité d'analyse sur de la donnée très structurée. 
Mais il a montré ses limite: 
* l'augmentation de volume a été à l'origine du concept de "big data" quand les limites de cette solution ont été atteintes
* pas de gestion de temps réel ou de temps court, aucune solution de streaming 
* la transformation de la donnée a été un problème pour les solutions d'IA et de ML en particulier 
* la solution liait stockage et calcul, et surtout était souvent propriétaire et couteuse 


Ce besoin croissant de ML et d'IA a provoqué un vif intérêt pour les solutions de stockage permettant de traiter tous les types de données. 
En particulier, Hadoop a été perçu comme une bonne solution pour traiter les limitations des DW. 
Les principaux apports sont: 
* la gestion de la donnée pour tout niveau de structuration 
* l'approche de schema on read, c'est à dire qu'on peut tout stocker, et on définit ce qu'on a besoin de lire après coup 
* le stockage dit _immutable storage_ au sens d'une donnée non modifiée 
* le matériel comme _commodity hardware_ au sens d'un outil beaucoup moins cher (un serveur de stockage) que la mise en place d'un serveur très cher dédié au DW. Le stockage disque de base est moins cher qu'un serveur énorme pour faire du DW

S'est alors mis en place la notion de data lakes avec une méthode: 
1. Intégrer les données directement des sources. En particulier, il devenait possible de mettre en place des batchs, bien sûr, mais aussi du quasi temps réel 
2. Traiter cette donnée pour les applications IA et ML 
3. Agréger et nettoyer cette information pour les usages de rapports et visualisation de la donnée 


Hive en particulier permettait de lire la donnée dans une logique de DW, bien qu'aucune solution pure DW ne soit apparue. 
Cependant, le constat était mitigé: 
* on pouvait mettre en place du ML et de l'IA
* Hive comme MapReduce, en plus depuis un stockage disque, créait une différence nette avec les solutions hyper optimisées que propose un DW 
* Pas de support pour l'ACID 

## Arrivée des modern data platforms 

Les solutions de DW traditionnelles et Hadoop sont toutes des solutions on-premise. 
Pousser la problématique dans le cloud a donné lieu aux modern data platforms, qui se définissent comme ces solutions (lake et dw) dans le cloud pour: 
* avoir la possibilité de mieux gérer la charge 
* ne plus avoir à gérer ses infrastructures (plus de DBA, plus d'administrateur Hadoop)
* la réduction des couts en payant uniquement à l'usage, l'optimisation de la performance en choisissant les caractéristiques de stockage et de calcul 
* une meilleure intégration des solutions nouvelles 
* accélérer la livraison en facilitant la mise en production

Fondamentalement, il y a deux approches pour la mise en place d'une modern data platform: 
* approche unique: _cloud data lake_ ou _cloud data warehouse_
* approche combinée, donc les deux. Le principe est d'intégrer la donnée dans le cloud et spécifiquement dans un data lake. Ensuite, toujours dans le cloud, on part du data lake et on met en place les bonnes transformations pour arriver au _cloud data warehouse_ 


Avec du recul, la proposition de valeur client est la suivante:  
* la gestion de tous les cas d'usage, y compris les nouveaux dont l'Iot, les images, les sons, les logs. On attend à pouvoir les intégrer et à réaliser des rapports et des analyses en les utilisant 
* la gestion de la charge, la flexibilité en changeant de format ou en ayant le bon outil technique, et la mise en place de solution ouvertes sans format propriétaire 
* des outils d'optimisation de la charge et des couts 
* approche orientée utilisateurs, avec la prise en compte des cas d'usage analytiques 
* la réduction des délais de mise en oeuvre pour faire face aux changements, y compris la possibilité de traiter des nouveaux cas d'usage encore inconnus à date. On parle d'_agile and future proof_ 

## Comparer un lake, un warehouse ou un lakehouse 

Pour comparer ce qui peut l'être, il faut prendre la version cloud de chaque solution. 
Petit rappel: ce document est un résumé des arguments fournis dans les sources, pas de prise de position de son auteur. 
Le fait que les lakehouses n'aient quasiment pas d'inconvénient pose en particulier question. 

### Avantages et inconvénients de chaque solution 

#### Le standalone cloud data warehouse 

Les avantages: 
* Cette solution a un domaine d'application spécifique: analytique et prise de décision. 
* L'architecture est simple conceptuellement et validée par des années d'expérience. 
* En particulier, les solutions (open source ou commerciales) sont très optimisées et le couplage fort stockage / calcul les rend très performantes. 
* Excellent outillage pour les ETL 
* Cette structuration forte de la donnée permet de bien contrôler la donnée et des traitements ACID 
* Possibilités d'optimisation avec les index, partitions, vues matérialisées 
* la donnée est plus simple à traiter, donc les cycles de développement sont plus courts 

Les inconvénients: 
* pas adapté aux cas ML et IA
* effort conséquent pour traiter de la donnée non structurée 
* du fait de solutions spécifiques, le partage n'est pas simple avec des clients et nécessite souvent une API d'accès 
* Support limité pour Spark et Python, mais ça s'améliore 
* la donnée est agrégée, il n'y a pas de solution native de gestion ligne à ligne. Certains fournisseurs offrent une gestion des versions ligne à ligne, mais pas tous
* les solutions propriétaires et le matériel entrainent des couts énormes 

#### Le standalone cloud data lake 

Les avantages:
* bonne gestion de la donnée non structurée, et donc parfaitement adapté aux cas ML et IA 
* gestion du temps réel comme du batch 
* partage de données avec les clients plus facile 
* Cas d'usage parfait pour Spark 
* Faible cout de stockage 

Les inconvénients: 
* gestion limitée mais possible des besoins BI 
* pas de support (ou très limité) du SQL 
* pas de possibilité de modifier la donnée (données immutables une fois stockées)
* pas de gestion ACID, pas de support de transactionnel 
* on ne peut suivre les versions que des fichiers, pas au ligne à ligne 
* Il n'y a pas de garantie de qualité de la donnée, la gouvernance est compliquée. Cela peut entrainer une baisse de confiance chez l'utilisateur du service 

#### Approche combinée en général 

Les avantages: 
* supporte tous les niveaux de structuration de la donnée 
* gestion efficace de la BI (via le DW) et des cas ML et IA (via le lake)
* une fois traitée, la donnée du DW est bien structurée 


Les inconvénients: 
* besoin de synchroniser en permanence le lake et le warehouse 
* la donnée la plus structurée dans le DW reste difficile à partager 
* complexité des flux de données 
* qualité de la donnée source variable, en particulier, le schema on read peut rendre la donnée dure à exploiter 
* la gestion de la sécurité, en particulier garantir la cohérence entre le lake et le warehouse 
* le cout inhérent à la présence des deux couches de stockage, et le cout de traitement 

#### Les lakehouses 

Les avantages: 
* architecture en deux couches facile à comprendre et utiliser 
* bien mis en oeuvre, a de bonnes performances coté BI 
* gestion unifiée du batch et du stream avec les bons outils techniques 
* gestion des schémas et de l'évolution des schémas 

Les inconvénients: 
* peu de recul sur la solution par rapport aux autres 



# Le stockage 

Le stockage d'un lakehouse doit fournir des fonctionnalités de recherche rapide de l'information, de gestion ACID de la donnée, et la gestion du temps pour les données. 

## Les trois niveaux dans la gestion des fichiers

Un facteur clé de la performance est de lire le minimum possible de données. 
Le premier choix important est de savoir si le stockage fichier va être centré ligne (_row storage_) ou colonne (_columnar storage_).
Dans le premier cas, on lit toute la ligne quand on y accède. 
Dans le second, on ne lit que son id et la colonne souhaitée. 
Ensuite, on peut utiliser des index, de la méta donnée (min et max de telle colonne), des critères de partition. 


Spécifiquement sur les lakehouses, il y a trois couches de stockage: 
1. Le stockage cloud. Il assure l'accès à la donnée: sa durabilité, sa disponibilité, sa sécurité, la montée en charge du stockage, la gestion du cout 
2. les données stockées dans des formats ouverts. Certains formats sont compressés, d'autres incluent une gestion des schémas. L'autre facteur clé du choix est la gestion de sa performance d'accès et la facilité à retrouver l'information. Certains formats (ORC notamment) ajoutent une gestion transactionnelle, des indexes, la gestion des structures de données (struct, listes, dictionnaires)
3. les données au format open table (en fait, une famille de formats) pour améliorer les performances et ajouter la gestion transactionnelle de la donnée. Hive a rapidement montré ses limites: lent, pas de modification ou suppression des fichiers, pas de support ACID. Trois solutions sont apparues: Apache Iceberg, Apache Hudi, Delta lake 

## Les formats open table 

Deux points d'attention: 
* __il s'agit bien de formats de stockage de fichiers, au niveau du stockage et pas du calcul__
* __un format de fichiers n'est pas un fichier avec un format, mais bien possiblement des fichiers portant différentes informations__

### Apache Iceberg 
Le produit est à l'origine né chez Netflix, adopté depuis par Apple, Netflix et AirBnb. 
Dremio, Snowflake et Tabular le gèrent. 
Avant d'en donner une vue détaillée, voici un cas d'usage. 
Spark va lire et passer du SQL sur les fichiers au format open table. 
Il "suffit" de le configurer pour l'interfacer avec Iceberg, et on peut passer du SQL au style Iceberg. 
On dispose alors des fonctionnalités d'Iceberg en passant du SQL à la sauce Iceberg. 


Au niveau du fonctionnement, on va détailler: 
* __Iceberg n'est pas à proprement parler un format de stockage de fichier, mais un format de stockage de tables, se basant sur le stockage existant des fichiers pour les décorer en tables__
* La couche calcul va créer des tables au format Iceberg et insérer de la donnée. Iceberg va s'appuyer sur le stockage pour créer et gérer la donnée qu'on insère
* Iceberg maintient un _data catalog_ qui contient de la méta donnée pour chaque table. Chaque fois que la table est modifiée dans sa structure, une nouvelle version de la méta donnée est créée, mais Iceberg garde toutes les versions. 
* Iceberg utilise cette méta donnée pour gérer les fichiers de donnée. Ces fichiers peuvent être stockés en Avro, parquet, etc 


[Plus spécifiquement](https://iceberg.apache.org/spec/#overview): 
* chaque table est décrite dans un metadata file. Il contient les partitions, les informations sur les snapshot, le schema, etc. A chaque changement, un nouveau fichier est créé, le catalogue pointe vers la dernière version valide du metadata file 
* les manifest files stockent les informations sur les data files, avec des informations statistiques sur les colonnes
* les manifest lists gèrent un _snapshot_ de la table, donc les données de la version correspondante à ce snapshot. On ne veut pas réécrire à chaque fois la table, donc ces manifest lists font le lien entre les metadata files et les manifest files
* les data files peuvent être soit en parquet, avro 
```
CATALOG                   META DONNEE                                                       DONNEE 

Iceberg 
catalog 

- table 1 -- pointeur --> meta data file --> manifest list --> manifest file --> data files  
- table 2 -- pointeur --> meta data file --> manifest list --> manifest file --> data files 
```


### Apache Hudi 

C'est Uber qui a initié le projet en interne, et le nom vient de Hadoop Upserts, Deletes and Incrementals. 
Il dépasse la seule partie open table format, mais on peut l'utiliser uniquement à cette fin. 
Le principe est d'avoir une partie méta donnée (dans un répertoire `.hoodie`) et de la donnée partitionnée. 
Les fonctionnalités sont: 
* gestion ACID, suppression et mises à jour 
* _time travel_ 
* optimisation des performances avec du clustering (regroupement au même endroit de données similaires), des index, de la compaction 
* l'historisation des commits (_commit timeline_)


### Linux Foundation Delta lake 

Initialement développé par Databricks, le projet a été confié à la Linux Foundation. 
Il permet de gérer Spark, PrestoDB, trino notamment. 
Il met en place aussi une gestion par de la méta donnée, avec des fichiers pour gérer les transactions. 
La table qu'il gère est appelée une _delta table_. 
Ses spécificités sont: 
* la gestion des schémas avec le rejet de la donnée si elle ne le vérifie pas, ainsi que la mise à jour des schema 
* la possibilité d'écrire dans les tables par batch ou streaming avec la même interface 
* le _vacuum_ permet de supprimer de la donnée historique au choix 
* le _cloning_ permet de cloner une table sans la réécrire 

### Lequel choisir ? 

Iceberg: 
* supporte parquet, Avro et ORC 
* s'intègre bien avec AWS, GCP, Dremio, Tabular et Snowflake 

Hudi: 
* gère parquet et ORC, pas Avro 
* s'intègre bien avec AWS et Onehouse 

Delta lake: 
* gère très bien Spark 
* ne gère que Parquet 
* très bonne intégration avec Azure, Databricks, Microsoft Fabric
 


# Sources

* Practical lakehouse architecture. Thalpati, 2024
* [Apache vs parquet](https://www.decube.io/post/what-is-apache-iceberg-versus-parquet)
* [Documentation officielle Iceberg](https://iceberg.apache.org/docs/latest/)
* [Structure du format Iceberg](https://iceberg.apache.org/spec/)