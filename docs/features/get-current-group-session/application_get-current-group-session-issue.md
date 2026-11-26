# Récupérer la session en cours d’un groupe : application

**Contexte**
Mettre en œuvre le cas d’utilisation qui charge la session en cours à partir de l’identifiant du groupe et retourne son état métier : session programmée associée, rounds terminés et round en cours. Cette issue dérive des invariants du domaine et n’ajoute aucune règle métier.

**Critères d'acceptation**
Feature: récupérer la session en cours d’un groupe (couche applicative)

1. Orchestrer la récupération de la session en cours complète par groupe
Ref Domain: D1, D2, D3, D4, D5, D6
Scenario: le cas d’utilisation retourne la session courante et ses données métier
Given un identifiant de groupe dont la session en cours respecte les invariants du domaine
When le cas d’utilisation de récupération est exécuté avec cet identifiant
Then il retourne la session en cours unique du groupe et sa session programmée correspondante
And le résultat contient la liste des rounds terminés et l’unique round en cours
And le round en cours est retourné avec son numéro et sa liste de combats

2. Signaler l’absence de session en cours
Ref Domain: D1
Scenario: aucune occurrence active n’est chargée pour le groupe
Given le port de chargement ne retourne aucun groupe avec occurrence active pour l’identifiant demandé
When le cas d’utilisation de récupération est exécuté avec cet identifiant
Then il signale que la session en cours du groupe est introuvable par une erreur applicative dédiée

**Notes**
- Le port peut retourner un résultat vide si le groupe est absent ou s’il n’a pas d’occurrence active; ces cas sont exposés par la même erreur applicative.
