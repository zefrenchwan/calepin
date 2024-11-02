# Systèmes distribués

Copyright thefrenchwan, 2024
MIT License
Pas de conflit d'intérêt (personne ne m'a payé ou contacté)

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
 
__ATTENTION: Distribution uniforme de la donnée ne veut pas dire accès uniforme à la donnée_.

## Sources

* Designing distributed systems, 2nd Edition, de Brendan Burns