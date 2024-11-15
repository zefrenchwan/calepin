

## Principes fondamentaux 

Si on prend du recul, fondamentalement, une architecture data répond aux questions suivantes: 

| Nom | Description |
|--------------|------------|
| Stockage | Structures et support physique (disques ? cloud ?) |
| Traitements | Pour l'exploitation et la validation |
| Accès | API ? Interface utilisateur ? |
| Sécurité | Contrôles d'accès, chiffrement |
| Gestion de la vie privée | 
| Gouvernance | Qualité de la donnée, source, durée de stockage |


## Evolution des architectures pour la gestion de la donnée

Les bases de données relationnelles ont été la première solution, dans les années 70. 
L'organisation de fonde sur les tables comme objets de base, constituées de lignes. 
Chaque ligne est une donnée unitaire, chaque colonne un attribut. 
Le schéma de donnée est vérifié à l'écriture. 
D'autres solutions de sauvegarde font les vérifications à l'accès, dit (validation de) _schema on read_. 


Dans les années 80, les premiers data warehouses étaient aussi basés sur le modèle relationnel, avec des SGBDR. 
Ils comportaient aussi une partie calcul pour le traitement des requêtes. 
L'idée était que les serveurs étaient peu performants, et pour ne pas perturber l'activité, on séparait la donnée dans un RDW (relational data-warehouse). 
Tous les systèmes s'y déversaient: produits, CRM, ventes, etc. 


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





