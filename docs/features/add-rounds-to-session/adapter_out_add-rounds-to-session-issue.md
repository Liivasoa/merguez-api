# Ajouter des rounds à une séance : impact sur l’adaptateur sortant

**Contexte**
Persister les rounds d’une séance en cours et leurs combats afin de conserver leur numéro, leur statut, leurs membres et le gagnant éventuel, puis restituer les données nécessaires aux opérations suivantes. Cette issue découle de [domain_add-rounds-to-session-issue.md](./domain_add-rounds-to-session-issue.md) et ne définit aucune règle métier supplémentaire.

**Critères d'acceptation**
Feature: ajouter des rounds à une séance en cours (adaptateur sortant)

1. Persister et restituer les rounds et leurs combats
Ref Domain: D1, D3, D4, D5, D9, D10
Scenario: un round et ses combats sont conservés avec leurs informations métier
Given un round valide associé à une séance en cours
When l’adaptateur de persistance enregistre le round
Then il conserve son association à la séance, son numéro attribué et son statut
And il conserve chaque combat avec exactement les deux membres concernés
And il conserve le gagnant lorsqu’il est renseigné, ou son absence
And il peut restituer le round avec ces informations

2. Restituer les rounds et affectations existants pour poursuivre la séance
Ref Domain: D3, D6, D7, D8, D10, D11
Scenario: les données persistées permettent de connaître la prochaine numérotation et les membres déjà assignés
Given une séance avec des rounds et des combats déjà persistés
When l’application charge la séance et le round concerné
Then l’adaptateur restitue les numéros et statuts existants ainsi que les affectations des membres aux combats
And ces données permettent au domaine de calculer le prochain numéro et de vérifier les membres encore disponibles
And elles permettent au domaine de déterminer si le groupe a déjà un round en cours

**Notes**
- Le modèle de stockage exact des rounds et des combats n’est pas imposé par cette issue.
- Le traitement des rounds déjà enregistrés sans statut n’est pas défini.
