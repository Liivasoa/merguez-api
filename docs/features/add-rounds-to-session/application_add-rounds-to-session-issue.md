# Ajouter des rounds à une séance : impact sur le module applicatif

**Contexte**
Mettre en œuvre le cas d’utilisation d’ajout de rounds à une occurrence de séance en cours. Le cas d’utilisation orchestre l’accès à la séance, délègue les invariants au domaine et persiste le résultat. Cette issue découle de [domain_add-rounds-to-session-issue.md](./domain_add-rounds-to-session-issue.md) et n’ajoute pas de règle métier.

**Critères d'acceptation**
Feature: ajouter des rounds à une séance en cours (couche applicative)

1. Orchestrer la création d’un round et la persistance de son premier combat
Ref Domain: D1, D3, D4, D5, D9, D11
Scenario: le cas d’utilisation crée et enregistre un round initial valide
Given une commande d’ajout de round à une séance en cours avec au moins un combat valide et sans round en cours dans le groupe
When le cas d’utilisation est exécuté
Then il invoque le domaine pour créer le round et lui attribuer son numéro
And il demande la persistance du round, de son statut et de ses combats, avec le gagnant éventuel
And il retourne le round créé

2. Orchestrer l’ajout de combats au round et respecter les invariants du domaine
Ref Domain: D6, D7, D8
Scenario: le cas d’utilisation ajoute les combats disponibles et propage un refus métier
Given un round existant et une demande d’ajout d’un combat avec deux membres non assignés
When le cas d’utilisation traite la demande
Then il demande au domaine d’ajouter le combat au round
And il persiste le round mis à jour si l’ajout est valide
And si le domaine refuse l’ajout, il ne persiste pas un résultat de succès

3. Ne pas persister un round refusé par le domaine
Ref Domain: D2, D4, D5, D10
Scenario: les demandes incompatibles avec les invariants ne créent pas de round
Given une demande de création de round pour une séance qui n’est pas en cours, contenant un round initial invalide ou appartenant à un groupe ayant déjà un round en cours
When le cas d’utilisation exécute la demande et que le domaine la refuse
Then le refus du domaine est propagé
And aucun round n’est persisté pour cette demande

**Notes**
- La forme exacte des commandes, résultats et exceptions applicatives n’est pas spécifiée.
