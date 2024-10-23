# Kafka 

Son cas d'utilisation typique est un problème de type producteur consommateur. 
En toute généralité, celui-ci se résoud comme suit:
* Des producteurs produisent une donnée sous forme de message
* des consommateurs utilisent cette donnée en lisant ces messages
* Ils sont découplés par une structure intermédiaire de type FIFO

Kafka comporte aussi plusieurs projets: 
* _Kafka Streaming_ pour transformer des data flow au dessus de Kafka. En plus de la logique de stream, on trouve un état partagé et des join distribués
* _Kafka Connect_ pour connecter d'autres systèmes à Kafka. Par exemple, on peut prendre un mysql en _source_, un fichier avro en _sink_, et réaliser des transformations entre. 
* _ksqldb_, le principe est d'avoir une requête qui s'exécute sur un flux de données

## Concepts 

### Messages, topics, serveurs 
Kafka gère des messages (aussi appelés _records_). 
Devant les volumes manipulés, Kafka est par essence un système distribué de gestion de messages. 
Tous portent la date exacte de création (_timestamp_), une valeur, et optionnellement une clé. 
On peut ajouter des en têtes. 
Par exemple, un message de log (la valeur) a pour clé l'hostname de sa source, et comme timestamp sa date de création. 
Pour organiser les messages, ceux de même thème, de même catégorie sont regroupés en _topics_. 
Un nom de topic est unique au sein d'un même cluster Kafka. 

Ces messages sont envoyés à des _brokers_ qui sont en fait le côté serveur de Kafka. 
Un broker n'est pas nécessairement un serveur physique, au sens d'une machine. 
On peut en effet (merci k8s) en déployer plusieurs sur le même hôte. 
Par contre, quand un client veut envoyer un message, il n'a pas à connaitre l'intégralité des brokers Kafka. 
Déjà parce que la liste peut évoluer, aussi parce que cette liste est potentiellement longue. 
Le client a juste besoin de se connecter à quelques serveurs connus et fixes, et ceux-ci vont traiter la demande. 
C'est ce qu'on appelle les _bootstrap servers_. 
Bien sûr, ils doivent être opérationnels quand on lance les commandes Kafka, par exemple pour créer les topics. 
Sous le capot, Kafka peut utiliser Zookeeper mais dispose d'une version plus récente sans, [KRaft](https://developer.confluent.io/learn/kraft/). 

Au final:
* __Le client envoie donc des messages organisés en topics aux brokers. __
* __Pour se connecter, il contacte des serveurs précis sans avoir à retenir toute la liste des brokers, ce sont les bootstrap servers.__


### Sémantique de livraison de messages 

Il y a plusieurs façons de délivrer des messages: 
1. Au plus une fois: le message est considéré livré dès qu'il est parti 
2. Au moins une fois: le message est considéré livré quand il est confirmé lu
3. Exactement une fois: peu importe s'il y a échec ou défaillance, on a la garantie qu'il est livré une seule fois

| Type | Duplication de messages | Perte de messages |
|------|-------------------------|-------------------|
| Au plus un | Non | Oui |
| Au moins un | Oui | Non |
| EXACTEMENT un | Non | Non |


Kafka garantit la livraison de messages _exactly once_ sous conditions qu'on va creuser ensuite.  

### Partition: leader et followers

A titre d'exemple, créer un topic ressemble à ça: 

```
bin/kafka-topics.sh --create 
--bootstrap-server localhost:9094 # les serveurs pour savoir où commencer
--topic error_logs # le nom du topic 
--partitions 3 # le nombre de partitions par topic. En général le nombre de brokers
--replication-factor 3 # le facteur de réplication de la partition pour gérer les erreurs
```

Et pour avoir le détail (on les voit tous avec `list`): 

```
bin/kafka-topics.sh --bootstrap-server localhost:9094 --describe --topic error_logs
``` 

Un topic est donc organisé en partitions.
Elles sont réparties sur les brokers avec pour chacune un leader qui gère la partition et des followers (ceux qui la redondent). 
Un _replica_ est une instance d'une copie d'une partition. 
Par exemple, une partition avec un facteur de réplication de N a donc N replicas. 
Comment les écrire ? 
En fait, Kafka Raft ou Zookeeper vont permettre d'élire un leader de replica. 
Celui ci va gérer la demande du client et écrire la donnée dans le replica leader. 
Les followers sont avertis par le leader, et vont mettre à jour leur replica depuis le replica leader.  

### Les offsets 

En interne, une partition est une structure de donnée suivant le concept de _commit logs_. 
Chaque partition est un log: 
* Il n'y a pas d'opération de changement: on ne peut qu'écrire à la fin du log. 
* La position d'un message au sein du log s'appelle son _offset_. 
* Son implémentation sur disque est extrêmement efficace, ce qui rend la structure de données très utile pour les bases de données et Kafka

Quand un consommateur se connecte à Kafka, il lit des messages des partitions. 
Pour garantir la sémantique du exactly once, Kafka retient à chaque fois l'offset de chaque partition lue par tel ou tel consommateur. 
Quand un consommateur dans un groupe plante, la répartition des offsets est recalculée, on parle de_rebalancing_. 


## A l'usage 

On va se concentrer sur l'API java. 

### Côté producteur 

