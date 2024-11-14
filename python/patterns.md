Le but de ce texte est de réaliser du code Python en utilisant les pleines capacités du langage. 

* Copyright zefrenchwan, 2024
* MIT License
* Rien de publicitaire ou de contractuel / Pas de conflit d'intérêt

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



# Les principes de base 

## Encapsulation, héritage et injection de dépendances 

Quatre principes sont particulièrement importants: 
1. l'encapsulation de ce qui peut changer 
2. la composition plus que l'héritage 
3. les interfaces comme paramètres 
4. Le couplage léger (_loose coupling_)

### Encapsulation 

Plusieurs solutions: 
1. L'héritage avec une classe abstraite qui contient une version générique de code comme contrat. Chaque implémentation inclut ses spécificités 
2. Les champs d'un objet sont traités avec des getters / setters

Sur le second point, le principe est de mettre les calculs de validation dans le `@attr_name.settter`: 

```
class Person:
    def __init__(self, name):
        self._name = " ".join([v for v in name.split(" ") if len(v) != 0 ])
    @property
    def name(self):
        return self._name
    @name.setter
    def name(val):
		if len(val) == 0:
			raise ValueError("expecting at least one char")
        self._name = " ".join([v for v in val.split(" ") if len(v) != 0 ])
```

### Composition plus que l'héritage

Le problème est de changer une signature dans un graphe d'héritage, surtout sur les classes parent. 
Pour réduire ce couplage, le principe est de développer des méthodes séparées, chacune avec sa signature, en utilisant la composition. 
Dans le cas ci dessous, on n'utilise pas `param` dans la classe Child. 
Avec l'héritage, on l'aurait dans la signature de l'action. 
Sans, on peut s'adapter et garder action sans paramètre. 

```
class Parent: 
	def __init__(self):
		...
	def action(self, param):
		print(f"Hello {param}")
		
class Child:
	def __init__(self):
		...
	def action(self):
		print("Hello")
```

### Utiliser les interfaces plus que l'implémentation 

Alors, la notion d'interface elle même pose question: 
* En Java, une classe implémente une interface avec un lien d'héritage 
* En Scala avec les traits, en Golang avec l'interface, on utilise le __structural duck typing__ dont voici le principe: si une structure a les mêmes méthodes qu'une interface (sans lien d'héritage, et pour cause!), elle "implémente de fait" cette interface 



Python permet de faire les deux: 
* déclarer une relation d'héritage en utilisant les _abstract base class_ (abc). Le principe est d'avoir une définition par interface en étendant ABC avec les méthodes annotées avec `@abstractmethod`
* déclarer une duck typed interface via les _protocol_ avec  une déclaration de protocol


```
from typing import Protocol

class A(Protocol): 
	def action(self):
		... # vraiment, on écrit ...
		
# pas de lien d'héritage 
class B:
	def action(self):
		print("test")

# et donc on peut utiliser une instance de B 
def run(a: A):
	a.action()
``` 


Alors, pourquoi ? 
Si on revient aux fondamentaux de python, le type déclaré est un _hint_, alors que le type implémenté est celui qu'on utilise effectivement. 
On trouve ces interfaces dans les déclarations de méthode ou de types (en hint). 
On peut d'ailleurs effectuer une analyse statique de code, par exemple avec [mypy](https://github.com/python/mypy). 


### Le couplage léger 

La solution est l'injection de dépendance. 
C'est à dire qu'une classe ne va pas créer d'instance d'autre classe (sauf classes de bases ou utilitaires), mais elle recevra cette instance. 
Par exemple, un traitement métier recevra à sa création une instance de la classe qui gère le stockage, sans la créer lui même. 


## Les principes SOLID 

| Nom du principe | Description |
|----------------|---------------------------|
| Single responsibility | Une classe = une responsabilité |
| Open Closed | On n'ajoute pas de fonctionnalité  nouvelle |
| Liskov Substitution | Une sous classe réalise aussi un bon traitement |
| interface Segregation | Pas besoin d'implémenter d'interface inutile |
| Dependency injection | Casser le couplage haut niveau - bas niveau |


Pour le dernier point: 
```
from abc import ABC, abstractmethod 
from typing import Protocol

# better than interface, because of implicit duck typing 
class Saver(Protocol):
    def save(self, content: str):...

# crappy implementation 
class FileSaver:
    def __init__(self, path: str):
        self.path = path
    def save(self, content:str):
        with open(self.path, "w") as f:
            f.write(self.content)

# High level logic 
class BusinessLayer: 
    def __init__(self, dao: Saver):
        self.dao = dao 
    def register_user(self, username):
		# makes no sense, but for demo purpose 
        self.dao.save(username)
		
if __name__ == '__main__':
	# core dependency injection 
    f = FileSaver("/tmp/popo.txt")
    b = BusinessLayer(f)
    ...
```

# Sources

* Mastering Python - Second Edition. Rick van Hattem, 2022
* Mastering Python Design Patterns, 3ed. Kamon Ayeva, Sakis Kasampalis, 2024
