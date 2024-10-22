# Cloud français

Le but de ce document est d'établir un panorama des cloud français dans une optique professionnelle. 

## Panorama des clouds et risques 

### Les types de cloud

Le résumé du cloud en une punchline serait: _louer des machines à des prestataires_. 
On peut découper les offres en distinguant: 
1. __IAAS (infrastructure aas)__: le prestataire gère les machines, on gère le système déployé, la sécurisation, l'environnement, les applications, et les données. Cas particulier, le CAAS (container aas): on ne gère pas des VM mais des conteneurs. Les entreprises louent des machines qu'elles ne pourraient pas se payer, cas typique. 
2. __PAAS (Platform aas)__: le prestataire gère tout le socle, on ne gère que les applications et les données nécessaires au fonctionnement de l'application. Par exemple, la partie cloud run de GCP. 
3. __SAAS (software aas)__: on utilise une application déjà déployée et qui a déjà ses propres données. On utilise un produit déjà initialisé et configuré. Par exemple, une boite mail distance, une suite bureautique en ligne


### L'état du droit 
Un peu d'histoire: 
1. En 2008, adoption du _FISA act_ aux USA qui se donne droit de collecter les données de citoyens non américains si elles passent par une entreprise américaine
2. 2018: Adoption de la _RGPD_ au niveau européen
3. Réponse des USA dans la foulée (2018): le _cloud act_ qui définit des conditions de non respect de la RGPD pour les entreprises américaines oeuvrant en Europe
4. 2023: _Data act_ au niveau européen: lutter contre la main mise des GAFAM en clarifiant la valorisation de la donnée et en permettant un passage sans coût d'un fournisseur de cloud à un autre
5. 2024: _DSA_ au niveau européen: meilleure lutte contre la haine en ligne, la désinformation, les contenus illicites. Plus de transparence, meilleur contrôle des processus électoraux. Finalement, "ce qui est illégal hors ligne est illégal en ligne" 

### La doctrine française 

D'après le gouvernement, il faut un cloud indépendant et sans risque vis a vis de l'usage des données. 
Le plan s'articule autour de 3 axes: 
1. Développer un label _cloud de confiance_: l'ANSSI valide le visa de sécurité Secnumcloud. Les exigences sont l'interopérabilité avec les clouds européens, l'infrastructure en Europe et le respect d'un référentiel technique
2. Accélérer l'usage du cloud au sein du service public en faisant du cloud le choix par défaut des nouveaux projets, et en imposant soit un cloud certifié SecNumCloud, soit un cloud interne au service public (pi géré par le ministère de l'intérieur, numo par la DGFIP soit les finances publiques)
3. Soutenir et développer les projets technologiques français. 

### L'état de l'offre en France 

La liste est longue avec des produits et entreprises méconnus du grand public. 
Sans exhaustivité, on peut déjà citer:
* [Orange](https://cloud.orange-business.com/offres/infrastructure-iaas/)
* [Oodrive](https://www.oodrive.com/fr/)
* [NumSpot](https://www.lemondeinformatique.fr/actualites/lire-cloud-de-confiance-numspot-lancera-ses-offres-mi-2024-92300.html)
* [OVHCloud](https://www.ovhcloud.com/fr/) 
* [Outscale](https://fr.wikipedia.org/wiki/Outscale) est un fournisseur de cloud de type IAAS
* [CloudTemple](https://www.cloud-temple.com/qui-sommes-nous/)

Il est important de noter le rapprochement d'acteurs français avec des acteurs américains:
* [AWS et Atos](https://atos.net/fr/2022/communiques-de-presse_2022_11_30/aws-et-atos-renforcent-leur-collaboration-grace-a-un-nouveau-partenariat-strategique-pour-transformer-lindustrie-des-infrastructures-informatiques-managees)
* [Thales et Google](https://www.s3ns.io/pourquoi-s3ns)
* [Azure, Orange et CapGemini](https://www.capgemini.com/fr-fr/actualites/communiques-de-presse/capgemini-et-orange-annoncent-le-projet-de-creer-bleu-une-societe-qui-fournira-un-cloud-de-confiance-en-france/)

## Source: 

### Les grandes lignes du droit 

* [oodrive](https://www.oodrive.com/fr/blog/reglementation/cloud-act-fisa-4mn-pour-comprendre-dou-vient-la-menace/)
* [Data act selon le gouvernement](https://www.entreprises.gouv.fr/fr/actualites/adoption-du-data-act-au-conseil-de-l-union-europeenne)
* [DSA selon vie publique](https://www.vie-publique.fr/eclairage/285115-dsa-le-reglement-sur-les-services-numeriques-ou-digital-services-act)


### Les types de cloud 

* https://cloud.google.com/learn/paas-vs-iaas-vs-saas?hl=fr
* https://cloud.google.com/learn/what-is-iaas
* https://cloud.orange-business.com/paroles-dexperts-fr/types-de-cloud/

### La doctrine cloud en France 

* https://www.economie.gouv.fr/securite-performance-souverainete-strategie-cloud
* https://www.numerique.gouv.fr/services/cloud/doctrine/

### Les partenariats de cloud 
* https://www.lemondeinformatique.fr/actualites/lire-cloud-de-confiance-numspot-lancera-ses-offres-mi-2024-92300.html
* https://siecledigital.fr/2022/10/18/aws-et-atos-sassocient-pour-decrocher-le-label-cloud-de-confiance/
* https://siecledigital.fr/2022/06/23/le-cloud-de-confiance-de-capgemini-et-orange-verra-le-jour-en-2024/
* https://www.thalesgroup.com/fr/marches/defense-et-securite/activites-services-numeriques/google-cloud-provider