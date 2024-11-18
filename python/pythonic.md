Le but de ce texte est de réaliser du code Python en utilisant les pleines capacités du langage. 

* Copyright zefrenchwan, 2024
* License MIT 
* Pas de partenariat

# Exécution de code 


## Les environnements virtuels 
Avoir un seul environnement de python est compliqué ne serait ce que parce qu'on peut avoir besoin de dépendances différentes suivant les projets. 
En inclure une nouvelle peut casser d'autres dépendances ou être incompatibles. 
On utilise alors les environnements virtuels avec `venv`, pour avoir un environnement par projet. 

```
# créer un environnement dans path 
python -m venv path 
# aller dans path et lancer l'environnement virtuel 
cd path 
./bin/activate # linux  
.\Scripts\activate.bat # Windows 
```


Une fois l'environnement virtuel activé, l'installation de package se base sur `pip`. 
Certains projets utilisent pip en interne mais l'enrichissent: 
* poetry 
* pipenv 

## Utilisation interactive de Python 

L'idée est d'utiliser un outil interactif suivant une boucle REPL (read, eval, print, loop). 
Bien sûr, il y a l'interpréteur de base, mais aussi: 
* bpython: l'interpréteur de base amélioré avec la librairie curses, et d'autres features. Il reste un outil en ligne de commande 
* ptpython: une version en ligne de commande aussi, avec gestion de la souris, ressemblant à jupyter pour son mode d'utilisation 
* ipython: en ligne de commande, mais offre une meilleure gestion des packages et du calcul distribué
* jupyter: version web d'ipython. Astuce: utiliser ? pour avoir de l'information sur un module, une fonction, etc


Pour utiliser jupyter depuis l'environnement virtuel: 

```
pip install --upgrade jupyterlab
# puis on le lance 
python -m jupyter lab # qui va lancer le navigateur
```



# Sources

* Mastering Python - Second Edition. Rick van Hattem, 2022