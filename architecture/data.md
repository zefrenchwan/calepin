* Copyright zefrenchwan, 2024
* License MIT 
* Pas de conflit d'intérêt (personne ne m'a contacté, je ne fais pas de pub cachée)


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

## Stocker c'est bien, exploiter c'est mieux 

# Sources 

* Deciphering Data Architectures. James Serra, 2024

