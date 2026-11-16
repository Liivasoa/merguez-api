# Démarrer une séance : impact sur le module de domaine

**Contexte**
En tant qu’administrateur ou membre d’un groupe, je veux démarrer une séance planifiée afin de créer une occurrence qui représente cette séance en cours. Le démarrage enregistre la date et l’heure sur cette occurrence, sans modifier la séance planifiée, et un groupe ne peut avoir qu’une occurrence démarrée à la fois.

**Couverture des règles**
| Règle | Scénarios |
|---|---|
| Un membre peut démarrer une séance | D1 |
| Un administrateur peut démarrer une séance | D2 |
| La date et l’heure de démarrage sont enregistrées | D3 |
| Une seule séance peut être démarrée pour un groupe donné | D4 |
| Des séances peuvent être démarrées simultanément dans des groupes différents | D5 |

**Critères d'acceptation**
Feature: démarrer une séance

1. Démarrer une séance en tant que membre du groupe
Scenario: un membre démarre une séance de son groupe
Given un membre d’un groupe et une séance planifiée de ce groupe sans occurrence démarrée
When le membre démarre la séance
Then une occurrence démarrée est créée pour cette séance planifiée

2. Démarrer une séance en tant qu’administrateur du groupe
Scenario: un administrateur démarre une séance de son groupe
Given un administrateur d’un groupe et une séance planifiée de ce groupe sans occurrence démarrée
When l’administrateur démarre la séance
Then une occurrence démarrée est créée pour cette séance planifiée

3. Enregistrer la date et l’heure du démarrage
Scenario: le démarrage crée une occurrence avec son propre instant de début
Given une séance planifiée qui n’a pas encore d’occurrence démarrée
When un membre ou un administrateur du groupe la démarre
Then une occurrence distincte est créée et associée à la séance planifiée
And la date et l’heure du démarrage sont enregistrées sur cette occurrence
And la séance planifiée reste inchangée

4. Empêcher le démarrage d’une deuxième séance dans le même groupe
Scenario: un groupe ne peut pas avoir deux séances démarrées
Given un groupe qui a déjà une séance démarrée
And une autre séance planifiée de ce groupe
When un membre ou un administrateur tente de démarrer cette autre séance
Then le démarrage de cette autre séance est refusé
And la séance déjà démarrée reste la seule séance démarrée du groupe

5. Autoriser des séances démarrées dans des groupes différents
Scenario: chaque groupe peut avoir sa propre séance démarrée
Given un groupe qui a déjà une séance démarrée
And un autre groupe qui n’a aucune séance démarrée
When un membre ou un administrateur du second groupe démarre une de ses séances planifiées
Then une occurrence démarrée est créée pour le second groupe
And chaque groupe n’a qu’une seule séance démarrée

**Notes**
- La source exacte de la date et de l’heure, ainsi que les règles de fuseau horaire, ne sont pas spécifiées.
- Une occurrence démarrée est distincte de la séance planifiée qu’elle référence.
