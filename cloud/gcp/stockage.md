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