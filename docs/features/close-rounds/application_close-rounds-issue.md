# Clôturer un round : application

**Contexte**
Coordonner la clôture du round et la sauvegarde de ses résultats.

**Critères d'acceptation**
Feature: Coordonner la clôture d'un round

1. Transmettre et sauvegarder le résultat
Ref Domain: D1, D2
Scenario: Clôturer et sauvegarder un round
Given un round en cours et les résultats de ses combats
When le cas d'utilisation de clôture est exécuté
Then il applique les résultats au round et sauvegarde le round clôturé
And il retourne le round clôturé

2. Transmettre le refus du domaine
Ref Domain: D3
Scenario: Refuser la clôture d'un round déjà clôturé
Given un round déjà clôturé
When le cas d'utilisation de clôture est exécuté
Then il transmet le refus du domaine sans sauvegarder le round

**Notes**
- Les règles métier sont définies dans l'issue domaine.
