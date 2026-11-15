# Créer une séance : impact sur le module applicatif

**Contexte**
Mettre en œuvre le flux applicatif de création d’une séance dans un groupe et de récupération des séances de ce groupe. Le cas d’utilisation orchestre la création par le domaine et la persistance sans introduire de nouvelles règles métier. Cette issue découle de [domain_create-session-issue.md](./domain_create-session-issue.md).

**Critères d'acceptation**
Feature: créer une séance (couche applicative)

1. Orchestrer la création d’une séance valide
Ref Domain: D1, D2, D3, D4
Scenario: une requête valide crée une séance pour un membre ou un administrateur du groupe
Given une commande de création de séance pour un groupe et l’un de ses membres ou administrateurs
And la commande contient un jour et une heure de début, avec un nom facultatif
When le cas d’utilisation de création de séance est exécuté
Then il crée la séance via le domaine et l’enregistre pour ce groupe
And un nom absent ou vide est généré à partir du jour et de l’heure de début

2. Rejeter une séance à laquelle il manque une information de planification obligatoire
Ref Domain: D5, D6
Scenario: une commande sans jour ou heure de début ne crée pas de séance
Given une commande de création de séance à laquelle il manque un jour ou une heure de début
When le cas d’utilisation de création de séance est exécuté
Then la requête est rejetée comme invalide
And aucune séance n’est enregistrée

3. Garantir l’unicité dans un groupe, mais pas entre les groupes
Ref Domain: D7, D8
Scenario: le créneau existant d’un groupe n’est pas dupliqué, tandis qu’un autre groupe peut l’utiliser
Given un groupe possède déjà une séance le mardi à 14 h 00
And un autre groupe n’a pas de séance le mardi à 14 h 00
When une séance le mardi à 14 h 00 est demandée dans chacun des groupes
Then le premier groupe ne possède toujours qu’une séance pour ce créneau
And la séance peut être créée pour l’autre groupe

4. Récupérer les séances d’un groupe
Ref Domain: D9
Scenario: la consultation des séances d’un groupe inclut une séance nouvellement créée
Given une séance a été créée pour un groupe
When l’application récupère les séances de ce groupe
Then la liste renvoyée inclut la séance créée

**Notes**
- Les routes HTTP et les codes d’état exacts ne sont pas spécifiés.
