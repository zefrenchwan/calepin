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

 


# Sources

