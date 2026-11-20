# Clôturer une séance : domaine

**Contexte**
Un admin ou un membre du groupe clôture une occurrence de séance en cours. Les résultats des combats servent à calculer les merguez à régler et chaque membre est notifié.

**Critères d'acceptation**
Feature: Clôturer une séance et calculer les merguez

1. D1 - Clôturer une séance par un admin
Scenario: Clôture nominale par un admin
Given une séance en cours avec un round où A a gagné contre B
And un admin du groupe demande sa clôture
When la séance est clôturée
Then la séance est clôturée
And le calcul donne 1 merguez à recevoir pour A et 1 à payer pour B
And une transaction de 1 merguez de B vers A solde ces positions
And chaque membre du groupe est notifié

2. D2 - Clôturer une séance par un membre
Scenario: Clôture nominale par un membre
Given une séance en cours avec au moins un round
And un membre du groupe demande sa clôture
When la séance est clôturée
Then la séance est clôturée
And le calcul des merguez est lancé
And chaque membre du groupe est notifié

3. D3 - Refuser une séance qui n'est pas en cours
Scenario: Clôture d'une séance non en cours
Given une séance qui n'est pas en cours
When sa clôture est demandée
Then la clôture est refusée
And aucun calcul ni notification de clôture n'est lancé

4. D4 - Refuser une séance sans round
Scenario: Clôture d'une séance sans round
Given une séance en cours sans round
When sa clôture est demandée
Then la clôture est refusée et la séance reste en cours
And aucun calcul ni notification de clôture n'est lancé

5. D5 - Calculer les scores nets et solder les positions
Scenario: Victoires et défaites réparties sur plusieurs rounds
Given une séance en cours où A a gagné deux combats contre B et B un combat contre C
And D est membre du groupe mais n'a gagné ni perdu de combat
When la séance est clôturée
Then les scores nets sont A = 2, B = -1, C = -1 et D = 0
And B et C paient chacun 1 merguez à A et D ne paie ni ne reçoit rien
And les positions sont exactement soldées et leur somme totale reste nulle
And tous les membres du groupe, y compris D, sont notifiés

6. D6 - Ne créer aucun paiement pour des positions nulles
Scenario: Résultats sans position nette à régler
Given une séance en cours avec deux combats entre A et B, gagnés une fois chacun
And un combat sans vainqueur entre C et D
When la séance est clôturée
Then les scores nets de A, B, C et D sont nuls
And aucune transaction de paiement n'est nécessaire
And chaque membre du groupe est notifié

7. D7 - Réduire le nombre de paiements
Scenario: Solder des positions qui peuvent être appariées directement
Given une séance en cours dont les scores nets calculés sont A = 8, B = 7, C = -7 et D = -8
When les transactions de règlement sont calculées
Then D paie 8 merguez à A et C paie 7 merguez à B
And les positions sont exactement soldées en deux paiements
And la somme totale des positions est conservée

**Notes**
- Couverture : acteurs et clôture : D1, D2 ; séance en cours : D1 à D4 ; au moins un round : D1 à D4 ; lancement du calcul : D1, D2, D5 à D7 ; notification de chaque membre : D1, D2, D5, D6 ; score net et règlement : D1, D5 à D7 ; réduction des paiements : D7.
- Le canal, le contenu et le traitement des échecs de notification ne sont pas définis.
- Le traitement d'un round encore en cours au moment de la clôture et la garantie d'un minimum global de paiements ne sont pas précisés.
