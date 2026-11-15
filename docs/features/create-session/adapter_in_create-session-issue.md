# Créer une séance : impact sur l’adaptateur entrant

**Contexte**
Exposer la création de séances et la consultation des séances d’un groupe via l’adaptateur REST entrant, en faisant correspondre les requêtes aux cas d’utilisation applicatifs. Cette issue découle de [domain_create-session-issue.md](./domain_create-session-issue.md) ; elle ne définit aucune règle métier supplémentaire.

**Critères d'acceptation**
Feature: créer une séance (adaptateur entrant)

1. Transmettre les requêtes de création de séance à l’application
Ref Domain: D1, D2, D3, D4
Scenario: une requête contenant un jour, une heure de début et un nom facultatif déclenche la création d’une séance
Given une requête REST visant à créer une séance pour un groupe
And la requête contient un jour et une heure de début, avec un nom facultatif
When l’adaptateur entrant traite la requête
Then il transmet le groupe et les détails de la séance au cas d’utilisation de création de séance
And il conserve le nom absent ou vide afin que l’application puisse le générer

2. Gérer l’absence des informations de planification obligatoires
Ref Domain: D5, D6
Scenario: une requête sans jour ou heure de début n’est pas considérée comme une création de séance valide
Given une requête REST de création de séance à laquelle il manque un jour ou une heure de début
When l’adaptateur entrant traite la requête
Then la requête est rejetée comme invalide
And le cas d’utilisation de création de séance ne crée aucune séance

3. Exposer la liste des séances d’un groupe
Ref Domain: D9
Scenario: une requête de consultation des séances d’un groupe inclut la séance créée
Given une séance a été créée pour un groupe
When l’adaptateur entrant traite une requête visant à consulter les séances de ce groupe
Then la réponse inclut la séance créée

**Notes**
- Les routes exactes, les champs de réponse et les codes d’état HTTP ne sont pas spécifiés.
