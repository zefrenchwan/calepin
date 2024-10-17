# Kubernetes

K8S pour les intimes est à la base un projet créé par Google en 2015 (première tentative en 2003 en C++, passé depuis en golang). 
Il est passé depuis en open source. 
La fondation qui le gère est aussi responsable d'encourager l'usage des conteneurs et les déploiements automatisés. 


Il sert principalement à:
* _automatiser des déploiements d'applications conteneurisées_ (disons sous Docker). On peut livrer une version, annuler la livraison
* _gérer les ressources_ consommées par ces applications conteneurisées en leur allouant plus ou moins de ressources (scale up et scale down)
* les monitorer et déléguer leur gestion (redémarrage, blocage)

## Pourquoi l'utiliser ?

Une application va tourner avec un langage, une version des librairies, voire sur un système d'exploitation spécifique. 
La pire solution est d'avoir des machines dédiées à une application.
Un peu mieux mais très couteux aussi: une machine virtuelle par application. 
La conteneurisation propose de porter tout ce qui est spécifique à l'application dans un conteneur le plus léger possible. 
Ainsi, on a: 
1. Le matériel et son OS
2. Le gestionnaire de conteneurs (_container engine_)
3. Des conteneurs gérés par le gestionnaire de conteneurs

Mais les conteneurs ne savent rien de leur hôte par sécurité. 
Ils ne peuvent pas se lancer ou se réparer automatiquement. 
Kubernetes va être déployé sur un cluster pour gérer les conteneurs. 
Du point de vue de l'utilisateur, on n'a plus besoin de gérer les machines et les ressources des machines. 
On ne voit que des _pods_ (en première approximation, des conteneurs gérés par kubernetes)

## Architecture et concepts

En quelques mots: 
* On déclare un _état souhaité_ en YAML. 
* Le _master k8s_ va se charger de la lire et l'installer sur _les workers_ qu'il contrôle. 
* Il garde à disposition un serveur pour voir l'état en cours du cluster ou recevoir des commandes. 
* Chaque worker du cluster k8s porte un processus, le _kubelet_, qui va superviser les conteneurs et en notifier le master. 
* Il gère des _pods_, c'est à dire des groupes d'un ou plusieurs conteneurs sur le même worker, qui partagent les mêmes ressources. 

Le _cluster_ k8s est géré par un _orchestrateur_. 
Un orchestrateur déploie et gère des applications. 
Ce cluster est composé de noeuds, de deux types: 
1. Les _control plane nodes_ (le master) obligatoirement sous linux, qui gère le serveur de contrôle, les propriétés globales (scaling). On peut gérer la HA (haute disponibilité) et en avoir plusieurs
2. Les _worker nodes_ (les workers, donc)

### Le rôle du control plane node 

Il gère ce qu'on appelle l'_API server_, donc le système qui interagit avec les demandes client. 
Par exemple, pour le déploiement ou la mise à jour d'une application, le cycle est le suivant: 
1. Description de l'état souhaité (_requirements_) dans un fichier de configuration YAML
2. Le fichier est envoyé à l'API server 
3. Celui ci valide la sécurité (authentification et autorisations)
4. Les mises à jour à effectuer sont alors calculées, sauvées dans le _cluster store_
5. Ces mises à jour sont ensuite planifiées (_schedule_) au sein du cluster

#### Le cluster store 