Sur l'API Java, le principe est de fabriquer un `KafkaProducer` et d'envoyer des `ProducerRecord<K,V>`. 
Il faut que K et V soient sérialisables, de sorte qu'on construit un producteur avec de quoi sérialiser les clés et les valeurs. 
Il permet de: 
1. Envoyer des messages. En interne, Kafka commence par sérialiser le message, puis le _Partitioner_ calcule la partition cible (si elle n'est pas précisée). Le retour de `send` est un `Future<RecordMetadata>` pour savoir quel offset, quelle partition. Sinon, c'est une exception. __Donc, un Future veut bien dire que le send est par défaut asynchrone. Pour attendre le résultat, on utilise get sur le Future.__
2. Gérer des transactions de messages. On y reviendra, mais le principe est que le producteur écrit dans des partitions de manière transactionnelle. En fonction du niveau d'isolation de lecture des consommateurs, les offsets correspondant seront visibles uniquement si la transaction réussit.
3. Exposer des métriques de fonctionnement 

#### Utilisation de base 
Fondamentalement, [il s'utilise ainsi](https://kafka.apache.org/38/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html): 

```
 Properties props = new Properties();
 props.put("bootstrap.servers", "localhost:9092");
 props.put("transactional.id", "my-transactional-id");
 Producer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());

 producer.initTransactions();

 try {
     producer.beginTransaction();
     for (int i = 0; i < 100; i++)
         producer.send(new ProducerRecord<>("my-topic", Integer.toString(i), Integer.toString(i)));
     producer.commitTransaction();
 } catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {
     // We can't recover from these exceptions, so our only option is to close the producer and exit.
     producer.close();
 } catch (KafkaException e) {
     // For all other exceptions, just abort the transaction and try again.
     producer.abortTransaction();
 }
 producer.close();
```

#### Configuration 

Mentionnons les principaux éléments: 
* `client.id` pour identifier logiquement les messages de ce client 
* `acks` définit combien de partitions (0, 1, toutes) doivent valider l'écriture avant de considérer que l'opération est un succès
* les paramètres de retry et timeout pour l'envoi 
* la gestion de la mémoire et des batchs
* les sérialiseurs, on peut d'ailleurs implémenter le sien depuis l'interface `Serializer<K>`
* On peut ajouter des interceptors qui vont réagir à l'envoi de messages et au acks. Ils implémentent [une interface](https://kafka.apache.org/38/javadoc/org/apache/kafka/clients/producer/ProducerInterceptor.html) et le lien avec Kafka est déclaratif (dans la configuration).

#### Les bonnes idées 

On peut utiliser Avro pour sérialiser les valeurs des messages. On peut alors définir un _schema registry_ qui contient la définition des schémas. 
Le principe devient: 

```
props.put("key.serializer",
   "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("value.serializer",
   "io.confluent.kafka.serializers.KafkaAvroSerializer"); 
props.put("schema.registry.url", schemaUrl); 
```

On définit le schéma Avro, on fabrique un `GenericRecord` depuis ce schéma, et on envoie un `ProducerRecord<K, GenericRecord>`. 



### Côté consommateur 

#### Lien consommateur - partition 
Avoir un seul consommateur par topic peut ne pas suffire. 
On a alors besoin de mieux répartir la charge avec un groupe de consommateurs qui se partagent les partitions du topic. 
En général, on prend un nombre assez élevé de partitions pour pouvoir rajouter ensuite plus de consommateurs au besoin. 
En général:
* Si on lit N partitions avec un _consumer group_ de N consommateurs, chacun va prendre une partition et une seule
* Si par exemple 5 consommateurs lisent 4 partitions, l'un va rester inactif
* Si 2 consommateurs lisent 4 partitions, chacun va en lire deux simultanément et peuvent saturer (mais pas obligatoirement)
* Si deux consumer groups lisent le même topic, chaque groupe les lit à son rythme et Kafka gère les offsets pour chacun d'eux

#### Le rebalancing 
Si un consommateur plante ou quitte le groupe, si un admin Kafka ajoute des partitions, la symétrie partitions consommateurs est brisée, et il faut réallouer. 
On parle alors de _rebalancing_, et il y a deux solutions: 
1. _eager rebalancing_: tous les consommateurs arrêtent de consommer leurs partitions, Kafka les réassigne, les consommateurs consomment alors un autre ensemble de partitions. Mécaniquement, il y a un délai inhérent à l'arrêt total de la consommation
2. _cooperative rebalancing_: le but est de ne pas interrompre les consommateurs, on réalloue dynamiquement les partitions en essayant de minimiser les changements. Le consommateur reçoit l'ordre d'arrêter de lire telle partition, celle-ci est réallouée, et le nouveau consommateur reprend la lecture de la partition

#### L'id dans le groupe de consommateurs 

Un consommateur a un ID unique au sein de son groupe. 
Quand un consommateur quitte son groupe, cet ID se perd. 
Si ce consommateur rejoint de nouveau le groupe, il va lire d'autres partitions avec de nouveaux offsets. 
Et sur les partitions qu'il relit, il sera en retard sur les offsets de cette partition. 
On peut fixer un ID qui perdure même si le consommateur quitte le groupe et le retrouve après, c'est le `group.instance.id`. 
L'intérêt est clair si le consommateur gère de l'information sur ce qu'il consomme de son côté (état local). 
Dans ce cas là, il faut pouvoir mettre cet état à jour. 
Attention, quitter un groupe n'est pas tout de suite perçu, le cluster le détecte à cause d'un timeout de session. 
Si ce timeout est long, l'offset aura beaucoup bougé dans l'intervalle de temps. 

_En fait, cet id garantit que quand un consommateur perd sa connection au groupe, Kafka lui redonne les mêmes informations car il a toujours le même id._

#### Exemple concret: 

```
Properties props = new Properties();
props.put("bootstrap.servers", "broker1:9092,broker2:9092");
props.put("group.id", "CountryCounter");
props.put("key.deserializer",
    "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer",
    "org.apache.kafka.common.serialization.StringDeserializer");

KafkaConsumer<String, String> consumer =
    new KafkaConsumer<String, String>(props);
	
consumer.subscribe(Collections.singletonList("customerCountries"));
``` 