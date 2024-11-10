# Les data-warehouses

* Copyright zefrenchwan, 2024
* License MIT 
* Pas de conflit d'intérêt (personne ne m'a contacté, je ne fais pas de pub cachée)


Tout part d'un besoin de comprendre son activité pour rester compétitif. 
Par exemple, saisir la dynamique d'un marché pour développer telle offre et pas telle autre. 
On peut définir la _business intelligence_ comme l'analyse de la donnée dans le but d'améliorer la prise de décision métier. 
Par extension, il s'agit de tous les outils qui permettent une analyse et une représentation claire et synthétique de ces données. 





## Du cube OLAP aux bases de données colonnes 

### Du bon usage des bases de données (la partie culture)

Déjà, pour la culture, on distingue deux types de traitements pour les bases de données: 
* OLAP: on agrège de la donnée qu'on analyse, à froid 
* OLTP: on traite en temps réel et de manière transactionnelle de la donnée, sans agrégation 

| Nom | But | 
|-------|-------|
| OLTP | Faire tourner le business |
| OLAP | Comprendre le business |


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

# Sources 

* Définition de la BI [ici](https://business.adobe.com/blog/basics/business-intelligence-definition)
* Définition d'un DWH [ici](https://www.oracle.com/database/what-is-a-data-warehouse/)
* Transision OLAP vers BDD Colonnes [ici](https://www.cartelis.com/blog/declin-cube-olap/). 

