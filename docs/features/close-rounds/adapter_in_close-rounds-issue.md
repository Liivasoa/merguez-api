# Clôturer un round : adaptateur entrant

**Contexte**
Permettre la clôture d'un round par l'API du groupe.

**Critères d'acceptation**
Feature: Transmettre la clôture d'un round

1. Transmettre les résultats et retourner le round
Ref Domain: D1, D2
Scenario: Envoyer une demande de clôture
Given une demande identifiant un round et les résultats de ses combats
When l'API reçoit la demande de clôture
Then elle transmet les résultats au cas d'utilisation
And elle retourne le statut et les combats du round clôturé

2. Exposer le refus de clôture
Ref Domain: D3
Scenario: Demander la clôture d'un round déjà clôturé
Given une demande de clôture d'un round déjà clôturé
When le cas d'utilisation refuse la demande
Then l'API retourne un refus sans exposer de détails internes

**Notes**
- Les règles métier sont définies dans l'issue domaine.
