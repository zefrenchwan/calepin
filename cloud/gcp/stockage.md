# Stockage 

On peut stocker suivant différentes modalités

| Nom | Description |
|-----|-------------|
| Durée | Temps court vs sur des années |
| Ecriture | Batch vs Streams |
| Structure | Format défini vs texte, image, etc |

## Traduire un besoin métier 

Le cycle de vie de la gestion de la donnée (_data lifecycle) est:
1. Ingestion: faire entrer la donnée dans GCP
2. Stockage: la donnée est persistée dans GCP 
3. Traitement et analyse: transformer la donnée pour son exploitation à des fins d'analyse
4. Exploration et visualisation: pour une application métier, et aussi peut être pour des utilisateurs métier

### Ingestion 

Les trois modes d'ingestion sont: 

| Nom | Exemples |
|---------------|------------------|
| Application data | logs, formulaire client rempli, ...|
| Streaming data | Evenements client, données de senseur, ... |
| Batch data | Souvent par fichiers | 

#### Application data 

Attention déjà, leur définition est large et la donnée peut aller de petits éléments (clicks client) à des plus gros (images). 


__Pour l'ingestion__ les solutions techniques de GCP peuvent être: 
* [Compute Engine](https://cloud.google.com/products/compute?hl=fr): des VM qu'on peut exécuter 
* [Kubernetes Engine](https://cloud.google.com/kubernetes-engine/docs/concepts/kubernetes-engine-overview?hl=fr)
* [App Engine](https://cloud.google.com/appengine/docs/an-overview-of-app-engine?hl=fr)

Pour l'écriture:

* (le livre de la certification date donc Stackdriver a été racheté et renommé en [Google Operations Platform](https://cloud.google.com/products/operations?hl=fr), avec dedans Cloud Logging): [Cloud Logging](https://cloud.google.com/logging/docs?hl=fr) qui ressemble à ce que fait un datadog 
* Pour la partie base de données gérée, c'est [Cloud SQL](https://cloud.google.com/sql?hl=fr)
* Google met en avant Cloud Datastore, mais qui semble remplacée par [Google Firestore](https://cloud.google.com/firestore?hl=fr)

#### Streaming data

Tout ce qui vient de données de senseur ou d'événements, venant continuellement d'une source de données. 
Leur ingestion peut d'emblée nécessiter des traitements spécifiques. 
Pour des raisons de réseau, les données postées n'arrivent pas nécessairement dans l'ordre d'envoi. 
On peut alors, par exemple, avoir un état qui stocke la donnée pendant un temps estimé suffisamment long pour avoir un tout cohérent qu'on trie (par exemple par event date). 


La solution canonique reste [Pub/Sub](https://cloud.google.com/pubsub/docs/overview?hl=fr)

#### Données de batch 

En plus des batchs de transformation de données, on peut déplacer de la donnée vers un support de stockage (SQL par exemple) ou la faire ingérer par bloc dans GCP. 
Pour le stockage à proprement parler des fichiers, la solution est [Google Cloud storage](https://cloud.google.com/storage/docs/introduction). 
Pour transférer les données, c'est le [Cloud Transfer Service](https://cloud.google.com/storage-transfer-service?hl=fr). 
Il y a une grille de coût pour le transfer entrant. 
En particulier, pour faire arriver les données sur GCP depuis un système on-premise, si le ratio volume / réseau n'est pas suffisant, on déplace les données physiquement. 
C'est la solution de [Google cloud Transfer Appliance](https://cloud.google.com/transfer-appliance/docs/4.0/overview?hl=fr)


### Stockage 

Le stockage est orienté exploitation et analyse pour la partie suivante. 
Les points d'attention sont: 
* l'accès. Plusieurs pistes: a t'on besoin de requêtes (cloud sql ou datastore) ? Lit on valeur par valeur (_row_), ou par groupe de valeurs (_rows_) ? Enfin, si on est en mode batch à inclure de la donnée sans distinction, on peut prendre des fichiers sur GCS. Attention, on peut utiliser Firestore aussi avec son modèle document
* le contrôle d'accès, et à quel niveau on réalise ce contrôle. Les options de Google Cloud SQL et [Google Cloud Spanner](https://cloud.google.com/spanner?hl=fr) permettent un contrôle au niveau des tables, avec lecture, écriture (updates). On peut créer des vues avec des droits différents. GCS descend au niveau des fichiers, avec la possibilité de lire, écrire, ou lister les fichiers.  
* la durée de stockage. Elle a un impact sur les choix de disque, par exemple: en prenant Compute engine, on peut utiliser du SSD pour de la donnée éphémère. Pour le long terme, on préfère GCS qui a 4 modalités de stockage en fonction des conditions d'accès (standard, nearline, coldline, archive du plus chaud au plus froid). BigQuery est aussi une option. On peut d'ailleurs déplacer de la donnée de GCS à BiqQuery au dernier moment pour y accéder 

### Traitement et analyse 

Le but est de prendre la donnée stockée pour obtenir une forme utile aux requêtes ad hoc et à l'analyse. 


#### La transformation des données 
Que ce soit pour un batch ou en streaming, on utilise [Google Cloud Dataflow](https://cloud.google.com/dataflow/docs/overview?hl=fr). 
Au niveau de la façon de faire:
* pour le _nettoyage_, il s'agit de gérer le format de données, bien sûr, mais aussi la validité métier de la donnée 
* il y a aussi la _standardisation_ de la donnée, comme un format d'heure particulier à un fuseau spécifique

#### L'analyse des données 

L'emploi de la statistique est un excellent moyen de synthétiser l'information: 
* isoler les caractéristiques (moyenne et écart type, etc), histogrammes pour comprendre la distribution des valeurs
* mesurer le lien entre variables (corrélations). Avec des modèles de régression, essayer de prédire les valeurs de certains attributs en fonction des autres 
* par _clustering_, réaliser des partitions de données partageant les mêmes caractéristiques ou ayant des comportements similaires 
* sur la donnée texte, rechercher les entités nommées, réaliser des comptages


Les solutions GCP possibles sont:  
| Nom | Description |
|--------|----------------------|
| Cloud dataflow | Gestionnaire de transformation de données |
| Cloud dataproc | Faire du spark sur Hadoop dans GCP | 
| BiqQuery | Base de donnée orientée entrepot de données | 
| Cloud ML Engine | Ecosystème d'applications de ML |

### Exploration et visualisation

Les solutions proposées par GCP sont:
* [Vertex AI](https://cloud.google.com/vertex-ai?hl=fr) qui remplace Cloud Datalab (basé sur Jupyter) 
* [Looker  Studio](https://cloud.google.com/looker-studio?hl=fr) (ancien Data Studio)