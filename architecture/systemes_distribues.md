# Systèmes distribués

* Copyright zefrenchwan, 2024
* License MIT 
* Pas de partenariat

## Les bases 

### Définir un contrat d'API 

Utiliser un système dont on n'a pas à connaitre les détails passe par une _API_.
Sur un système distribué spécifiquement, on utilise du RPC pour _remote procedure call_: 
1. client et serveur génèrent un code (dit _stub_) qui permet de communiquer par réseau interposé. Ce code gère la conversion des objets en éléments sérialisables, le réseau, etc
2. le client contacte le serveur via un appel synchrone 
3. le serveur répond (souvent, sinon le client doit le gérer)
4. le client utilise l'information transmise

Le concept est général, l'implémentation en JSON et HTTP étant une des possibilités, mais pas la seule. 
Sur le couple JSON + HTTP, il peut être long et pénible d'écrire le client à la main, mais on peut utiliser sur les gros projets des _IDL_ (interface description languages) qui vont générer le client. 
Un exemple d'implémentation est [OpenAPI](https://www.openapis.org/).	
On peut générer le client dans plusieurs langages du fait du JSON + HTTP. 

Ce qu'on va demander à un serveur en plus de l'API est son objectif de niveau de service, ou _SLO_ en anglais, soit des indicateurs de performance. 
Les plus fréquents sont:

| Nom anglais | Définition |
|------------|------------------|
| Latency | Temps de traitement d'une requête |
| Reliability | Pourcentage d'appels réussis |
| Throughput | Nombre de requêtes acceptées par seconde |


Rien d'absolu, souvent des distributions (percentiles: 95% des requêtes doivent... et 99% doivent...). 


### Sémantique de livraison de messages 

Sur une architecture basée sur des événements, ou en général avec un système de messages, on distingue plusieurs sémantiques de livraison: 
* at least once: un message peut être reçu plusieurs fois, mais une au moins 
* at most once: un message est reçu zéro ou une fois. Un cas d'usage où ce comportement est envisageable (mais pas optimal) est un ordre de paiement bancaire
* exactly once: une fois et une seule (le meilleur cas)


Sur les sémantiques _at least once_, un bon système devrait être idempotent, c'est à dire que l'état final du système est le même que le message soit traité une fois ou deux ou cinq ou mille...
Formellement, une opération est idempotente si elle donne le même résultat qu'on l'applique une fois ou plusieurs. 


K8s en fournit un cas très concret avec une déclaration de ce qu'on attend. 
Le système va alors finir par y arriver, mais appliquer deux fois une configuration devra donner le même résultat. 
En général, toute configuration déclarative est idempotente. 

### Qualité de la donnée 

