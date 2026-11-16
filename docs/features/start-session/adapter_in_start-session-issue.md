# Démarrer une séance : impact sur l’adaptateur entrant

**Contexte**
Exposer le démarrage d’une séance par l’adaptateur REST entrant et transmettre la demande au cas d’utilisation applicatif. Cette issue découle de [domain_start-session-issue.md](./domain_start-session-issue.md) et ne définit aucune règle métier supplémentaire.

**Critères d'acceptation**
Feature: démarrer une séance (adaptateur entrant)

1. Transmettre une demande valide de démarrage
Ref Domain: D1, D2, D3
Scenario: la requête de démarrage transmet les informations de la séance au cas d’utilisation
Given une requête REST visant à démarrer une séance d’un groupe
When l’adaptateur entrant traite la requête
Then il transmet l’identifiant du groupe et de la séance au cas d’utilisation de démarrage
And il renvoie le résultat du démarrage avec la date et l’heure enregistrées

2. Exposer le refus d’un démarrage déjà actif dans le groupe
Ref Domain: D4, D5
Scenario: l’adaptateur distingue un démarrage refusé dans un groupe d’un démarrage permis dans un autre
Given un groupe qui a déjà une séance démarrée et un autre groupe qui n’en a aucune
When des requêtes sont reçues pour démarrer une séance dans chacun de ces groupes
Then la requête pour la séance supplémentaire du premier groupe expose le refus du cas d’utilisation
And la requête pour le second groupe expose le résultat du démarrage réussi

**Notes**
- La route, le format détaillé des réponses et le code HTTP du refus ne sont pas spécifiés.