Seule partie à gérer un état, elle contient l'état souhaité de chaque applications. 
La base de données derrière est [etcd](https://etcd.io/docs/v3.5/quickstart/). 


__ATTENTION: elle ne sert pas de gestionnaire de sauvegarde__.


#### Les controleurs

Il y en a de plusieurs types. 
Disons simplement qu'ils sont les garants des fonctionnalités du master k8S. 
Un _controler manager_ les gère, chacun réalise une partie d'une tache (vérifier que les workers sont en bonne santé, par exemple). 

#### Le scheduler 

_Il attend de nouvelles taches venant de l'API Server_ et les assigne à des workers sains et pouvant gérer cette charge. 
Si aucun worker ne peut prendre une tache, la tache est en pending. 

#### Le cloud controller manager 

K8s peut tourner sur Azure, GCP ou AWS. 
Ce composant fait le pont avec les cloud publics utilisés par le cluster. 

### Les workers 

La responsabilité des workers est de faire tourner les applications client. 

* L'agent déployé sur le worker, le _kubelet_, attend les taches de l'API Server, les traite, et envoie régulièrement son statut à l'Api Server. 
* Un [démon _containerd_](https://containerd.io/) est déployé sur le worker en question. Il gère les instances Docker (pull, start, stop, etc) et est piloté par le kubelet. 
* Le _kube-proxy_ pour gérer le réseau. C'est notamment inclure les pods dans le réseau partagé 

### Rendre une application compatible avec K8S

Un _pod_ est une composant qui décore une application pour la faire tourner sur k8s en première approximation. 
Il a la charge de toute la gestion de son état, son déplacement, et en général de toute l'interface avec K8s. 

#### Zoom sur docker et les registries

En deux mots et selon la [documentation officielle](https://docs.docker.com/get-started/docker-concepts/the-basics/what-is-a-registry/), 
* une _image docker_ définit comment une application est packagée. Le produit fini est un fichier exécutable qui va décrire comment un container devrait le faire tourner
* un _container docker_ fait tourner une image dans un environnement contrôlé par Docker, au dessus de l'OS de la machine cible
* un _container registry_ stocke des images et peut les distribuer. Un _container repository_ gère toutes les versions d'une image donnée (redis, par exemple). Par exemple, un registry héberge les repositories frontproject et backend. Chacun embarque son _container registry_ avec, disons pour le premier, les images frontproject-v1.0 et frontproject-v2.0
* _DockerHub_ est le docker registry public de Docker. Il en existe d'autres, certains sont privés, ils peuvent être _self hosted_. 

En général, la procédure pour utiliser une image: 
1. Ecrire l'application et son Dockerfile 
2. Soit utiliser docker compose, le fameux `docker compose up`
3. Soit build image par image, avec par exemple un tag `docker build -t myapp:v1 .`
4. On met ensuite l'image à disposition dans le registry. Les syntaxes peuvent différer ([gitlab-ci](https://docs.gitlab.com/ee/user/packages/container_registry/build_and_push_images.html), [docker image push](https://docs.docker.com/reference/cli/docker/image/push/)


#### La démarche en général 

Imaginons donc une image conteneurisée, mise à disposition sur son registry. 
1. Un _container docker_ décore l'application et fournit ses dépendances
2. Un _pod_ décore le container docker pour que k8s puisse le faire tourner
3. Un _controller de déploiement_ décore le pod et ajoute les fonctionnalités de redimension (_scale_), gestion des erreurs, etc. Sa configuration est décrite dans un fichier yaml

### Le modèle déclaratif

On déclare un modèle qui décrit l'état attendu du cluster. 
En pratique, c'est un fichier yaml qui est soumis à l'api server, le plus souvent avec `kubectl`.
Pour passer de l'état observé (on dit aussi état courant) à celui attendu, k8s procède à une _réconciliation_. 
Le principe est le suivant: 
1. Le fichier est reçu par le serveur 
2. La demande est authentifiée et autorisée
3. Elle est sauvée dans le cluster store 
4. Si l'état voulu et celui constaté ne correspondent pas, un controleur lance la réconciliation. 
5. Ce controleur va éventuellement lancer ou supprimer des pods, récupérer des images docker du registry et lancer les conteneurs, les relier au réseau, commencer les processus docker. 


### Pods 

__Un pod est un environnement d'exécution partagé par un ou plusieurs conteneurs Docker.__

Un seul conteneur par pod se comprend bien.
Pourquoi en avoir plusieurs ? 
L'usage typique est d'embarquer l'application et un log scraper. 
En général, le principe est de mettre ensemble l'application principale et toute application qui n'est liée qu'à elle. 
Ils formeraient le plus petit tout cohérent qu'on déploie, toute la partie étant déployée ou pas. 

Son environnement d'exécution inclut ses volumes, la mémoire partagée, la couche réseau. 
Un pod avec une unique application a sa mémoire, son réseau, etc. 
Mais tous les conteneurs du même pod partagent la mémoire et le réseau. 
Par conséquent, on ne peut pas ajouter un conteneur dans un pod pour la mise à l'échelle. 
Un pod est un objet immutable. 
On ne manipule que des pods pour les mises à l'échelle. 

Quand un pod meurt, k8s le remplace. 
Attention, le nouveau pod a son propre id et sa propre ip. 
Au niveau de la conception, il faut réduire le couplage et gérer les plantages secs d'un pod. 

En résumé, toute application tourne dans un pod: 
1. Déployer se fait dans un pod
2. Arrêter une app est arrêter son pod 
3. _Scale up_ une application revient à ajouter des pods de cette application 
4. Et _scale down_ est en enlever, 
5. Enfin, remplacer une application, c'est déployer de nouveaux pods et tuer les anciens

### Les objets Services 

Les pods recréés ont des IP différentes. 
Un pod qui a une connection ouverte avec un autre peut la perdre s'il meurt. 
Les IP changent donc régulièrement, mais les groupes de pods utilisent les Kubernetes Services (S majuscule) qui vont gérer le réseau en offrant une connection stable. 
Basiquement, un Service (S majuscule) va afficher une IP utilisable et constante aux autres pods  et gérer les pods de son service. 
Ils proposent un réseau utilisable avec des IP à l'externe fixes, et en interne gèrent le load balancing. 


## Usage 

L'outil de base est la console avec la commande _kubectl_. 
Son principe général est: `kubectl [commande] [TYPE] [NOM] [flags]` avec: 
* les commandes possibles sont notamment: create, get, describe, delete
* les noms sont les objets k8s, par exemple: pods, nodes, services, deployments, jobs

Il utilise un répertoire de configuration, `.kube`, qui contient: 
* la définition des clusters qu'il permet de gérer. Chacun a un nom, un certificat, et un endpoint à atteindre pour l'API server 
* les droits utilisateurs des comptes qu'on utilise 
* les contextes, qui sont un cluster qu'on manipule avec un compte utilisateur 

On peut voir cette configuration avec la commande `kubectl config current-context`

## Sources

* [La doc officielle sur kubectl](https://kubernetes.io/fr/docs/reference/kubectl/overview/)
* [jesuisundev](https://www.jesuisundev.com/comprendre-kubernetes-en-5-minutes/)
* The Kubernetes book, second edition