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



## Sources

* [jesuisundev](https://www.jesuisundev.com/comprendre-kubernetes-en-5-minutes/)
