# Créer une séance : impact sur le module de domaine

**Contexte**
En tant qu’administrateur ou membre d’un groupe, je veux créer une séance dans mon groupe afin que ses séances soient organisées. Le domaine est responsable des règles relatives aux séances : leur groupe, leur nom facultatif, le jour et l’heure de début obligatoires, la génération d’un nom lorsqu’il n’est pas fourni, l’unicité du créneau jour/heure au sein d’un groupe et l’appartenance à la liste des séances du groupe.

**Couverture des règles**
| Règle | Scénarios |
|---|---|
| Un membre ou un administrateur peut ajouter une séance à son groupe | D1, D2 |
| Une séance peut avoir un nom | D1, D3, D4 |
| Une séance doit avoir un jour | D5 |
| Une séance doit avoir une heure de début | D6 |
| Si le nom est absent ou vide, il est généré à partir du jour et de l’heure de début | D3, D4 |
| Un groupe ne peut avoir qu’une séance pour un même jour et une même heure de début | D7 |
| Des groupes différents peuvent utiliser le même jour et la même heure de début | D8 |
| Une séance créée apparaît dans la liste des séances du groupe | D9 |

**Critères d’acceptation**
Fonctionnalité : créer une séance

1. Un membre du groupe crée une séance nommée
Scénario : un membre crée une séance pour son groupe
Étant donné un membre du « Dojo du mardi »
Lorsque le membre crée une séance nommée « Entraînement du soir » pour mardi à 14 h 00
Alors la séance est créée dans le « Dojo du mardi »
Et son nom est « Entraînement du soir »

2. Un administrateur du groupe crée une séance nommée
Scénario : un administrateur crée une séance pour son groupe
Étant donné un administrateur du « Dojo du mardi »
Lorsque l’administrateur crée une séance nommée « Entraînement du soir » pour mardi à 14 h 00
Alors la séance est créée dans le « Dojo du mardi »
Et son nom est « Entraînement du soir »

3. Générer un nom lorsqu’aucun nom n’est fourni
Scénario : une séance sans nom fourni reçoit un nom généré
Étant donné un membre du « Dojo du mardi »
Lorsque le membre crée une séance pour mardi à 14 h 00 sans fournir de nom
Alors la séance est créée
Et son nom généré est basé sur mardi et 14 h 00

4. Générer un nom lorsque le nom fourni est vide
Scénario : une séance avec un nom vide reçoit un nom généré
Étant donné un membre du « Dojo du mardi »
Lorsque le membre crée une séance avec un nom vide pour mardi à 14 h 00
Alors la séance est créée
Et son nom généré est basé sur mardi et 14 h 00

5. Exiger un jour
Scénario : une séance sans jour n’est pas créée
Étant donné un membre du « Dojo du mardi »
Lorsque le membre crée une séance sans jour et avec une heure de début de 14 h 00
Alors la séance n’est pas créée, car un jour est obligatoire

6. Exiger une heure de début
Scénario : une séance sans heure de début n’est pas créée
Étant donné un membre du « Dojo du mardi »
Lorsque le membre crée une séance pour mardi sans heure de début
Alors la séance n’est pas créée, car une heure de début est obligatoire

7. Garantir l’unicité du créneau jour/heure dans un groupe
Scénario : un groupe ne peut pas avoir deux séances au même jour et à la même heure de début
Étant donné que le « Dojo du mardi » a déjà une séance le mardi à 14 h 00
Lorsqu’un membre crée une autre séance dans le « Dojo du mardi » pour mardi à 14 h 00
Alors le « Dojo du mardi » ne possède toujours qu’une séance le mardi à 14 h 00
Et aucune deuxième séance n’est créée

8. Autoriser le même jour et la même heure dans des groupes différents
Scénario : des groupes différents peuvent avoir des séances le même jour et à la même heure de début
Étant donné que le « Dojo du mardi » a une séance le mardi à 14 h 00
Et que le « Dojo du jeudi » n’a pas de séance le mardi à 14 h 00
Lorsqu’un membre crée une séance dans le « Dojo du jeudi » pour mardi à 14 h 00
Alors la séance est créée dans le « Dojo du jeudi »

9. Inclure la séance créée dans la liste des séances du groupe
Scénario : une séance créée apparaît dans la liste des séances de son groupe
Étant donné qu’un membre crée une séance dans le « Dojo du mardi » pour mardi à 14 h 00
Lorsque les séances du « Dojo du mardi » sont consultées
Alors la séance créée apparaît dans la liste

**Notes**
- Le format exact du nom généré n’est pas spécifié ; il doit être basé sur le jour et l’heure de début.
- Les jours pris en charge et les éventuelles règles de fuseau horaire ne sont pas spécifiés.
