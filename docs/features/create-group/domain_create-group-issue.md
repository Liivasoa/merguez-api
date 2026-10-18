# Créer un groupe : impact module Domain

**Contexte**
En tant qu'utilisateur, je veux créer un groupe afin de comptabiliser avec d'autres participants les résultats de combats de jiu-jitsu. Le domaine porte les invariants : un groupe a obligatoirement un nom, au moins un participant autre que son créateur, et une description optionnelle. À la création, le créateur devient admin et les participants ajoutés deviennent membres.

**Couverture des règles**
| Règle | Scénarios |
|---|---|
| Un utilisateur peut créer un groupe | D1 |
| Un groupe doit avoir obligatoirement un nom | D4, D5 |
| Un groupe doit avoir au moins un autre participant que le créateur | D6, D7 |
| Un groupe peut avoir une description | D2, D3 |
| Le créateur devient admin du groupe | D8 |
| Les utilisateurs ajoutés deviennent membres du groupe | D9 |

**Critères d'acceptation**
Feature: Créer un groupe
    In order to comptabiliser les résultats de combats avec d'autres participants
    As a utilisateur
    I want to créer un groupe

1. Création nominale d'un groupe (D1)
Scenario: Un utilisateur crée un groupe avec un nom et un participant
Given un utilisateur "Alice"
And un autre utilisateur "Bob"
When Alice crée un groupe nommé "Dojo du mardi" avec Bob comme participant
Then le groupe "Dojo du mardi" est créé

2. Création avec description (D2)
Scenario: Un groupe est créé avec une description
Given un utilisateur "Alice" et un autre utilisateur "Bob"
When Alice crée un groupe nommé "Dojo du mardi" avec la description "Séances entre amis" et Bob comme participant
Then le groupe est créé
And la description du groupe est "Séances entre amis"

3. Création sans description (D3)
Scenario: Un groupe est créé sans description
Given un utilisateur "Alice" et un autre utilisateur "Bob"
When Alice crée un groupe nommé "Dojo du mardi" sans description avec Bob comme participant
Then le groupe est créé
And le groupe n'a pas de description

4. Refus d'un groupe sans nom (D4)
Scenario: La création est refusée lorsque le nom est absent
Given un utilisateur "Alice" et un autre utilisateur "Bob"
When Alice crée un groupe sans nom avec Bob comme participant
Then la création est refusée car le nom est obligatoire
And aucun groupe n'est créé

5. Refus d'un groupe avec un nom vide (D5)
Scenario: La création est refusée lorsque le nom est vide ou composé uniquement d'espaces
Given un utilisateur "Alice" et un autre utilisateur "Bob"
When Alice crée un groupe dont le nom est vide ou composé uniquement d'espaces avec Bob comme participant
Then la création est refusée car le nom est obligatoire
And aucun groupe n'est créé

6. Refus d'un groupe sans autre participant (D6)
Scenario: La création est refusée lorsque aucun participant n'est ajouté
Given un utilisateur "Alice"
When Alice crée un groupe nommé "Dojo du mardi" sans aucun participant
Then la création est refusée car au moins un autre participant que le créateur est requis
And aucun groupe n'est créé

7. Refus d'un groupe dont le seul participant est le créateur (D7)
Scenario: La création est refusée lorsque le créateur est le seul participant
Given un utilisateur "Alice"
When Alice crée un groupe nommé "Dojo du mardi" avec uniquement elle-même comme participant
Then la création est refusée car au moins un autre participant que le créateur est requis
And aucun groupe n'est créé

8. Le créateur devient admin (D8)
Scenario: Le créateur du groupe est admin
Given un utilisateur "Alice" et un autre utilisateur "Bob"
When Alice crée un groupe nommé "Dojo du mardi" avec Bob comme participant
Then Alice est admin du groupe

9. Les participants ajoutés deviennent membres (D9)
Scenario: Les utilisateurs ajoutés sont membres du groupe
Given un utilisateur "Alice" et deux autres utilisateurs "Bob" et "Chloé"
When Alice crée un groupe nommé "Dojo du mardi" avec Bob et Chloé comme participants
Then Bob est membre du groupe
And Chloé est membre du groupe
And Bob et Chloé ne sont pas admins du groupe

**Notes**
- Hors périmètre : authentification/autorisation, unicité du nom de groupe, longueur maximale du nom ou de la description, ajout/retrait ultérieur de participants, gestion des participants en doublon ou inexistants (non spécifiés).
- Le point "nom vide ou uniquement des espaces" (D5) est une interprétation de « nom obligatoire ».
- Les issues `application` et `infrastructure` dérivent de ces scénarios D1 à D9.
