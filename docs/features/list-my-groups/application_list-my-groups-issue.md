# Consulter mes groupes : impact module Application

**Contexte**
Fournir un cas d'usage de lecture qui récupère les groupes associés à un utilisateur et retourne, pour chacun, les informations résumées demandées. Cette issue dérive de [domain_list-my-groups-issue.md](./domain_list-my-groups-issue.md) et n'introduit pas de règle métier supplémentaire.

**Critères d'acceptation**
Feature: Consulter mes groupes (couche application)

1. Retour des résumés des groupes associés à l'utilisateur
Ref Domain: D1, D2, D3
Scenario: Le cas d'usage retourne les résumés des groupes où l'utilisateur est membre ou admin
Given une demande de consultation contenant l'identifiant d'un utilisateur appartenant à des groupes comme membre ou admin
When le cas d'usage de consultation des groupes est exécuté
Then il demande les groupes associés à cet utilisateur au port de lecture
And le résultat contient pour chaque groupe son nom, sa description, le nombre de membres, le nombre de sessions programmées et si l'utilisateur est admin

2. Retour d'une liste vide
Ref Domain: D4
Scenario: Le cas d'usage retourne une liste vide lorsqu'aucun groupe n'est associé à l'utilisateur
Given une demande de consultation contenant l'identifiant d'un utilisateur sans groupe associé
When le cas d'usage de consultation des groupes est exécuté
Then le résultat est une liste vide

**Notes**
- L'identifiant de l'utilisateur est fourni au cas d'usage par son appelant ; l'authentification et la détermination de l'utilisateur courant sont hors périmètre de cette fonctionnalité.
