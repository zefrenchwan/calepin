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

## Sources

* Designing distributed systems, 2nd Edition, de Brendan Burns