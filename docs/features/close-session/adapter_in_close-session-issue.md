# Clôturer une séance : adaptateur entrant

**Contexte**
Permettre à un admin ou à un membre de demander la clôture d'une séance par l'API du groupe.

**Critères d'acceptation**
Feature: Transmettre la clôture d'une séance

1. Transmettre une demande de clôture
Ref Domain: D1, D2, D5, D6, D7
Scenario: Recevoir une demande de clôture
Given une demande de clôture identifiant le groupe et la séance
When l'API reçoit la demande
Then elle transmet les informations nécessaires au cas d'utilisation
And elle retourne le résultat de la clôture

2. Exposer un refus de clôture
Ref Domain: D3, D4
Scenario: Recevoir une demande refusée par le domaine
Given une demande de clôture d'une séance qui n'est pas en cours ou ne contient aucun round
When le cas d'utilisation refuse la clôture
Then l'API retourne un refus sans exposer de détails internes

**Notes**
- Les règles métier sont définies dans l'issue domaine.
- Le contrat HTTP précis n'est pas défini par la demande.
