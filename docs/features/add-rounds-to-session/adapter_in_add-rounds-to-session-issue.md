# Ajouter des rounds à une séance : impact sur l’adaptateur entrant

**Contexte**
Exposer l’ajout d’un round et de combats à une séance par l’adaptateur REST entrant, convertir les données de la requête en commande applicative et transmettre le résultat du cas d’utilisation. Cette issue découle de [domain_add-rounds-to-session-issue.md](./domain_add-rounds-to-session-issue.md) et ne définit aucune règle métier supplémentaire.

**Critères d'acceptation**
Feature: ajouter des rounds à une séance en cours (adaptateur entrant)

1. Transmettre une demande valide de création de round
Ref Domain: D1, D3, D4, D5, D9
Scenario: l’adaptateur transmet les combats initiaux et expose le round créé
Given une requête REST de création d’un round contenant au moins un combat de deux membres
When l’adaptateur entrant traite la requête
Then il transmet à l’application les identifiants de la séance et des membres ainsi que le gagnant éventuel
And il expose le résultat du cas d’utilisation, notamment le numéro du round attribué et ses combats

2. Transmettre une demande d’ajout de combat à un round
Ref Domain: D6, D7, D8
Scenario: l’adaptateur transmet un combat supplémentaire et expose le résultat applicatif
Given une requête REST d’ajout d’un combat à un round existant
When l’adaptateur entrant traite la requête
Then il transmet l’identifiant du round et les membres du combat à l’application
And il expose soit le résultat du round mis à jour, soit le refus renvoyé par l’application

3. Exposer le refus d’une demande invalide
Ref Domain: D2, D4, D5, D6, D8
Scenario: une séance ou une composition de combat refusée ne produit pas de réponse de succès
Given une demande que le domaine refuse parce que la séance n’est pas en cours ou que les invariants du round ne sont pas respectés
When l’adaptateur entrant reçoit le refus du cas d’utilisation
Then il expose ce refus à l’appelant
And il ne retourne pas un résultat de création ou de mise à jour réussi

**Notes**
- La route, la structure détaillée des requêtes et réponses ainsi que les codes HTTP ne sont pas spécifiés.
