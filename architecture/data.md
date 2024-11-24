* Copyright zefrenchwan, 2024
* License MIT 
* Pas de partenariat


# Introduction 

## Définition d'une architecture pour la gestion de la donnée 

Si on prend du recul, fondamentalement, une architecture data répond aux questions suivantes: 

| Nom | Description |
|--------------|------------|
| Stockage | Structures et support physique (disques ? cloud ?) |
| Traitements | Pour l'exploitation et la validation |
| Accès | API ? Interface utilisateur ? |
| Sécurité | Contrôles d'accès, chiffrement |
| Gestion de la vie privée | Conformité légale, anonymisation |
| Gouvernance | Qualité de la donnée, source, durée de stockage |


## Evolution des architectures pour la gestion de la donnée

### Apparition des data-warehouses 

Les bases de données relationnelles ont été la première solution, dans les années 70. 
L'organisation de fonde sur les tables comme objets de base, constituées de lignes. 
Chaque ligne est une donnée unitaire, chaque colonne un attribut. 
Le schéma de donnée est vérifié à l'écriture. 
D'autres solutions de sauvegarde font les vérifications à l'accès, dit (validation de) _schema on read_. 


Dans les années 80, les premiers data warehouses étaient aussi basés sur le modèle relationnel, avec des SGBDR. 
Ils comportaient aussi une partie calcul pour le traitement des requêtes. 
L'idée était que les serveurs étaient peu performants, et pour ne pas perturber l'activité, on séparait la donnée dans un RDW (relational data-warehouse). 
Tous les systèmes s'y déversaient: produits, CRM, ventes, etc. 

### La business intelligence 

Tout part d'un besoin de comprendre son activité pour rester compétitif. 
Par exemple, saisir la dynamique d'un marché pour développer telle offre et pas telle autre. 
On peut définir la _business intelligence_ comme l'analyse de la donnée dans le but d'améliorer la prise de décision métier. 
Par extension, il s'agit de tous les outils qui permettent une analyse et une représentation claire et synthétique de ces données. 

Déjà, pour la culture, on distingue deux types de traitements pour les bases de données: 
* OLAP: on agrège de la donnée qu'on analyse, à froid 
* OLTP: on traite en temps réel et de manière transactionnelle de la donnée, sans agrégation 

| Nom | But | Type |
|-------|-------|-----------|
| OLTP | Faire tourner le business | SGBDR |
| OLAP | Comprendre le business | DW |


Pourquoi online ? Raison historique, rien de fondamental. 

#### L'émergence d'OLAP

Requêter une base de données qui contient les informations métier a posé au moins deux problèmes: 
1. la surcharge de la base métier en question qui doit gérer les requêtes métier et les analyses avec les mêmes ressources.
2. Son impossibilité à répondre aux requêtes car le volume à traiter est énorme 

On a tout intérêt à séparer les données dans une autre base, en effectuant déjà les agrégations pertinentes : bref, on fait un cube OLAP. 
Une dimension est un agrégat qu'on va vouloir requêter et filtrer. 

```
select dimensions, operations(metrics)
from ...
group by dimensions 
``` 

L'idée est alors de fabriquer un pipeline qui prend ses données des sources OLTP pour les agréger dans un cube OLAP. 
l'avantage est de gérer mieux la charge, les inconvénients sont le coût humain et le délai inhérents au développement technique. 

#### Les bases de données colonnes 

Si cette culture d'OLAP est toujours très présente, elle a évolué: 
1. Car les machines sont plus puissantes 
2. L'émergence de solutions cloud telles que Google Big Query ou Amazon Redshift ont réglé la question de la puissance de la base de données (à condition d'avoir une bonne CB)
3. Les bases de données colonnes sont une meilleure solution pour faire de l'analytique qu'une base de données de type SGBDR sur laquelle on ferait du OLAP


Le principe est de passer d'un stockage par ligne, impliquant un full scan de toute la donnée, à un stockage par colonne, permettant de prendre uniquement les "bonnes dimensions", donc les bonnes colonnes. 

#### Sources 

