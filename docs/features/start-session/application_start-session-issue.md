# Démarrer une séance : impact sur le module applicatif

**Contexte**
Mettre en œuvre le cas d’utilisation qui démarre une séance planifiée en créant une occurrence distincte, conserve l’instant de début produit par le domaine et orchestre la persistance de cette occurrence. Cette issue découle de [domain_start-session-issue.md](./domain_start-session-issue.md) et n’ajoute pas de règle métier.

**Critères d'acceptation**
Feature: démarrer une séance (couche applicative)

1. Orchestrer le démarrage d’une séance et enregistrer son résultat
Ref Domain: D1, D2, D3
Scenario: le cas d’utilisation crée et persiste une occurrence de séance
Given une demande de démarrage d’une séance planifiée par un membre ou un administrateur de son groupe
When le cas d’utilisation de démarrage est exécuté
Then il invoque le domaine pour créer une occurrence démarrée associée à cette séance planifiée
And il demande la persistance de l’occurrence avec la date et l’heure de démarrage enregistrées
And la séance planifiée n’est pas remplacée par l’occurrence

2. Respecter l’unicité des séances démarrées dans le périmètre du groupe
Ref Domain: D4, D5
Scenario: un second démarrage est refusé dans le même groupe mais permis dans un autre
Given un groupe qui a déjà une séance démarrée
And un autre groupe qui n’a aucune séance démarrée
When le cas d’utilisation tente de créer une deuxième occurrence dans le premier groupe et une occurrence dans le second
Then la deuxième occurrence du premier groupe est refusée et n’est pas persistée
And l’occurrence du second groupe est créée et persistée

**Notes**
- Les erreurs applicatives précises et leur représentation ne sont pas spécifiées.
