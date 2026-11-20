# Clôturer une séance : adaptateurs sortants

**Contexte**
Conserver la clôture de la séance et transmettre les notifications aux membres du groupe.

**Critères d'acceptation**
Feature: Persister la clôture et transmettre les notifications

1. Conserver la clôture
Ref Domain: D1, D2, D5, D6
Scenario: Sauvegarder puis recharger une séance clôturée
Given une séance clôturée après calcul des merguez
When l'adaptateur de persistance sauvegarde puis recharge la séance
Then la séance rechargée est identifiée comme clôturée

2. Transmettre les notifications
Ref Domain: D1, D2, D5, D6
Scenario: Notifier chaque membre du groupe
Given une demande de notification de clôture destinée à chaque membre du groupe
When l'adaptateur de notification la transmet
Then chaque membre est destinataire d'une notification, y compris les membres dont le score est nul

**Notes**
- Les règles métier sont définies dans l'issue domaine.
- Le canal de notification et les garanties de livraison ne sont pas définis par la demande.
