# Consulter mes groupes : impact module Domain

**Contexte**
En tant qu'utilisateur, je veux consulter les groupes auxquels j'appartiens afin de voir les informations essentielles de chacun. Le domaine doit inclure les groupes où l'utilisateur est membre ou admin et fournir le nom, la description, le nombre de personnes du groupe, le nombre de sessions programmées ainsi que le statut admin de l'utilisateur.

**Couverture des règles**
| Règle | Scénarios |
|---|---|
| La liste comprend les groupes où l'utilisateur est membre | D1 |
| La liste comprend les groupes où l'utilisateur est admin | D2 |
| Chaque groupe expose son nom, sa description, le nombre de membres, le nombre de sessions programmées et le statut admin de l'utilisateur | D3 |
| Un utilisateur sans groupe obtient une liste vide | D4 |

**Critères d'acceptation**
Feature: Consulter mes groupes

1. Groupes où l'utilisateur est membre
Scenario: La liste inclut les groupes où l'utilisateur est membre
Given un utilisateur membre de deux groupes
When les groupes de cet utilisateur sont consultés
Then les deux groupes figurent dans la liste
And aucun groupe dont l'utilisateur n'est ni membre ni admin ne figure dans la liste

2. Groupes où l'utilisateur est admin
Scenario: La liste inclut les groupes où l'utilisateur est admin
Given un utilisateur admin d'un groupe
When les groupes de cet utilisateur sont consultés
Then ce groupe figure dans la liste

3. Informations résumées de chaque groupe
Scenario: Chaque groupe fournit les informations demandées
Given un groupe avec un nom, une description, quatre personnes dont un admin et trois membres, et deux sessions programmées
And l'utilisateur qui consulte est l'admin de ce groupe
When les groupes de cet utilisateur sont consultés
Then le résumé contient le nom et la description du groupe
And le nombre de membres est quatre, admin compris
And le nombre de sessions programmées est deux
And le statut admin de l'utilisateur est vrai

4. Aucun groupe associé
Scenario: Un utilisateur sans groupe obtient une liste vide
Given un utilisateur qui n'est membre ni admin d'aucun groupe
When les groupes de cet utilisateur sont consultés
Then la liste retournée est vide

**Notes**
- Le nombre de membres inclut l'admin, car celui-ci est une personne du groupe.
- Dans le modèle actuel, les sessions programmées désignent les créneaux de session enregistrés pour le groupe.
