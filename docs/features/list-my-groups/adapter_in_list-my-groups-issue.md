# Consulter mes groupes : impact adapter entrant REST

**Contexte**
Exposer le cas d'usage de consultation des groupes par une requête REST et convertir le résultat applicatif en réponse HTTP. Cette issue dérive de [domain_list-my-groups-issue.md](./domain_list-my-groups-issue.md) ; elle n'ajoute aucune règle métier.

**Critères d'acceptation**
Feature: Consulter mes groupes (adapter REST entrant)

1. Mapping de la requête et de la réponse contenant les groupes
Ref Domain: D1, D2, D3
Scenario: La requête REST retourne les groupes et les champs de leur résumé
Given une requête GET sur /api/v1/groups avec l'identifiant d'un utilisateur associé à des groupes comme membre ou admin
When l'adapter REST invoque le cas d'usage de consultation
Then la réponse contient chaque groupe associé à cet utilisateur
And chaque entrée contient le nom, la description, le nombre de membres, le nombre de sessions programmées et l'indicateur admin de cet utilisateur

2. Mapping d'un résultat vide
Ref Domain: D4
Scenario: La requête REST retourne une collection vide sans groupe associé
Given une requête GET sur /api/v1/groups avec l'identifiant d'un utilisateur sans groupe associé
When l'adapter REST invoque le cas d'usage de consultation
Then la réponse contient une collection vide

**Notes**
- Le contrat proposé reçoit l'identifiant de l'utilisateur en paramètre de requête ; le mécanisme d'authentification n'est pas spécifié dans cette fonctionnalité.
