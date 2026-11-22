# Consulter mes groupes : impact adapter sortant de persistance

**Contexte**
Implémenter la lecture des groupes liés à un utilisateur et fournir les valeurs nécessaires à leur résumé. Cette issue dérive de [domain_list-my-groups-issue.md](./domain_list-my-groups-issue.md) ; elle n'ajoute aucune règle métier.

**Critères d'acceptation**
Feature: Consulter mes groupes (adapter sortant de persistance)

1. Recherche et projection des groupes associés
Ref Domain: D1, D2, D3
Scenario: La lecture retrouve les groupes par appartenance et fournit les données de résumé
Given des groupes persistés où l'utilisateur apparaît avec le rôle membre ou admin
When l'adapter de persistance recherche les groupes associés à cet utilisateur
Then il retourne les groupes où l'utilisateur apparaît avec l'un ou l'autre de ces rôles
And les données retournées permettent de calculer le nombre de personnes du groupe, admin compris
And les données retournées permettent de calculer le nombre de sessions programmées
And elles permettent d'indiquer si l'utilisateur a le rôle admin

2. Aucun groupe correspondant
Ref Domain: D4
Scenario: La recherche ne trouve aucun groupe associé
Given aucun groupe persisté ne contient l'utilisateur comme membre ou admin
When l'adapter de persistance recherche les groupes associés à cet utilisateur
Then il retourne une collection vide

**Notes**
- Les erreurs techniques de stockage et leur traduction ne sont pas spécifiées par la fonctionnalité et restent hors périmètre.
