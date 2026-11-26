# Récupérer la session en cours d’un groupe : adaptateur entrant REST

**Contexte**
Exposer la récupération de la session en cours par l’API REST en utilisant l’identifiant du groupe, puis convertir le résultat applicatif en réponse contenant les données métier requises. Cette issue dérive du domaine et ne définit pas de règle métier supplémentaire.

**Critères d'acceptation**
Feature: récupérer la session en cours d’un groupe (adaptateur entrant)

1. Exposer la session en cours et ses rounds dans la réponse REST
Ref Domain: D1, D2, D3, D4, D5, D6
Scenario: la requête de récupération renvoie l’état complet de la session courante du groupe
Given un identifiant de groupe avec une session en cours conforme aux invariants du domaine
When l’utilisateur demande la session en cours de ce groupe
Then l’adaptateur transmet l’identifiant du groupe au cas d’utilisation
And la réponse expose la session programmée correspondante et la liste des rounds terminés
And la réponse expose l’unique round en cours avec son numéro et sa liste de combats

2. Retourner 404 lorsqu’aucune session en cours n’est disponible
Ref Domain: D1
Scenario: le cas d’utilisation signale l’absence de session active pour le groupe demandé
Given l’identifiant d’un groupe inexistant ou sans occurrence active
When l’utilisateur demande la session en cours de ce groupe
Then l’API retourne le statut 404 avec un ProblemDetail

**Notes**
- La route et la forme détaillée de la réponse de succès ne sont pas spécifiées.
