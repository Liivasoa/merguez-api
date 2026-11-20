# Ajouter des rounds à une séance : impact sur le module de domaine

**Contexte**
En tant qu’administrateur ou membre d’un groupe, je veux ajouter des rounds à une séance en cours pour y organiser les combats entre les membres du groupe. Le domaine garantit l’état requis de la séance, le statut et la numérotation des rounds, et la composition des combats.

**Couverture des règles**
| Règle | Scénarios |
|---|---|
| Seule une séance en cours accepte des rounds | D1, D2 |
| Le premier round porte le numéro 1 et les suivants s’incrémentent | D3 |
| Un round possède un statut et démarre en cours | D1 |
| Un groupe ne peut pas démarrer un nouveau round tant qu’un de ses rounds est en cours | D3, D10 |
| Un round en cours dans un groupe ne bloque pas un autre groupe | D11 |
| Un round est créé avec au moins un combat | D1, D4 |
| Les combats réunissent exactement deux membres | D1, D5 |
| Un membre ne participe qu’à un combat par round | D6 |
| Des combats peuvent être ajoutés tant qu’il reste deux membres non assignés | D7, D8 |
| Un combat peut avoir un gagnant ou aucun gagnant | D1, D9 |
| Un administrateur ou membre du groupe peut ajouter un round | D1 |

**Critères d'acceptation**
Feature: ajouter des rounds à une séance en cours

1. Ajouter le premier round avec un combat valide (D1)
Scenario: un administrateur ou un membre ajoute un premier round à une séance en cours
Given un administrateur ou un membre du groupe et une séance de ce groupe en cours sans round
And deux membres du groupe qui ne sont assignés à aucun combat
When l’utilisateur ajoute un round contenant un combat entre ces deux membres, sans gagnant désigné
Then le round est créé pour la séance
And il contient au moins ce combat composé exactement de ces deux membres
And le combat est conservé sans gagnant
And son statut est en cours

2. Refuser l’ajout d’un round si la séance n’est pas en cours (D2)
Scenario: une séance qui n’est pas en cours ne peut pas recevoir de round
Given une séance qui n’est pas en cours
When un administrateur ou un membre du groupe tente d’y ajouter un round valide
Then l’ajout du round est refusé
And aucun round n’est ajouté à cette séance

3. Numéroter les rounds à partir de 1 et incrémenter le numéro (D3)
Scenario: les rounds d’une séance reçoivent des numéros successifs
Given une séance en cours dont le premier round porte le numéro 1 et n’est plus en cours
And aucun autre round du groupe n’est en cours
When un membre du groupe ajoute un nouveau round valide
Then le nouveau round porte le numéro 2
And son statut est en cours

4. Refuser la création d’un round sans combat (D4)
Scenario: un round ne peut pas être créé sans combat initial
Given une séance en cours sans round
When un administrateur ou un membre tente de créer un round sans combat
Then la création du round est refusée
And aucun round n’est ajouté à la séance

5. Refuser un combat qui ne contient pas exactement deux membres (D5)
Scenario: un combat avec un seul membre ou plus de deux membres est refusé
Given une séance en cours et un round valide
When un administrateur ou un membre tente d’ajouter un combat avec un seul membre ou plus de deux membres
Then l’ajout du combat est refusé
And le round reste inchangé

6. Empêcher un membre de combattre plus d’une fois dans le même round (D6)
Scenario: un membre déjà assigné ne peut pas être ajouté à un autre combat du round
Given un round contenant un combat auquel un membre est déjà assigné
When un administrateur ou un membre tente d’ajouter un autre combat avec ce membre
Then l’ajout du combat est refusé
And le membre reste assigné uniquement à son combat initial dans ce round

7. Ajouter plusieurs combats avec des membres non assignés (D7)
Scenario: chaque combat ajouté utilise deux membres encore non assignés
Given une séance en cours avec un round contenant un combat entre deux membres
And deux autres membres du groupe qui ne sont assignés à aucun combat de ce round
When un membre du groupe ajoute un combat entre ces deux membres non assignés
Then le nouveau combat est ajouté au round
And chaque membre du round figure dans un seul combat

8. Refuser un combat lorsqu’il ne reste pas deux membres non assignés (D8)
Scenario: aucun combat supplémentaire ne peut être ajouté sans deux membres disponibles
Given un round dans lequel il ne reste qu’un seul membre non assigné ou aucun
When un administrateur ou un membre tente d’ajouter un combat
Then l’ajout du combat est refusé
And les combats existants du round restent inchangés

9. Conserver un gagnant lorsqu’il est renseigné (D9)
Scenario: un combat peut désigner un gagnant
Given une séance en cours et deux membres non assignés dans un round
When un membre du groupe ajoute un combat entre ces deux membres en désignant l’un d’eux comme gagnant
Then le combat est ajouté au round
And le gagnant désigné est associé à ce combat

10. Refuser un nouveau round lorsqu’un round du groupe est en cours (D10)
Scenario: un groupe ne peut pas démarrer deux rounds en même temps
Given une séance en cours et un round de ce groupe dont le statut est en cours
When un administrateur ou un membre tente d’ajouter un autre round valide
Then l’ajout du nouveau round est refusé
And le round existant reste inchangé
And aucun autre round n’est ajouté

11. Ne pas bloquer un autre groupe (D11)
Scenario: un round en cours dans un groupe ne bloque pas un autre groupe
Given un premier groupe ayant un round en cours
And un second groupe ayant une séance en cours sans round en cours
When un membre du second groupe ajoute un round valide à sa séance
Then le round du second groupe est créé avec le statut en cours
And le round du premier groupe reste inchangé

**Notes**
- Le numéro des rounds est propre à chaque séance et repart à 1 pour une nouvelle séance.
- La clôture d’un round sera définie ultérieurement et ne fait pas partie de cette issue.
- Les rounds existants sans statut seront supprimés de la base de données ; leur traitement est hors périmètre.
