# Clôturer une séance : adaptateurs sortants

**Contexte**
Conserver la clôture de la séance et journaliser le remplacement temporaire des notifications aux membres du groupe.

**Critères d'acceptation**
Feature: Persister la clôture et journaliser les notifications reportées

1. Conserver la clôture
Ref Domain: D1, D2, D5, D6
Scenario: Sauvegarder puis recharger une séance clôturée
Given une séance clôturée après calcul des merguez
When l'adaptateur de persistance sauvegarde puis recharge la séance
Then la séance rechargée est identifiée comme clôturée

2. Journaliser les notifications reportées
Ref Domain: D1, D2, D5, D6
Scenario: Journaliser pour chaque membre du groupe
Given une demande de journalisation de clôture pour chaque membre du groupe
When l'adaptateur la traite
Then un log est émis pour chaque membre, y compris les membres dont le score est nul

**Notes**
- Les règles métier sont définies dans l'issue domaine.
- Aucun canal de notification n'est implémenté dans cette fonctionnalité ; le log ne constitue pas une notification.