Fondamentalement, on va regarder deux questions: 
* la donnée est elle la même dans tous les systèmes de stockage ? On parle d'intégrité relationnelle, ou en anglais _relational integrity_.
* quand considère t'on qu'une donnée est bien cohérente et donc que son écriture est finie ? On distingue la _strong consistency_ (on répond fini quand tous les sous systèmes l'ont confirmé) ou _eventual consistency_ (le système garantit à terme que l'écriture sera cohérente, mais il valide l'écriture dans le premier endroit écrit).


### Orchestration 

Imaginons que le code est bien écrit et marche. 
Reste à le déployer sur un système distribué. 
C'est la responsabilité d'un orchestrateur: on définit comment déployer et le système se débrouille à le garantir. 
Spécifiquement sur k8s, l'unité de base d'un déploiement, le plus petit bloc ayant un sens, est appelé _pod_. 

#### Quand est ce déployé ? 

Pour avoir une idée du statut d'un composant, on réalise des tests de statut ou _healthcheck_. 
On peut distinguer: 
* le conteneur est vivant ou non, c'est à dire qu'il répond ou pas (_liveness_)
* l'application est prête ou pas, c'est à dire sa _readiness_


#### k8s: le side car 

Une bonne pratique est de séparer clairement les responsabilités et donc les instances déployées d'une application. 
L'idée est que les conteneurs déployés doivent servir un but unique et qu'ils peuvent être scalables. 
A l'inverse, embarquer un back et un front dans une même image est une mauvaise pratique: un plantage de l'un bloque tout le déploiement. 

Par contre, on peut utiliser le pattern dit _side car_. 
Le contexte est d'avoir deux conteneurs déployés sur la même machine dans un pod k8s unique. 
L'un est un morceau de l'application, l'autre rend un service. 
Par exemple, l'un est un serveur HTTP, le side car embarque la partie HTTPS. 
Les deux doivent être sur la même machine pour réduire les échanges réseau, et le même pod. 
Ils partagent alors le disque, le réseau, etc. 


## Gérer un système distribué 

On veut réduire le couplage entre les composants d'un système distribué pour augmenter sa capacité à traiter différentes requêtes. 
Pour y répondre, les _microservices_ sont un style architectural fondé sur plusieurs principes: 
* des composants faiblement couplés, chacun réalisant une fonction spécifique 
* communiquant par une API bien définie (ce qui réduit drastiquement leur couplage). Par exemple, un service de ML ne fait qu'exposer `findObjectsInImage(image) -> list[object]`
* ayant une cohérence globale, formalisant un déploiement de composants interdépendants 

Les services sont donc largement indépendants, peuvent être développés à leur propre rythme, et peuvent être mis en oeuvre différemment. 
Par exemple, on peut déployer la version 2 de tel service sur 5 pods et la version 17 de tel autre sur 120 machines. 
Par contre, la mise à l'échelle (_scaling_) n'est pas la même en fonction de la présence d'un état: 
* un service sans état ne pose aucun problème quand on change le nombre d'instances actives 
* quand il y a un état, par contre, la gestion de la donnée utilise d'autres techniques et est plus compliqué: _sharding_ (partition sur plusieurs machines) ou _partitioning_ (partition au sein de la même instance)


En conséquence, les debug est bien plus compliqué que sur une application unique. 
Les équipes ont également une tendance à trop découper les microservices (et le réseau est beaucoup trop sollicité). 

### Utiliser Ambassador pour cacher la complexité des microservices

Pour réduire la complexité, il existe plusieurs patterns. 
Certains sont spécifiques aux microservices (Ambassador), d'autres non (Adapter). 

_Ambassador_ consiste à avoir un composant entre un service et l'extérieur, pour gérer des problèmes que le service n'a pas à connaitre: 
* Une implémentation peut gérer les retries, le monitoring, la sécurité
* Une autre peut être contactée par un client pour rediriger ensuite vers le bon microservice, comme une sorte de façade 
* Enfin, une autre peut faire office d'adapter en liant une interface attendue à une implémentation fournie par le service 

```
| Service | <---> | Ambassador | <-----> | Client |

| Client | <---> | Ambassador | <-----> | Microservice 1 |
                                <-----> | Microservice 2 |
                                <-----> | Microservice 3 |
```


### Répartir la charge avec un _load balancer_

Le principe est de faire traiter les requêtes par différentes machines. 
La méthode de répartition peut aller du basique tourniquet (_round robin_) à un système qui gère les sessions et envoie le même utilisateur sur la même machine. 
Dans tous les cas, chaque instance envoie au load balancer si elle est prête à accepter des requêtes (différence entre vivant (alive) et prêt (ready)).
Sur k8s, [ça se configure](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) très concrètement. 

#### Le cas de la gestion des sessions 

On veut qu'une requête avec une session donnée soit traitée par la même machine. 
L'avantage est qu'on peut utiliser alors un cache local plus efficace que si on doit redonder l'information en fonction du hasard. 
Si la même machine reçoit toujours les mêmes sessions, le cache aura plus de chance d'avoir la donnée sans aller chercher sur disque (ou en base) celle-ci (notion de _cache hit rate_).
Au passage, en terme d'usage, une répartition par IP n'est pas optimale, il vaut mieux utiliser une session id (portée au besoin par un cookie). 

### Utiliser un cache 

Le principe est basique: si deux requêtes sont les mêmes, on traite la première dont on met en cache le résultat, et la seconde ne lit que le cache. 
On parle de _caching layer_. 
C'est typiquement le pattern side car: 
* au sein du même pod, le cache et le service 
* le cache voit alors le service en local (et c'est le cas en effet)
* le service n'est appelé que quand il y a défaut de cache 

Par contre, le problème est qu'il faut déployer les instances en fonction: dès qu'il manque du cache ou dès que le service n'arrive plus à traiter la demande. 
En fait, s'il y a N instances de cache, il y aura potentiellement N copies du même résultat de la même requête. 
Il vaut mieux prendre N petit et beaucoup de ressources (mémoire notamment) sur chaque pod pour réduire la duplication. 
Pour ces raisons, on prendra plus un cache distribué indépendamment du service. 
Le load balancer distribue les requêtes sur le bon cache, et en cas de défaut de cache, l'application est sollicitée par le pod du cache. 
Par exemple: 1 load balancer, 3 caches, 2 applications déployées séparément. 
__ATTENTION: bien distinguer un service des instances (ou _replica_) qui le gèrent__

### Shard: quand chaque replica ne sait gérer qu'une partie des requêtes

Un point de vocabulaire: 
* un service est répliqué si chaque instance peut gérer n'importe quelle requête client. On a juste plus d'instances pour tenir la charge. Souvent, ce sont les systèmes sans état 
* un service est shardé (pas mieux, on pourrait dire partitionné, mais...) si chaque instance (ou _replica_) ne sait gérer qu'une partie des requêtes, mais que toutes réunies, elles traitent toute les requêtes. Souvent, ce sont les systèmes avec état. 


Pour cacher la présence de shards, on peut utiliser _Ambassador_ qui centralise les demandes et appelle le bon shard. 

#### Estimer le nombre de machines pour un partitionnement 

En pratique, on peut calculer le nombre de shards, disons pour un cache. 
Un système calcule des valeurs en fonction de paramètres, dont la taille maximum est 400Go. 
On a besoin d'un cache pouvant stocker ces 2To par clé de requête. 
Or, on constate qu'il faut servir 10000 requêtes par seconde. 
Chaque instance (pod, machine, bref) peut en gérer 1000 et dispose de 32 Go de RAM utilisable pour un cache. 
Il faut donc 10 machines pour servir les requêtes. 
Si on ne partitionne pas la donnée sur des shards, chaque machine peut stocker 32Go de RAM, mais ce peut être la même donnée que son voisin. 
On aurait donc 32Go gérés sur les 400Go possibles, soit environ 8% de donnée en cache (le _hit rate_). 
Si au contraire on shard, on utilise une partition des données par shard, ce qui permet de stocker 320Go de données, soit 80% des valeurs possibles ! 
C'est déjà un excellent moyen de préserver le backend. 


Au passage, attention à la position du cache vis a vis du service: 
* si le service lit la requête et accède après au cache, la capacité à servir les requêtes est donc bien celle du service. 
* Si le cache est AVANT le service, donc au plus près du frontend, alors c'est bien la capacité de service du frontend qui va déterminer la capacité à servir les requêtes 
En fait, la meilleure architecture, en fonction des machines et du cache, peut être: 

```
| Client | <----> | Front end | <-----> | Cache proxy | <----> | les shards du cache |
                                <-----> | Back end |
``` 

Donc, préférentiellement, mettre le cache le plus proche du frontend possible. 


#### En cas de plantage d'un shard 

Bien sûr, il faut se poser la question de la défaillance d'un shard. 
La charge sur le backend serait alors beaucoup plus grosse puisqu'un pourcentage de donnée conséquent ne serait plus en cache, donc à recalculer à chaque appel. 
Peut être d'ailleurs le backend ne tiendrait pas cette charge. 
Donc, la solution est de répliquer chaque shard, donc d'écrire N fois la donnée de chaque shard (avec N >= 2 évidemment).
C'est au passage ce qu'on retrouve chez Kafka, Hadoop, etc.  


#### Répartition dans les Shard

Attention, il existe un cas où un shard va traiter beaucoup plus de requêtes que les autres. 
Imaginons qu'on prenne un cache qui shard de la donnée uniformément. 
Autrement dit, étant donnée une valeur V, la probabilité qu'elle soit sur un shard pris parmi N est de 1/N. 
Le problème vient de l'accès à la donnée. 
Par exemple, on stocke des photos, on shard par hash du fichier. 
Imaginons qu'une photo fasse le buzz et se retrouve la plus accédée de tout le cluster. 
Et par buzz, on entend un nombre d'accès délirant par rapport au volume normal. 
Ce shard va devenir ce qu'on appelle un _hot shard_. 
Sans réplication, ce serait probablement le plantage du shard, puis du backend qui servirait la même photo sans fin. 
On peut également réaliser de la réplication dynamique d'un shard. 
Pas qu'il soit plein en données, mais pour répartir les accès à ce shard. 
Par exemple, si on a N machines, on peut copier les shards les moins accédés sur des machines qui ont la place. 
Une fois la copie terminée, on réalloue les machines vides pour gérer le hot shard en réplicant ses valeurs. 


Imaginons par exemple que les shards C et D soient peu accédés, B beaucoup, et que A explose. 
On passerait donc de la distribution du haut à celle du bas. 
```
| Cache proxy | <---------> [ Shard A ]
                <---------> [ Shard B ]
                <---------> [ Shard C ]
                <---------> [ Shard D ]


| Cache proxy | <---------> [ Shard A ]
                <---------> [ Shard A ]
                <---------> [ Shard B,C ]
                <---------> [ Shard B,D ]
```
 
__ATTENTION: Distribution uniforme de la donnée ne veut pas dire accès uniforme à la donnée__


### Scatter / Gather (Disperser / regrouper)

La problématique est le traitement d'une requête unique sur un système distribué. 
Chaque worker va prendre sa part, ce qui permet de rendre beaucoup plus rapide (voire simplement possible) le traitement de telles requêtes. 
Le pattern tient son nom de la structure arboresente du traitement: 
1. La racine est la requête 
2. Les feuilles sont les traitements par les workers 
3. Les noeuds intermédiaires sont les opérations de dispersion (découpage en taches de base) et de rassemblement (agrégation pour traiter la requête). 


Les calculs et possiblement la donnée sont partitionnés sur les workers. 
Cette répartition peut être dynamique avec réallocation si un worker plante ou est trop lent.  
Par exemple, sur une recherche dans un FS distribué, on cherche tous les documents contenant les mots m_1 à m_n. 
L'idée est que chaque worker lise une sous partie des documents disponibles (si possible localement), et cherche dans chacun. 
Pas de noeud intermédiaire, chaque worker renvoie la liste des documents qui correspondent, et la machine racine (le master) se charge de prendre l'union de ce que les workers lui renvoient. 


Par contre, cette répartition n'est pas évidente: 
* les sources sont elles partitionnées ? 
* y'a t'il des goulots d'étranglement ? Disque, réseeau, mémoire, cpu ? 
* prendre trop de noeuds coute en réseau: on passe son temps à échanger avec plein de workers. Chacun va recevoir la requête, la traduire, créer éventuellement une JVM, traiter, fabriquer le résultat et le renvoyer. Si le worker passe peu de temps à chercher dans le document, c'est sous optimal. 
* prendre beaucoup de noeuds augmente la probabilité que l'un défaille ou réponde lentement. __Le temps de réponse est le temps maximum de réponse des workers__ et donc, si un est lent, la réponse est lente. Si la probabilité p de réponse en moins d'une seconde est de 99%, disons, avec N machines, la probabilité que tout le système réponde en moins d'une seconde est p**N

| probabilité pour chaque worker | Nombre de workers | probabilité de plantage |
|-----------|------------|---------|
| 0.99 | 5 | 4.9% |
| 0.99 | 10 | 9.5% |
| 0.99 | 50 | 39.5% |
| 0.99 | 100 | 63% | 
| 0.98 | 5 | 9.6% |
| 0.98 | 10 | 18.3% |
| 0.98 | 50 | 63.6% |
| 0.98 | 100 | 87% |

### Gestion des évênements et fonctions comme service 

Dans une architecture, on a le plus souvent un serveur d'applications qui tourne en permanence. 
Il est en général gourmand en terme de ressources. 
Certains clouds proposent de traiter des petites requêtes et de payer par requête, au lieu d'un serveur loué au temps. 
C'est la notion de _Function as a service_, ou de _serverless computing_.
En particulier, ce cloud peut gérer aussi bien la création et l'exécution des instances, mais aussi la gestion de la charge. 
Il est indispensable de ne pas avoir besoin d'un état pour la gestion de l'événement ou de la fonction. 
Cela ne veut pas dire que la dite fonction ne peut pas écrire dans une base, mais en tant que telle, son instance est détruite à la fin du traitement, et un éventuel état serait perdu. 
L'outil est très pertinent pour des petits traitements rapides et sans état, souvent pour lancer d'autres services.  


### Election de leaders pour gérer un traitement (ownership election)

Le besoin d'un leader de traitement se comprend bien pour des outils tels que Spark. 
Ce leader va coordonner les traitements et les répartir sur le cluster. 
Mais que se passe t'il si ce leader unique se plante ? 
On aimerait éviter de reprendre le traitement. 
Il lui faut donc un certain nombre de noeuds de secours pour continuer. 
N noeuds vont être retenus et prendront la main si lui défaille. 
Organiser ce système passe par une élection de leader. 
Certains systèmes, kubernetes notamment, garantissent qu'un conteneur qui plante est redémarré, et éventuellement déplacé sur une autre machine si la machine a planté. 
Il n'est pas nécessaire d'appliquer des algorithmes tels que Raft ou Paxos, des outils l'implémentent ou permettent de le mettre en oeuvre (consul, etcd, zookeeper). 


## Les patterns de traitement par batch 

Ces batchs doivent tourner le plus rapidement possible, donc être parallélisés et localement rapides. 
C'est par exemple _Map Reduce_ qui a donné lieu à toute une industrie. 
Mais il en existe d'autres. 


### Queues de travail (work queues systems)

Le schéma producteur consommateur s'applique particulièrement bien aux systèmes distribués. 
Il est en plus indépendant du travail effectué par le système, d'où son isolation en tant que pattern. 
Pour mettre en oeuvre une telle queue, on utilise: 
* un service distribué 
* chaque conteneur gère la queue en tant que telle 
* dans ce conteneur, un ambassadeur permet de manipuler spécifiquement les objets. Ainsi, le même code de queue sera utilisé, l'ambassadeur se charge de la sérialisation-désérialisation spécifique (image, video, objets, json, etc). 

```
Container group:
-----------------------------------

Work queue <----> Work queue            <------->   External work 
manager           source                            Queue source 
container         container 
                  (ambassador)
```


C'est déjà un bon début, mais la question est de savoir comment gérer dynamiquement la charge. 
Si on a un nombre de machines fixe:
* en cas d'énorme charge, les jobs vont s'empiler et potentiellement dépasser la capacité de la queue 
* en cas de grosse charge, les jobs vont attendre d'avoir un worker disponible 
* en cas de petite charge, des workers vont simplement ne rien faire 


La queue ne va pas juste stocker et donner de la donnée, elle a intérêt à produire et donner de la statistique dans son usage: 
* le taux entrant
* le temps moyen de traitement mesuré comme la vitesse à laquelle une instance demande un nouveau job à traiter 
* en fonction, on peut déterminer si la queue va saturer, quand, etc 


Dans le cas de cloud, on aimerait avoir la possibilité d'avoir exactement assez de workers pour gérer la charge.  
Ce qui veut dire que les statistiques de queue permettent l'allocation: 
* de ressources dans la queue pour gérer de la donnée 
* de ressources côté consommateur en fonction de la charge

A noter que kubernetes le permet aussi, c'est le projet KEDA (Kubernetes Event Driven Autoscaling). 

#### Le pattern du multi-worker 

Quand un job est dépilé, il peut impliquer d'appeler plusieurs services: 
* chercher la donnée 
* analyser la donnée 
* diffuser la donnée 
* monitorer le traitement 


Et donc, on confie côté consommateur la responsabilité à un traitement qui gère les autres. 
On parle alors de _Multi Worker Aggregate_, qui va lancer les tâches les unes après les autres. 


### Gestion des évênements en mode batch (event driven batch processing)

Si le système de queue est pertinent, on peut avoir à gérer des flux complets de transformation de données, ou _workflows_. 
Un job est lancé, il donne lieu à plusieurs jobs à traiter ensuite, etc. 
Simplement demander à un job de poster dans la queue la donné du job d'après peut ne pas suffire (ne serait ce que pour le debug ou pour faire le join entre des données sources). 
Les patterns se basent sur une working queue comme brique de base mais vont la décorer pour fournir des fonctionnalités plus intéressantes. 
__ATTENTION: le contenu de la queue est bien la donnée à traiter, pas les jobs en eux mêmes__
* _copier_: copie la donnée source en plusieurs exemplaires. Le cas d'usage est de faire tourner deux traitements en parallèle sur la même donnée (analyse de sentiment et recherche d'entités nommées sur le même texte).
* _filter_: laisse passer de la donnée correspondant à certaines conditions exclusivement. Si le système est distribué, il ne garantit pas nécessairement le même ordre 
* _splitter_: pour le traduire en code `si condition(data) alors aller dans telle queue sinon aller dans telle autre`
* _sharder_: répartir de la donnée suivant une fonction de répartition dite _sharding function_
* _merger_: à partir de plusieurs flux entrants, produit un unique flux sortant (déduplication par exemple)

#### Amélioration de la performance et de la fiabilité 

Jusque là, l'hypothèse implicite était double: 
* les consommateurs dépilent à la même vitesse 
* les données traitées prennent le même temps indépendamment du worker qui les traitent
* chaque traitement réussit 


Imaginons qu'une machine ou un conteneur spécifique prenne plus de temps que les autres à traiter la donnée. 
Plus généralement, imaginons un consommateur qui traite bien moins vite que les autres sa donnée. 
La solution dite de vol de travail ou _work stealing_ consiste à lui retirer au niveau de la queue une partie du travail pour la redistribuer. 


En cas de plantage d'un consommateur, on aimerait que la donnée soit remise dans la queue pour être retraitée par un autre. 
Si le consommateur est juste lent, on risque cependant de faire traiter de nouveau la donnée. 
C'est la question de l'idempotence: il faut que ce traitement soit possible. 
Autre cas compliqué: c'est la donnée qui provoque le crash d'un consommateur. 
Dans ce cas, si on la remet dans la queue, on risque de faire planter en cascade tous les consommateurs. 
On peut mettre en place une politique de retry pour limiter les tentatives de traitement (problème dit du _work poison_). 
On a alors deux queues: la queue des données en première tentative, et la queue des données en retry. 
On met en place une politique de priorité: la première est prioritaire et le système laisse passer de temps en temps des messages de la seconde. 


### Synchronisation dans les batchs 

Le problème est d'attendre une condition (que toute la donnée soit disponible) pour lancer la suite du traitement. 
On peut y arriver avec un merge sur N queues pour une seule sortie. 
La sortie contient alors toute la donnée (par construction) et on peut agréger dessus. 
C'est souvent un algorithme suffisant. 
Mais il existe des cas plus complexes. 

#### Le join 

Imaginons qu'on ait un découpage de la donnée pour son traitement. 
On n'a pas juste besoin de la donnée agrégée, mais d'attendre d'avoir récupéré toutes les parties pour reconstituer un bloc. 
C'est par exemple le cas d'un flux vidéo qui est assemblé comme 30 ou 60 images par seconde, dans l'ordre, jusqu'à produire une seconde de vidéo. 
Dans ce cas, un join va agréger la donnée par bloc. 
On attend que toute la donnée du bloc soit arrivée pour produire le résultat. 

#### Le reduce 

Etant données N sources, on les regroupe en une seule sortie, jusqu'à avoir une seule sortie par type d'entrée. 
C'est ce que fait la partie reduce de map reduce, qui produit pour une clé la liste des valeurs correspondantes. 
Le reduce peut se faire en plusieurs temps, et se parallélise mieux que le join.

### Surveillance et observabilité 

Le log individuel n'est pas exploitable en l'état, il faut des outils pour les regrouper, les suivre, ou produire de la donnée pour mieux comprendre. 
Des outils tels que Prometheus le permettent.
On a intérêt à attacher une sorte d'ID fonctionnel dans les traitements, pour pouvoir les suivre isolément. 
On parle de _correlation ID_, et il existe des outils pour les gérer, comme le projet _Open Telemetry_.
Que ce soit par niveau de log, système émetteur, correlation id, on peut agréger la donnée et la visualiser à des fins d'analyse. 

### Servir des modèles d'IA, inférer à partir des données 

L'IA existe depuis des dizaines d'années, mais a pris la lumière avec les LLM et le deep learning. 
Cette IA en particulier est basée sur des modèles, qui sont le poids des connexions du réseau de neurones sous jacent. 
Ces poids sont appris par entraînement. 
Le modèle est utilisé en soumettant une entrée, qui produit une prédiction, via une API dédiée (généralement un service web). 
Cela pose quelques questions: 
* la taille du modèle et la puissance de calcul nécessaire à sa mise en oeuvre. En particulier, certains modèles utilisent de la GPU. On a aussi l'apparition de _small languages model_, ou comment réduire la taille des modèles sans trop dégrader la performance 
* la latence acceptable: combien de temps entre la requête et la réponse du modèle ? Vu sa taille, le modèle ne doit pas être téléchargé si possible, utilisé localement, probablement mis en mémoire ou en cache. 


Par sa nature même, le modèle est entrainé puis livré. 
Il a une faculté limitée à s'adapter à de la nouvelle information. 
Le concept de RAG est donc: 
1. Etant donnée une requête, chercher l'information contextuelle pertinente (géolocalisation de la personne, contacts, etc)
2. La requête est reformulée et enrichie avec l'information contextuelle 
3. Le résultat est produit et envoyé à l'utilisateur 

L'information produite est plus pertinente. 
Le modèle n'a pas besoin de tout savoir d'un utilisateur (meilleur pour sa vie privée).

## Points d'attention dans la mise en place  

#### The thumbering herd (émergence d'un comportement problématique)

Individuellement, un utilisateur a un effet marginal sur les requêtes que gère un système. 
Mais quand le système encourage une action, les utilisateurs la font en masse, ce qui peut créer un problème global par émergence. 
Par exemple, une page lente ou une barre de progression cassée peut forcer 90% des utilisateurs à rafraichir leur page. 
Le site va recevoir quasiment le double des requêtes habituelles. 
Si on a été prévoyant pour que la charge constatée maximale frôle les 70% de la capacité réelle du système, on a un risque de dépassement. 
Pour s'en prémunir: 
* _jitter_: mettre en place un délai artificiel, aléatoire et court, avant d'envoyer une requête pour lisser la charge. Prenons le cas d'un retry avec un _exponential backoff_. Si tout le système plante en même temps, tous les retries vont avoir lieu en même temps. Pour l'éviter, on lisse cette charge de retry avec un jitter
* _circuit breaker_: quand un sous système envoie trop de requêtes, il est temporairement sacrifié pour protéger tout le système et ne pas avoir un effet domino

#### L'absence d'erreurs est une erreur 

Un système complexe et distribué connait forcément des erreurs. 
On a alors un nombre de base d'erreurs, qui est inhérent à ce genre d'architecture. 
Dès lors, on ne voudra pas ne pas avoir d'erreur, mais que ce volume soit cohérent dans le temps. 
On va regarder alors les variations d'erreurs, pas le volume en soi. 
En particulier, pas d'erreur est pratiquement impossible. 


#### Toutes les erreurs ne se valent pas 

Le client peut envoyer des requêtes provoquant une erreur, même sans mauvaise intention. 
Par exemple, une erreur dans la saisie du mot de passe va lever une erreur d'authentification. 
Mais quand on a une explosion de ce nombre d'erreurs, c'est un vrai problème. 
On a donc un volume de requêtes client en erreur "normal" qui sert de référence. 

#### Les problèmes de versioning 

On distingue: 
* la version de l'API 
* la version des traitements 
* la version de la donnée stockée 

En séparant bien les trois, on a la possibilité d'établir des retours en arrière. 

#### Les composants optionnels qui se retrouvent pilier du système 

Le cas le plus évident est l'ajout de cache, vu comme une simple astuce technique pour accélérer le traitement. 
Sauf que sa mise en place va durer (parce que c'est facile) et le composant va rester (parce que le système en avait vraiment besoin). 

#### Risque de perte de donnée 

Attention sur la gestion du TTL de la donnée. 
On organise en général du nettoyage au fur et à mesure. 
Si ce code est buggé ou si le nettoyage est lancé à tort, le risque de perte de donnée est réel. 
On le compense par la mise en place de sauvegardes et de _circuit breaker_ éventuellement. 

#### La validation des entrées 

Le nombre de requêtes possible est énorme, et avec lui, on s'attend à un certain pourcentage d'erreurs ou de cas à exclure.
On peut bien entendu tester leur forme pour éviter des failles de sécurité (injection SQL par exemple). 
Mais il va rester des cas que le système ne sait pas gérer. 
Côté client, on envoie souvent la même requête. 
Quand celle ci plante, ce qui était un détail coté développeur est critique pour le client, une sorte d'effet projecteur. 
Donc, il s'agit de bien tester: 
* par fuzzing du code 
* par la validation d'un échantillon des données réelles 


#### Traiter de la donnée inutile 

Une requête a une durée de vie, et on peut exclure des requêtes devenues non pertinentes. 
C'est le cas d'un utilisateur qui rafraichit sa requête (F5): on n'a plus besoin de servir sa requête initiale. 
Autre exemple: sa déconnexion. 


Pour faire la différence, plusieurs solutions: 
* la requête a un timeout, une durée de vie, donc
* le système va gérer ses ressources et en allouer dynamiquement pour arriver à traiter bien et assez rapidement une requête 
* les requêtes d'un même client sont triées pour donner la priorité à la plus récente 

#### Et si on réécrivait tout ? 

Quand un système distribué semble de plus en plus problématique, la solution de tout réécrire peut être tentante. 
Mais pour garder le SI à flot, il faut maintenir en attendant le premier système. 
Donc, le second système est écrit et doit intégrer en plus les dernières évolutions du premier. 
Cette course sans fin fait que le second système n'est jamais prêt et constitue un temps perdu inacceptable. 
Sur les systèmes distribués, on a une solution. 
S'il est bien conçu, ses composants sont faiblement couplés. 
Donc il est possible au moins en théorie de le changer par bloc tant qu'on garantit les interfaces. 

## Sources

* Designing distributed systems, 2nd Edition, de Brendan Burns