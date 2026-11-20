# Clôturer un round : adaptateur sortant

**Contexte**
Conserver le statut et les résultats du round clôturé.

**Critères d'acceptation**
Feature: Persister un round clôturé

1. Conserver le statut et les résultats des combats
Ref Domain: D1, D2
Scenario: Recharger un round clôturé
Given un round clôturé contenant des combats avec ou sans vainqueur
When le round est sauvegardé puis rechargé
Then son statut CLOSED et les vainqueurs de ses combats sont conservés

**Notes**
- Les règles métier sont définies dans l'issue domaine.
