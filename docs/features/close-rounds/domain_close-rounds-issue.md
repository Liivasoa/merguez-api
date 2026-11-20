# Clôturer un round : domaine

**Contexte**
Un round d'une séance contient des combats dont chacun peut avoir un vainqueur.

**Critères d'acceptation**
Feature: Clôturer un round

1. D1 - Clôturer un round avec des vainqueurs
Scenario: Clôturer un round avec plusieurs résultats
Given un round en cours avec plusieurs combats
When le round est clôturé avec un vainqueur pour chaque combat
Then son statut devient CLOSED
And chaque combat possède le vainqueur indiqué

2. D2 - Clôturer un round sans vainqueur pour certains combats
Scenario: Clôturer un round avec des combats sans vainqueur
Given un round en cours avec plusieurs combats
When le round est clôturé avec un vainqueur pour certains combats seulement
Then son statut devient CLOSED
And les autres combats n'ont pas de vainqueur

3. D3 - Refuser la clôture d'un round déjà clôturé
Scenario: Clôturer un round fermé une seconde fois
Given un round clôturé
When une nouvelle clôture est demandée
Then la demande est refusée
And les résultats du round restent inchangés

**Notes**
- Couverture : statut CLOSED : D1, D2, D3 ; vainqueur facultatif : D1, D2.
- Le vainqueur d'un combat doit déjà être l'un de ses participants.
- Les résultats complets remplacent les vainqueurs précédemment enregistrés, y compris par l'absence de vainqueur.
- Les modalités de clôture d'une séance sont hors périmètre.