* Définition de la BI [ici](https://business.adobe.com/blog/basics/business-intelligence-definition)
* Définition d'un DWH [ici](https://www.oracle.com/database/what-is-a-data-warehouse/)
* Transision OLAP vers BDD Colonnes [ici](https://www.cartelis.com/blog/declin-cube-olap/). 


### Vers les data lakes 

Dans les années 2010, les data lakes apparurent, comme étant un stockage objet sans calcul. 
L'exemple canonique est HDFS, et les clouds suivirent. 
On sauve la donnée sous forme de fichier, dont on fait sens à la lecture (schema on read). 
La promesse d'éditeurs tels qu'Hortonworks, cloudera et Mapr était simple: remplacer les data warehouses, tout mettre et faire sens après. 
Ce data lake pouvait alors contenir des fichiers, des données de senseurs, de webapps, des bases de données voire des documents, de l'audio, de la vidéo, etc. 
On pensait qu'il suffisait d'un Hive ou d'un bon notebook jupyter pour interpréter la donnée. 
Mais évidemment, nettoyer et charger la donnée était beaucoup plus difficile, et le data lake parut alors très compliqué à exploiter et très gourmand en ressources, humaines comme matérielles. 
Il en résulta la fin d'Hortonworks et de MapR. 


On prit alors le meilleur des deux mondes (datalake et RDW) pour batir les _modern data warehouses_ autour de: 
* le data lake pour la collecte et le nettoyage des données (data engineering). Puis les data scientists traitent la donnée pour fabriquer un modèle utile à la décision. 
* le data warehouse pour les besoins métiers, à lui de gérer les problématiques de sécurité, conformité, etc 


Les _data fabric_ apparurent en 2016 comme une amélioration du concept de modern data warehouse (MDW).
Pas de rupture technique majeure, une amélioration de toutes ses composantes. 
Le nom vient du fait que ce système, cette data fabric, peut ingérer tous types de donnée.  

### Centralisation autour du lake ou data mesh ? 


Databricks, en 2020, popularisa le concept de _data lakehouse_.
Le concept est de se débarasser de la partie data warehouse pour arriver à ce que la composante lake devienne la seule composante. 
En particulier, il devient la source de base pour la partie data warehouse. 
Comment faire ? 
Et bien en utilisant une architecture en delta avec un outil, le _Delta lake_ qui donne l'illusion qu'on travaille sur une base relationnelle (comme un RDW). 


En 2019, apparut la notion de _data mesh_. 
Tous les concepts avant (MDW, data fabric, data lakehouse) se basaient sur l'idée d'une donnée centralisée. 
Là, on parle d'une idée, dont l'implémentation sur étagère n'existe pas. 
Mais sur le principe, on revient à des services qui gèrent chacun une partie du SI (ventes, crm, produit, etc). 
Chacun a son équipe IT et gère sa donnée, et leur besoin et architecture sont autonomes les uns des autres. 
L'entreprise s'organise, chaque partie de la donnée étant alors accédée au besoin par les autres. 
Pour faire une analogie avec les microservices, chaque partie du data mesh est accessible par les autres et réalise parfaitement son besoin. 

# Les concepts communs aux architectures data 


## Entrepôt de données relationnel (Relational Data-warehouse, RDW)

Le cas d'usage est la business intelligence, c'est à dire l'analyse métier pour la prise de décision. 
La condition canonique d'utilisation reste soulager les bases de données effectivement utilisées en production. 
On récupère alors la donnée de chaque source pour l'agréger au niveau entreprise. 
Ainsi, un data-warehouse est par définition la _single version of truth_, la seule source de vérité. 
Et si c'est la seule source d'information agrégée de l'entreprise, on parle d'_enterprise data-warehouse_ (EDW). 
Au niveau architectural, il est pertinent aussi quand il réduit les dépendances entre les applications et les sources de données. 
Par exemple, les applications de finance, marketing, ingéniorat, RH prennent leurs données de plusieurs sources. 
On a alors des services qui doivent écrire dans plusieurs bases (HR notamment). 
Avec un EDW, la donnée est envoyée et traitée dans un endroit unique.  
Dès lors, il faut un _master data management_, c'est à dire un suivi des transformations pour produire la donnée. 
L'idée est de pouvoir expliquer la provenance, détecter les doublons et les éviter, et garantir la cohérence de la donnée. 
Attention au niveau des tables à ne pas avoir un fouillis inextricacle de tables et databases qui règlent chacune un problème trop spécifique. 

### Quand et comment le mettre en oeuvre 

Le paradigme dominant étant le relationnel, les RDW l'appliquent aussi, se fondant sur les technologies des SGBDR. 
Mais il en existe d'autres non relationnels (colonnes, nosql, graphes). 
Ils ont moins de succès car il leur manque la lingua franca qu'est le SQL. 
De plus, un modèle de RDW:
* est facile à utiliser dans le sens où les opérations de base (CRUD) sont connues et faciles à utiliser (insert data, update, etc). 
* permet d'intégrer plusieurs sources de données avec les mécanismes de jointure 
* permet de revenir en arrière dans beaucoup de données (par contraste avec d'autres solutions à base de fichier)

En général, un entrepôt de données:
* permet de détecter dans le SI (avec un plan de MDM) des données redondées ou en désaccord 
* permet de compléter de l'information qu'un sous système aurait raté 
* soulage l'équipe IT si on donne la possibilité aux experts métier de créer des rapports 

### Ses faiblesses 

* le cout 
* l'intégration laborieuse de beaucoup de sources de données 
* les transformations sont couteuses en temps et source de bugs ou d'erreurs d'analyse 
* les volumes lus peuvent créer de la latence (au sens du temps d'exécution des requêtes)
* devenant le point central du SI pour l'analyse, il doit tourner 24h/24, ce qui pose des questions sur la maintenance 
* il risque de ne pas couvrir à terme tous les besoins data de l'entreprise 
* en terme de sécurité, par définition, c'est l'endroit le plus risqué pour un vol de données 


### Créer un DW 

Cette approche consiste à bâtir la solution technique à partir des besoins métier. 
Sur la façon de poser les questions business, on peut distinguer la description du diagnostic. 
Ce peut être comprendre ce qui s'est passé (description) ou pourquoi ça s'est passé (diagnostic). Sur le diagnostic, c'est beaucoup obtenir des croisements statistiques entre les données pour établir des liens (saisonnalité des ventes, par exemple). 
Par contre, le processus est long et couteux, en machines et en ressources humaines. 


Au niveau de la méthode, le plan consiste à: 
1. Saisir la stratégie d'entreprise. En déduire les questions à poser et les hypothèses à tester
2. En déduire ce que l'entreprise veut mesurer et ses KPI, qui constituent son besoin métier. On arrive alors aux rapports pertinents 
3. L'architecture du DW en découle: structure, modèle de donnée, comment intégrer cette donnée 
4. On construit en détail le data model: les schémas, les tables, leurs relations 
5. On alimente ce modèle avec la construction de la partie ETL 
6. On déploye les outils de BI (accès utilisateur, outils de visualisation et d'analyse) dans une approche économique 
7. On rentre dans une partie de maintenance et d'approfondissement des données 


L'ETL est aussi chargé d'absorber les changements dans les sources. 
Par exemple, une source change de nom de table ou supprime un schéma. 
La partie debug et compréhension de l'erreur peut être fastidieuse. 
Si les tables du RDW utilisent directement les tables sources, les reports vont planter. 
L'ETL va devoir absorber cette charge en un point unique. 

### Alimenter un DW

Les questions à résoudre sont: 
1. A quelle fréquence charger la donnée. On la trouve en fonction du besoin client mais aussi des ressources disponibles pour la transformation et le temps que prend le processus. Inutile de faire des batchs toutes les heures si la transformation en prend deux...
2. Les méthodes d'extraction. Deux axes: extraction incrémentale vs relecture complète, avec accès direct à la source ou par un export de la source dans un format intermédiaire (export fichier, API)
3. Savoir quand la donnée a changé. On peut utiliser des timestamp pour horodater la donnée, utiliser un mécanisme de _change data capture_ (logguer les ordres insert, delete, update dans la source et appliquer que ces changements), utiliser les partitions de la source (par date par exemple), mettre en place des triggers dans le SGBDR source. La pire solution étant une relecture totale pour faire un merge de la donnée 


### La fin des RDW ? 

Déjà, dans les années 2010, le passage à un pur datalake a souvent été source d'échecs du fait du manque de structure de la donnée. 
La mise en place des deux solutions en parallèle (les delta lakes) a donné de meilleurs résultats. 
La fin des RDW est remise en cause du fait de la plus value réelle métier des DW et de la confiance dont le modèle relationnel dispose. 


## Les data lakes 

La suprématie des bases relationnelles a posé question quand de nouveux types de données ont été exploitables: fichiers audio, video, csv, textes dont les mails, etc. 
On avait la possibilité de les stocker massivement et l'envie de les exploiter. 
L'autre problème avec les bases relationnelles au sens large était leur difficulté à gérer de très gros volumes (en général) et à mettre à jour souvent la donnée (RDW). 
Ainsi, l'explosion des volumes de donnée et la facilité à stocker la donnée brute a crée le besoin d'un nouveau paradigme: les data lakes. 
Voici un cas d'usage: 
1. les commentaires des réseaux sociaux sont intégrés en textes dans un lake 
2. Ces textes sont catégorisés (détection d'émotion). L'analyse spécifique des messages négatifs montre un lien avec des mots spécifiques
3. Correction métier des sources de mécontentement et début de l'itération suivante 


__Le principe est de stocker de la donnée massivement, sans schéma ou structure, et d'en faire sens à la lecture__ (schema on read).
Il faut donc une énorme puissance de calcul pour lire cette donnée et en faire sens, en plus de la faculté de stockage. 
En faire sens implique une phase de lecture, nettoyage, jointure avec d'autres données. 

### Pourquoi utiliser un data lake ? 

En complément d'un RDW, un lake est pertinent:
* pour ses couts de stockage plus faibles qu'une base relationnelle, et le fait qu'il n'y a besoin à l'écriture d'aucun schéma. Ainsi, si le DW est la source unique de vérité, le lake devient la source unique de données, d'où repartir en cas de doute sur le traitement.
* pour mettre en place des modèles de data science 
* pour la partie exploratoire à partir de la donnée brute. Si elle se révèle pertinente, on peut l'inclure dans un processus ETL en production au lieu de le faire _a priori_. 
* utiliser la puissance de calcul du lake pour préparer la donnée du DW, et écrire le WH seulement en cas de succès et le plus vite possible (pour réduire la durée de maintenance)


Parce que le cout de stockage devient minime avec un lake, les processi exploratoires de données permettent par nature de créer de la valeur en regardant la donnée disponible. 
La structure en fichiers est la plus utile aux data scientists, ce qui provoque l'explosion de l'analyse prédictive avec leur travail. 
En conséquence, on retrouve l'analyse dite prescriptive qui utilise les modèles prédictifs pour prendre les meilleures décisions. 


| Nom | Etape 1 | Etape 2 | Etape 3 |
|-----------------|-------------|--------|--------|
| Approche descendante | Structuration | Ingestion | Analyse |
| Approche ascendante | Ingestion | Analyse | Structuration | 


### Comment utiliser un datalake ? 

#### Répartir la donnée 

Même si idéalement on a une unique instance, il est possible d'avoir plusieurs data lakes:
* par découpage géographique, métier (comme les data mesh), ou nature de la donnée (un data lake client et un data lake pour la donnée interne)
* pour des raisons légales (RGPD) ou de souveraineté, de sécurité (séparation par niveau d'habilitation), ou pour séparer la production des tests 
* pour une gestion des risques, avec un plan de _disaster recovery_
* par durée de rétention ou séparation technique ou technologique


Le problème est bien sûr les actions pour transférer la donnée de l'un à l'autre. 


#### Structurer la donnée 
La facilité de stockage n'est pas une excuse pour tout y déposer sans gouvernance. 


D'abord, organiser la structure de fichiers. 
On peut organiser par niveau d'accès (sécurité), avec des critères de partition, des répertoires par version, des backups, des dossiers de méta donnée, etc. 
Une organisation fréquente est aussi par maturité:  
| Nom de la zone | Description |
|----------|------------|
| Raw / Staging / Landing| Donnée brute, immutable |
| Conformed / Base / Standardized | Donnée sous format unique, souvent parquet |
| Cleansed / Refined / Enriched | Donnée nettoyée et enrichie |
| Presentation / Curated / Analytics | Donnée agrégée et résumée | 
| Sandbox | Donnée copie de raw pour les DS, modifiable | 

## Gérer, stocker et traiter la donnée 



### Les problématiques de stockage 

Un _datamart_ est une partie métier d'un data-warehouse ( marketing, ressources humaines, finance, etc). 
Il correspond donc à un flux sortant du DW vers chaque partie. 
Cette spécialisation permet de développer des tables et indicateurs spécifiques. 
Idéalement, les équipes métier sont autonomes sans dépendance au service IT pour ces développements. 
Elles peuvent également appliquer une politique de sécurité et spécifiquement d'accès moins large que ce que propose l'IT. 



Un _operational data store_ est une vue au niveau entreprise de la donnée. 
Sa finalité est la présentation de la donnée en temps (quasi) réel, et sans profondeur d'historique.  
__Attention: un ODS est une vue instantanée, il n'y a pas de composante analytique. En particulier, rien à voir avec un DW__ ! 
Par contre, on peut appliquer des règles de nettoyage et des validations métier avant de valider la donnée dans l'ODS. 
Ainsi, il sert de base de staging à un DW. 


Ce qui nous donne l'architecture complète:

```
OLTP -- ETL --> base de staging ---- ETL -->|         |---> Data mart HR
                                            |--- DW --|---> Data mart finance  
OLTP -- ETL --> ODS - juste un transfert -> |         |---> Data mart marketing  
```

Un _data hub_ est un système de stockage qui sert à gérer ou échanger de la donnée entre deux systèmes. 
C'est un support tactique _versatile_ en anglais, c'est à dire polyvalent. 
Par exemple, servir de source de données à une marketplace. 


En résumé: 

| Usage / Capacité | Data hub | Data lake | RDW | Data calaog |
|------------------|----------|-----------|-----|-------------|
| But dans le SI | Distribution | Traitement, analytics | Traitement, analytics | Méta donnée |
| Type de données | Brut (ou presque) | Brut ou nettoyé | Traité | Meta donnée |
| Structuration | Tous types | Tous types | Structuré | Méta donnée |
| Traitements | Intégration / Distribution | Calculs | ETL et requêtage | Rien |
| Cas d'usage | Ingestion / Distribution | IA, ML, analytics | Analytics et prise de décision | Gestion et découverte |


### Calculs et traitements 

La _master data_ est la donnée qui fait foi, celle qu'on peut utiliser pour une exploitation ou prise de décision. 
On parle de données de référence. 
A ce titre, le _master data management_ est la gestion des sources de données maitres: 
* l'ingestion et le nettoyage 
* les transformations et fusions 
* la structuration et la hiérarchie des données (données niveau entreprise, département, etc)
* la validation technique et légale 


La _data virtualization_ est l'idée d'avoir des vues logiques (comme un DW) mais sans déplacer la donnée. 
Elle est liée au concept de _data federation_ qui consiste à ce que chaque partie du SI gère localement sa donnée (comme les états fédéraux aux USA), mais permettent de former un tout cohérent par la virtualisation. 
Le but est de ne plus avoir un DW central, mais bien un moteur de virtualisation qui va cacher la donnée et la lire de la source au besoin. 
Le _data virtualization engine_ va alors effectuer des requêtes dites fédérées, c'est à dire sur plusieurs sources.
Son avantage est le gain par rapport à la mise en place d'un DW avec copie de la donnée. 
Mais sa mise en place pose plusieurs questions: 
* la performance et les calculs: utilisation de caches, optimisation, etc. 
* le cout pour les systèmes sources qui sont requêtés comme back-end de l'engine 
* la sécurité avec l'accès des données regroupées 
* la fraicheur de la donnée, le nettoyage de la donnée du cache 


Pour comparer: 

| Axe d'analyse | Déplacer la donnée | Virtualiser la donnée |
|---------------|--------------------|-----------------------|
| Cout de stockage | Fort | Faible (index, cache) |
| Cout de mise en place | Créer et maintenir l'ETL | Cout moindre |
| Temps de mise en place | Long (ETL) | Court (itératif ou prototypage) |
| Sécurité | Donnée dupliquée donc deux fois vulnérable | Réduit la surface d'attaque |
| Fraicheur | A la fréquence de l'ETL | Récente |
| Validité | Risque de décrochage | Celui de la source |



Les _data catalogs_ sont une information centralisée sur les sources de données de l'entreprise: 
* serveurs et infrastructure 
* databases, schémas, tables, lien entre les éléments 
* processi d'ETL, et en général les logiques de transformation 
* qualité de la donnée, propriétaires, contraintes légales 
* scripts SQL de comment la donnée est gérée ou traitée
* sur le lake, les fichiers, leur structure 


### Acquisition de la donnée 

Il existe des _data marketplaces_ permettant: 
* à l'acheteur de visualiser, noter, évaluer le produit vendu. 
* au vendeur de valoriser et monétiser son patrimoine de donnée 


Leur cas d'utilisation typique reste de la donnée pour faire tourner des modèles. 
On trouve aussi bien des chercheurs, des développeurs ou des entreprises sur ces marchés. 



## Les approches de conception générale 

On verra dans le chapitre suivant les éléments spécifiques de conception pour une solution. 
Ici, on parle de choix de solutions. 

### OLTP vs OLAP 

Un OLTP est une base de données relationnelle qui est utilisée pour les opérations transactionnelles sur de la donnée unitaire. 
Par exemple, une gestion transactionnelle d'une opération d'achat d'un client (ouvrir une transaction, débiter, valider la commande, etc). 
Il est optimisé en ce sens avec une très faible latence pour les opérations unitaires CRUD. 
La donnée se mesure souvent en giga. 


Un système OLAP répond à une problématique de rapports et d'analyse métier. 
Il est donc très optimisé pour lire la donnée, généraliser ou zoomer à des niveaux différents (_slice and dice_). 
On écrit la donnée une fois, on la lit très fréquemment. 

```
OLTP -- ETL --> DW --> OLAP 
```


La structure de base est le cube, qui contient de la donnée agrégée par un certain nombre de dimensions. 
Une autre approche est la donnée dite tabulaire (_tabular data model_) qui consiste à utiliser un modèle par table sur laquelle on fait les opérations lors du calcul. 
La donnée se mesure en tera octets, et elle vient de plusieurs systèmes OLTP.


On parle alors de la _couche sémantique_ de donnée, parce qu'elle agrège et explique la donnée des OTLP.  
Ce qui amène à distinguer deux types de données: 
* la donnée opérationnelle, qui donne un état de l'information. On peut piloter son métier en temps réel 
* la donnée analytique qui est la vue historique et explicative de l'information. On peut piloter son métier au long terme et prendre des décisions stratégiques 


### SMP (un serveur) ou MPP (un modèle master worker)

Il existe deux types de traitement (processing): 
* Symmetric multi processing (SMP): un serveur unique qui traite la donnée. Au besoin, on rajoute de la puissance de calcul ou de la mémoire sur la machine. C'est l'approche historique
* Massively Parallel Processing (MPP): un modèle master worker, qui a remplacé le SMP face à la limite physique des machines. C'est un système distribué en mode master (ou control) et workers. On rajoute des machines au besoin


### Lambda architecture 

Le principe est d'avoir deux flux de données: un lent mais sûr, et un le plus rapide possible. 
Les deux sont réconciliés avant l'accès à la donnée (sur le niveau vue au sens vue sur la donnée). 
Elle se caractérise par: 
1. deux flux, dits _batch layer_ et _stream layer_. 
2. Ces deux flux sont le moins couplés possibles, et ont une attribution de ressources qui leur est propre. Ils sont développés comme deux flux séparés. Les flux ont intérêt à être sans état ou spécificité pour que l'on puisse utiliser indépendamment une source ou l'autre sans se poser de question
3. Une vue unifiée agrégeant les deux types de données (lent et rapide). On parle de couche de service. Elle décide si elle utilise la donnée la plus fraiche ou la plus sure


 ### Kappa architecture 

Spécifiquement pour le temps réel, l'architecture Kappa gère un flux de données unique et sans état. 
Ses caractéristiques sont: 
1. temps réel, l'événement est traité avec une latence proche de 0
2. un flux unique. Cela facilite la montée en charge (exactement, la _scalability_) en distribuant la charge sur plusieurs machines 
3. Sans état, ce qui permet de paralléliser au mieux sans maintenir un état partagé. N'importe quelle machine peut traiter n'importe quelle donnée de manière interchangeable 

Elle n'a pas d'intérêt sur la donnée historique, car elle va charger la partie service avec des requêtes nécessitant beaucoup de calcul. 


### Utiliser plusieurs sources dans la même application 

C'est la notion de gestion polyglotte du stockage, ou _polyglot persistence_. 
Concrètement, la donnée de chaque partie du SI peut être stockée dans un outil spécifique (graphe, document, clé valeur, SGBDR, etc). 
Elle peut être agrégée ensuite dans un outil commun. 
Le cout de montée en compétence sur ces technologies spécifiques est à comparer au cout de forcer une technologie commune (et à ses risques): faire du document avec un SGBDR peut devenir un casse tête. 


## Approches de la modélisation de donnée 

Le but est de prendre une donnée source et de l'amener dans un modèle relationnel. 
La technologie source n'a pas d'importance. 
La modélisation relationnelle est la mise en forme en 3NF de la donnée, ce qu'on fait naturellement. 
La modélisation dimensionnelle est apparue en 1996, avec une organisation par faits et dimensions. 

### Modélisation dimensionnelle 

Les deux principales différences sont: 
1. L'usage de la dénormalisation, pour s'économiser les jointures (hyper couteuses) 
2. La présentation par fait / dimension, très orienté rapport  



| Fait | Dimension |
|---------------|--------------|
| Mesure quelque chose | Caractéristise quelque chose |
| Valeur numérique agrégeable | Valeur de type attribut (éventuellement hierarchisé) |

Sur les dimensions, la gestion du changement (slowly changing dimension) prend trois formes: 
* On réécrit la donnée, l'ancienne n'a plus d'intérêt. C'est le plus simple et le plus fréquent 
* L'ancienne version est historisée, la donnée nouvelle donne l'état le meilleur et le plus récent 
* Les deux données sont gardées (ancienne et nouvelle), les deux ont la même valeur et constituent l'historique de la donnée 

### Common data model (Modèle partagé de donnée)

Le principe est de bâtir un modèle propre à l'entreprise, et de ne pas prendre celui du fournisseur (cas du CRM) ou de la source de donnée. 
On a un modèle en propre, l'ETL garantit le contrat, et chaque application client utilise le même modèle. 

```
App client     App client        App client 
    |                |                 |
MODELE PARTAGE DE DONNEES, QUE TOUS PARTAGENT 
    |                |                 |
Source 1           Source 2 .......  Source N
```

### Data vault 

C'est un modèle de conception pour fabriquer un DW au niveau entreprise en utilisant trois concepts: 
* les __hubs (ou centres)__ sont des concepts métiers fondamentaux. Par exemple,  les clients, les produits, etc. Ils sont stockés en entier, avec leur id en propre. Le stockage peut inclure de la donnée telle que la date de création, une clé partagée, etc 
* les __links (ou liens)__ relient les centres et stockent les données comme des tables de correspondance (clé étrangère vers la donnée de base, donc sa clé primaire). 
* les __satellites__ stockent les informations sur les centres et les relations entre eux. Très concrètement, c'est la donnée historique des hubs. 


### Les méthodologies de modélisation de DW

Ce sont les méthodes Kimball ou Immon. 

#### Préliminaires: le modèle en étoile ou en flocon 

[Le principe](https://www.databricks.com/fr/glossary/star-schema) est d'avoir: 
* une table centrale, dite de faits, qui contient la clé étrangère de chaque valeur de dimension (pas la valeur, juste la clé), et les attributs associés
* pour chaque dimension, une table qui contient la clé primaire de la dimension et sa valeur

Le plan d'exécution est donc: 
1. Trouver toutes les valeurs des dimensions qu'on cherche (`pays = fr`, `date = 20241201`), à raison d'une table de dimension par dimension 
2. Aller chercher dans la table de faits les faits qui correspondent aux id des dimensions

```
DIMENSION: PAYS 
id -> 1, valeur -> FR 
id -> 2, valeur -> ES 


FAITS 
=====
id_pays -> 1, id_secteur -> 17, id_année -> 2024, ... pib = beaucoup d'argent 


DIMENSION: SECTEUR
id -> 17, secteur -> Biens de consommation
```

[Le modèle en flocon](https://blog.developpez.com/jmalkovich/p8718/modelisation/modele_en_etoile_ou_en_flocons) est un modèle en étoile avec une hiérarchie des dimensions. 
Pour une dimension donnée, elle est liée à son parent (par exemple région -> pays), le plus bas niveau est connecté à la table de fait. 


```
DIMENSION: CONTINENT 
id -> 1, valeur -> UE


DIMENSION: PAYS 
id -> 1, id_continent -> 1, valeur -> FR 
id -> 2, id_continent -> 1, valeur -> ES 


FAITS 
=====
id_pays -> 1, id_secteur -> 17, id_année -> 2024, ... pib = beaucoup d'argent 


DIMENSION: SECTEUR
id -> 17, secteur -> Biens de consommation
```

#### Méthode d'Immon (dite top down)

Le procédé est le suivant: 
1. Copie de la source (OLTP) vers une zone dite de staging, sans transformation. Le but est de réduire la charge sur le système source, et permet un audit éventuel de la donnée source 
2. Copie du staging vers la Corporate Information Factory (CIF), avec transformation et mise en 3NF de la donnée, sans agrégation. __ATTENTION: le CIF est la source unique et la plus détaillée de la donnée. Une seule instance de CIF au niveau de l'entreprise__. 
3. Cette donnée du CIF est ensuite agrégée dans des data marts spécifiques, éventuellement mise en cube 
4. Les data marts sont les sources d'un _reporting layer__ 

```
OLTP 1 -- Copie --> Staging area 1 -- ETL --> |          |--> Data mart 1 --> |
...                                           |--> CIF --|                    |--> Reporting layer 
OLTP n -- Copie --> Staging area n -- ETL --> |          |--> Data mart m --> | 
```

Le nom vient du fait qu'on commence par concevoir la couche de reporting, et on remonte vers les données sources. 

#### Kimball (dite bottom up)

La méthode de Kimball consiste en: 
1. Prendre, comme Immon, la donnée source et la mettre dans un staging, sans transformation 
2. Les tables staging sont agrégées dans des data marts spécifiques, suivant un modèle en étoile. __ATTENTION: les dimensions sont normalisées et centralisées (conformed dimension)__ : l'id de la valeur X pour la dimension D est le même dans tous les datamarts. 

```
OLTP 1 -- Copie --> Staging area 1 -- ETL --> Data mart 1 --> |
...                                                           |--> Reporting layer 
OLTP n -- Copie --> Staging area n -- ETL --> Data mart m --> | 
```

#### Les modèles hybrides 

Le modèle hybride consiste à:
* utiliser le CIF éventuellement, s'il est utile, sur une partie spécifique. 
* un OLTP miroir, utilisé pour soulager l'OLTP source 


## Ingestion de la donnée 

Les problématiques sont: 
* lire la donnée du système de traitement vers un DW (ELT ou ETL)
* envoyer la donnée du DW vers les systèmes de traitement (reverse ETL)
* batch à fréquence régulière ou gestion en temps réel  ? 
* la gouvernance de la donnée 


### ETL et ELT

L'ETL est la méhode standard, mais l'ELT s'applique mieux aux data lakes. 
* L'ETL charge la donnée, la transforme à la volée, et la sauve dans la destination. Si la transformation est buggée ou si elle échoue, il faut relire la source en entier. Il est très pertinent sur de petits volumes ou des transformations faciles 
* L'ELT déplace la donnée, puis la transforme (et stocke le format final). Il est plus adapté aux énormes volumes de données et peut tourner depuis la première copie (et pas encore la source). 
 
### Reverse ETL 

En général, la donnée va du système opérationnel vers le DW. 
Cependant, quand des systèmes opérationnels prennent des décisions avec de la donnée stratégique, on a besoin de faire l'inverse. 
Par exemple, la donnée agrégée est utilisée pour l'apprentissage d'un modèle de machine learning. 
Par exemple, exporter de la donnée client pour détecter les clients qui vont quitter l'entreprise (churn). 
On parle alors d'_operational analytics_. 

### Batch processing vs Real time processing 

* Le mode batch tourne régulièrement sur d'énormes volumes de données 
* le real time processing consiste en un flux de donnée qui arrive à destination, dans un contexte de réaction à des événements


### La gouvernance de la donnée 

Les problématiques sont: 
* les méthodes de collecte, stockage, sécurisation, transformation et usage de la donnée (reporting notamment)
* la validation de la qualité de la donnée, sa précision, sa qualité 
* la conformité légale et réglementaire 


# Les architectures de données 

Pour saisir le problème, cette grille d'analyse offre un panorama de la problématique: 

Dans la classification de l'usage de la donnée, on peut établir quatre niveaux: 
| Question | Méthode | Valeur | Difficulté |
|----------|---------|--------|-------------|
| Que s'est il passé ? | Descriptive | Faible | Faible |
| Pourquoi ça s'est passé ? | Diagnostic | Moyen | Moyen |
| Que se passera t'il ? | Prédictive | Haute | Haute |
| Comment faire pour que ça se produise ? | Prescrictive | Enorme | Enorme | 



La présentation va suivre l'ordre chronologique d'apparition. 


## Modern data-warehouse (MDW)


Si on reprend la grille d'analyse ci-dessus, il y a deux versions: 
1. L'analyse du passé, possible avec des solutions telles que des RDW 
2. L'exploration et donc la fabrication de modèles pour la prédiction et la prescription. Cette nature exploratoire rend le data lake le plus adapté à ce besoin 


Le concept est d'avoir les avantages du data-warehouse (donnée agrégée) sans latence. 
La capacité affichée du MDW est de permettre les deux approches. 
La solution technique passe par de la réplication de donnée dans un RDW, en plus de la présence d'un lake. 
__C'est la solution proposée par les principaux fournisseurs de cloud, donc Google avec un BigQuery (DW) et le système de traitement complet (GCS, Dataflow, etc)__ 
Attention, il s'agit d'une architecture haut niveau, on peut tout à fait: 
* combiner un stockage on-premise et des calculs dans le cloud
* développer un ETL qui termine dans un RDW on premise, et mettre toute la donnée non structurée ou non évaluée dans un cloud. Ce cloud aurait la charge de gros calculs. Une application de reporting utiliserait les deux sources (cloud et RDW on premise)
* l'usage temporaire de ressources externes (cloud) pour des calculs lourds

Le principe est le suivant: 
1. Tous les types de données, du moins structuré au plus structuré, sont stockés dans leur format initial. Streaming, batch, tout est possible. Attention à la bande passante nécessaire et la fréquence des batchs. 
2. La donnée est copiée le plus brut possible dans une zone de bac à sable pour que les data scientists la travaillent. On comprend alors qu'on peut faire preuve de pragmatisme et évaluer la donnée avant, quitte à intégrer directement la donnée hyper structurée dans un RDW
3. La donnée est raffinée au fur et à mesure jusqu'à être intégrable dans un RDW 
4. Faire tourner les modèles et les traitements des sources se fait par la puissance de calcul et de stockage de l'outil en général 

```
Donnée de base --> raffinement progressif    --> RDW  --> visualisation et exploitation par les analystes
               --> copie dans un bac à sable --> lake à usage des data scientists  
``` 

## Data fabric 

Si la définition a minima du MDW consiste à faire coexister un datalake et un RDW, la data fabric est l'idée de le pousser plus loin: 
* en puissance de calcul (virtualisation de données, traitements en temps réel)
* en offrant la possibilité de gérer par API ou créer des API 
* en intégrant des outils de sécurité nativement, et des politiques d'accès aux données. En particulier, la proposition de valeur est aussi la gestion de contraintes d'ordre légal (dont la GDPR)
* en mettant aussi l'accent sur la gouvernance et le data management (metadata catalog, master data management). Le metadata catalog n'est pas juste la méta donnée, mais aussi l'historique de la donnée (pas que la méta donnée), les sources, les transformations 
* en créant une plateforme unifiée où les outils coexistent et forment un tout cohérent 


Les arguments mis en avant pour le passage d'un MDW à une data fabric se fondent sur les constats suivants: 
* les données changent beaucoup et souvent en structure 
* les besoins en calcul sont très variables (problématique dite de scalabilité)
* le besoin en temps réel est de plus en plus fort 


## Data lakehouse 

La proposition de valeur est d'avoir un stockage unifié entre les données non structurées d'un lake et les données en 3NF des RDW. 
En particulier, on peut utiliser le DML de SQL (insert, delete, update), et une commande de merge (appelée _merge_...).
Databricks l'a théorisé avec sa notion de delta lake. 
La fondation Apache s'en est saisie aussi avec Apache Iceberg et Apache Hudi. 


Sur le principe, on dispose d'un _relational service layer_ qui comprend des commandes très proches du SQL. 
Les requêtes sont traduites en jobs qui vont lire les fichiers et traiter la requête. 
Pour le DML (au sens large avec merge), il est lui aussi traduit en traitements qui traitent et modifient les fichiers. 
Ces traitements se fondent sur un journal de changement des fichiers dans le lake (comme un SGBDR). 
__Par contre, écrire du SQL Like n'est pas disposer du paradigme relationnel !__
La structure sous jacente reste celle d'un FS, ce qui impose les limites suivantes:
* Application du schema à la lecture (schema on read)
* La gestion ACID n'est pas à prendre au pied de la lettre: les fichiers sont modifiés transactionellement, mais pas au global. 
* Pas possible de définir de relation entre les données dans les fichiers. Le système peut le faire via des vues mais elles seront calculées au moment de la requête


## Data mesh 

Le concept se définit par quatre principes: 
1. Des équipes indépendantes travaillent chacune à leur domaine métier 
2. Une cohérence globale est garantie avec une politique générale décidée au niveau de l'entreprise suivie de tous 
3. L'infrastructure sous jacente est prévue pour être variable, avec des mécanismes de provisionnement 
4. La donnée est vue comme un produit pour fabriquer de la valeur. Elle doit être utilisable pour tout usage d'entreprise, surtout ceux non initialement prévus (le client de la donnée peut en faire ce qu'il veut, dans les limites du bon sens et de la loi)


C'est bien un concept et pas une architecture déjà définie. 
En particulier, on peut utiliser des architectures propres aux solutions précédemment évoquées. 


### Architecture de données décentralisée 

Le département informatique en tant que tout ne va pas gérer le développement d'un unique data-warehouse centralisé. 
La raison mise en avant est qu'à certains volumes, les systèmes centralisés (les DW en particulier) ne tiennent plus la charge. 
La réalité est plus compliquée: c'est souvent avant tout une équipe IT désorganisée ou en sous effectif, ou bien de mauvais choix techniques. 
Toujours est il que les data mesh proposent une nouvelle stratégie: 
* chaque domaine (au sens métier) gère son propre entrepôt, il en devient l'utilisateur et le responsable pour le reste de l'entreprise 
* ce domaine métier gère toute la partie technique, de la collecte à la présentation, et donne lui même les modalités et droits d'accès à l'entreprise 
* la donnée n'est pas copiée dans un système central, elle reste dans le stockage du domaine comme source de vérité. __ATTENTION: ça n'exclut pas qu'elle soit copiée ailleurs ou transformée par d'autres domaines__ 


Pour réaliser une transition vers un data mesh, depuis un système centralisé, on peut utiliser un modèle de transition: 
* définir les contrats de donnée et les domaines 
* fabriquer les flux depuis le système centralisé vers le modèle décentralisé 
* stabiliser ensuite en utilisant, en cas d'échec, la solution centralisée comme solution de secours 
* s'assurer de la transition de culture 
* mettre en place et optimiser les flux vers les domaines 



### Les grands principes

#### Reprendre la responsabilité de gérer les domaines 

Les équipes les plus proches du domaine sont les mieux placées en tant qu'expertes pour: 
* faire grandir le système à hauteur de la charge nécessaire pour fabriquer l'entrepôt 
* gérer le plus vite possible les changements métier, car les utilisateurs sont le plus proche de l'équipe de développement technique 
* garder la vue sur l'usage et les accès à la donnée. Quand au contraire la donnée devient la propriété d'une équipe centrale d'IT, l'équipe source en perd l'usage 

#### La donnée comme produit 

Penser la donnée comme un produit signifie que la donnée a une valeur métier et doit être considérée comme le tout minimal cohérent et livrable qui répond à un besoin métier.
Chaque domaine produit de la donnée comme produit et les services qui l'utilisent deviennent les clients de cette donnée. 
Ainsi, chaque domaine se place dans une configuration de producteur: 
* en fournissant une infrastructure pour gérer leur donnée, sans que ce soit une question pour un service IT ou les consommateurs 
* la donnée, évidemment, et sa méta-donnée. L'équipe a donc à sa charge la sécurité, le contrôle des accès, etc
* Les échanges de données comme produit sont alors pensés suivant un contrat de donnée pensé pour l'extérieur. Ce peut être en exposant une API, en utilisant une base de donnée exposée aux autres, en mettant à disposition des fichiers 
* chaque domaine gère son IT et utilise ce dont elle a besoin en terme d'infrastructure 

#### Les équipes des domaines utilisent l'infrastructure selon leur besoin 

Le principe est que les équipes des domaines vont se spécialiser sur leur domaine. 
Pas question pour eux de gérer leur infrastructure et d'avoir à résoudre chacun de leur côté la même question. 
Le service informatique global assure l'infrastructure comme un service, c'est à dire que les équipes domaine vont se positionner comme utilisateurs de l'infrastructure selon leurs besoins. 

#### La gouvernance centrale, les domaines comme fédérations 

En fait, l'équipe IT globale va définir des règles de gouvernance et chaque équipe va appliquer cette politique à son domaine. 
Par exemple, cette équipe globale va s'informer sur la RGPD et définir des bonnes pratiques. 
Chaque équipe domaine va devoir appliquer les bonnes pratiques qui la concernent. 
Cependant, elles ont leur marge de manoeuvre. 
Par exemple, dire que l'accès à la donnée sensible doit passer par une gestion des droits est décidé globalement. 
Mais telle équipe utilisera telle solution technique, et pas telle autre parce que son besoin est différent. 

### Architecture logique d'un data mesh 

#### Le domaine métier de la donnée 

L'usage du DDD (domain driven design) n'est pas une évidence): 
* parce que sa mise en oeuvre est complexe et longue 
* parce que son application à la donnée est compliquée: que veut dire un domaine donnée ? Par exemple, quand on parle de produit ou de client, la frontière entre une application et un domaine métier est compliquée, et la question de la responsabilité très difficile (équipe produit ? Vente ?)

En revanche, on peut tout à fait partager un modèle commun de connée (_common data model_). 
Pour éviter les duplicats, il faut mettre en place des ID partagés. 


Il y a trois types de domaines de donnée dans un data mesh: 
1. Les domaines de données alignés sur la source: aucune transformation entre le système source et la donnée. 
2. Les domaines de données agrégées: ce sont les données condensés de sources multiples. Leur usage typique est la gestion technique de la performance 
3. Les domaines de données alignés sur les consommateur: de la donnée agrégée et prête à l'utilisation pour un consommateur de donnée 

#### Le découpage logique 

Il consiste à partir des sources et à arriver jusqu'aux consommateurs de la donnée: 
1. On liste les sources 
2. On créé un domaine métier qui va en général traiter un type de donnée (source, agrégé, ou client)
3. Ces domaines métier ont des dépendances les uns avec les autres, les uns avec les sources, les autres avec les consommateurs 
4. On en déduit alors un graphe de dépendance et une spécialisation de donnée 


Il y a alors une topologie de l'architecture, avec trois possibilités: 
1. Le type I: une répartition logique, mais en fait, tout le monde utilise le même support technique, un data lake global. Tous stockent la donnée au même endroit, avec des répertoires pour chaque domaine 
2. Le type II: la répartition logique donne lieu à un data lake par domaine. Mais tous utilisent in fine  le même socle technique. Par exemple, l'entreprise utilise globalement un cloud provider (disons GCP), mais chaque équipe peut utiliser telle ou telle partie des outils du dit provider 
3. Le type III: chaque domaine est totalement autonome sur son stockage et sa technologie 

| Type | Technologie | Stockage |
|------|-------------|----------|
| I | Commun | Commun |
| II | Commun | Spécifique |
| III | Spécifique | Spécifique |


### Quand adopter ou  ne pas adopter un data mesh ? 

La solution n'a rien de magique: 
* sa mise en place est plus longue que les autres architectures 
* elle ne remplace pas un lake ou un entrepôt de données, elle en créé plusieurs instances décentralisées 
* elle n'a pas vocation à être le meilleur atout technique pour les énormes volumes de données. Les causes d'échec des solutions précédentes sont plus souvent dues à des choix techniques inadaptés ou à des équipes incompétentes
* le concept de data mesh ne donne pas de détail d'architecture. Sa mise en oeuvre concrète peut prendre des mois à concevoir. La notion de source unique de vérité va poser question. Elle devient possible à mettre en oeuvre dans un data mesh mais c'est un choix IT de gouvernance 


# Quelle architecture ? 

On reprend et on résume. 
Comme tout résumé, il ne rend pas honneur à la complexité du produit, mais se veut une explication en une phrase du concept. 


| Nom | Principe |
|---------|------------|
| Relational DW | Donnée agrégée et centralisée dans un SGBDR |
| Data lake | Toute la donnée est stockée en l'état dans un énorme FS |
| Modern DW | Un lake pour stocker et transformer, finit dans un RDW |
| Data Fabric | Offre enrichie de MDW (sécurité, MDM, API, etc) |
| Data lakehouse | Lake avec les fonctions d'analytics, mais sans RDW |
| Data mesh | Concept de donnée décentralisée organisée en domaine | 



Si on prend de la hauteur, on peut alors établir des profils d'entreprises qui ont intérêt à mettre en oeuvre telle ou telle technologie:
* __Les modern data warehouses__: peu de donnée, familier avec le modèle relationnel 
* __les data fabric__: beaucoup de données de sources différentes, avec l'accent mis sur l'intégration facile des flux 
* __les data lakehouse__: solution intermédiaire, choix de non choix, permettant facilement un retour en arrière en fonction des succès et limites rencontrés
* __les data mesh__: quand l'enjeu de passage à l'échelle est le coeur du problème, pour les entreprises traitant d'énormes volumes de données et souhaitant un investissement fort et à long terme 

# Sources 

* Deciphering Data Architectures. James Serra, 2024

