Ce texte porte sur la mise en place des design patterns appliqués à Python. 
Le but est d'écrire du code le plus proche du style python dans les problématiques courantes. 


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

# Les patterns du Gang of four 

On a trois types de pattern: 

| Nom | Description |
|-------------|--------------------|
| Creational | Comment créer des objets |

## Patterns de création d'objets 

Le principe de _factory_ est de cacher les détails de création d'objets avec une méthode centrale qui va gérer les cas. 
L'usage typique est d'avoir des classes différentes en fonction de certains paramètres, tous implémentant la même interface. 


Le pattern _builder_ consiste à avoir construire un objet au fur et à mesure. 
L'objet déclare quand il est fini, on applique des builders jusqu'à la fin. 
Par exemple: 


```
from typing import Protocol

# what to build 
class House: 
    def __init__(self):
        self._counter = 0
        self._name = "House of John Doe"
    @property
    def built(self):
        return self._counter == 3
    def incr(self):
        self._counter = self._counter + 1
    @property
    def name(self) -> str:
        return self._name
		
# builders 
class Builder(Protocol):
    def build(self, house: House):...

class RoofBuilder:
    def __init__(self):
        pass
    def build(self, house: House):
        print(f"build roof in {house.name}")
        house.incr()

class WallsBuilder:
    def __init__(self, number):
        self._number = number 
    def build(self, house: House):
        print(f"building {self._number} walls in {house.name}")
        house.incr()

class WindowsBuilder:
    def __init__(self):
        pass
    def build(self, house: House):
        print(f"Adding windows in {house.name}")
        house.incr()
		
# action part
steps = [RoofBuilder(), WallsBuilder(5), WindowsBuilder()]
house = House()
while not house.built:
    steps.pop().build(house)
```


Le pattern _prototype_ permet de cloner un objet. 
Le but est d'avoir une copie qu'on peut modifier, tout en gardant l'original en mémoire. 
Pour cela, on peut gérer un registry qui va garder les valeurs par un identifiant métier. 
Ce registry va alors cloner la valeur de base (le prototype), et ensuite l'améliorer. 
L'implémentation en python est assez souple puisqu'elle permet d'ajouter des attributs simplement. 

```
# ease cloning 
from copy import deepcopy

# basic object with attributes
class Metadata:
    def __init__(self, name:str, **kwargs):
		# clever one: kw args to enrich structure, no map needed 
        self.name = name.strip() 
        for key in kwargs:
            setattr(self, key,kwargs[key])
            
    def __str__(self):
		# dynamicaly find attrs and group them all
        values = []
		# note that vars is the map of attrs 
        for attr,val in sorted(vars(self).items()):
            values.append(f'{attr}: "{str(val)}"')
        return "Metadata { " + " , ".join(values) + "}"
		
m = Metadata("tables", db = "db", schema = "schema", table="t_data")
print(m)

# registry 
# implementation is a fast one, missing unregister and more cases (not found, etc)
class MDRegistry:
    def __init__(self):
        self._registry = {}
    def register(self, md: Metadata):
        self._registry[md.name] = md
    def clone(self, name: str, **kwargs):
        base = deepcopy(self._registry[name])
        for key in kwargs:
            setattr(base, key, kwargs[key])
        return base 


# use of registry 
base = MDRegistry()
base.register(Metadata("database"))
instance = base.clone("database", db="db")
print(instance)
```


Le pattern singleton consiste à fournir une unique instance d'une classe. 
La façon de le tester en Python passe par `is`, qui renvoie true si les deux objets sont à la même adresse mémoire (donc sont les mêmes). 
Le test est donc que deux appels de la fonction de singleton soient toujours le même au sens de _is_. 
Il y a plusieurs implémentations: 
* la version par variable globale, unique, déclarée, et on teste si elle est None (auquel cas on la set) ou pas. Dans tous les cas, on retourne cette valeur. 
* la version par métaclasse, plus complexe à mettre en oeuvre, mais qui cache complètement le singleton. 


Avant de la présenter, voici un exemple introductif:

```
class ControlingType(type):
    def __call__(cls, *args,**kwargs):
        print("Allocation")
        super(ControlingType, cls).__call__(*args, **kwargs)
		
class ConcreteType(metaclass = ControlingType):
    def __init__(self):
        pass
    def action():
        print("Hello")
		
p = ConcreteType()
# va afficher allocation sur la sortie standard
```

On peut d'ailleurs réutiliser ce gestionnaire sur plusieurs classes. 
L'usage pour singleton consiste à avoir un dictionnaire des instances créées, avec comme clé la classe. 
Quand on fait un appel à `__call__`, le paramètre _cls_ va être la clé à chercher dans le dictionnaire des instances. 

```
class ControlingType(type):
    _instances = dict()
    def __call__(cls, *args,**kwargs):
		# note the cls._instances
        val = cls._instances.get(cls)
        if val is not None:
            return val
        else:
            res = super(ControlingType, cls).__call__(*args, **kwargs)
            cls._instances[cls] = res
            return res
			
class ConcreteType(metaclass = ControlingType):
    def __init__(self):
        pass
    def action():
        print("Hello")
		
# and then
base = ConcreteType()
for i in range(10):
    current = ConcreteType()
    if current is not base:
        raise ValueError("failed")
    else:
        print("same")
```




# Sources

* Mastering Python Design Patterns, 3ed. Kamon Ayeva, Sakis Kasampalis, 2024
