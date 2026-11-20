# Clôturer une séance : application

**Contexte**
Coordonner la clôture d'une occurrence de séance, le calcul des merguez et la notification des membres du groupe.

**Critères d'acceptation**
Feature: Coordonner la clôture d'une séance

1. Coordonner une clôture acceptée
Ref Domain: D1, D2, D5, D6, D7
Scenario: Clôturer une séance et transmettre les résultats
Given une demande de clôture par un admin ou un membre et une séance en cours avec au moins un round
When le cas d'utilisation traite la demande
Then il applique la clôture et lance le calcul des scores nets et des transactions de règlement
And il sauvegarde la séance clôturée
And il transmet une demande de notification pour chaque membre du groupe, y compris ceux dont le score est nul

2. Transmettre les refus du domaine
Ref Domain: D3, D4
Scenario: Refuser une clôture non autorisée par l'état de la séance
Given une séance qui n'est pas en cours ou qui ne contient aucun round
When le cas d'utilisation traite une demande de clôture
Then il transmet le refus du domaine sans sauvegarder de clôture
And il ne lance ni calcul de merguez ni notification de clôture

**Notes**
- Les règles métier sont définies dans l'issue domaine.
- Les garanties en cas d'échec de sauvegarde ou de notification restent à définir ; ne pas masquer les erreurs techniques.
