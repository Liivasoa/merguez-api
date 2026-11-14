# Créer une séance : impact sur l’adaptateur entrant

**Contexte**
Exposer la création de séances et la consultation des séances d’un groupe via l’adaptateur REST entrant, en faisant correspondre les requêtes aux cas d’utilisation applicatifs. Cette issue découle de [domain_create-session-issue.md](./domain_create-session-issue.md) ; elle ne définit aucune règle métier supplémentaire.

**Critères d’acceptation**
Fonctionnalité : créer une séance (adaptateur entrant)

1. Transmettre les requêtes de création de séance à l’application
Référence au domaine : D1, D2, D3, D4
Scénario : une requête contenant un jour, une heure de début et un nom facultatif déclenche la création d’une séance
Étant donné une requête REST visant à créer une séance pour un groupe
Et que la requête contient un jour et une heure de début, avec un nom facultatif
Lorsque l’adaptateur entrant traite la requête
Alors il transmet le groupe et les détails de la séance au cas d’utilisation de création de séance
Et il conserve le nom absent ou vide afin que l’application puisse le générer

2. Gérer l’absence des informations de planification obligatoires
Référence au domaine : D5, D6
Scénario : une requête sans jour ou heure de début n’est pas considérée comme une création de séance valide
Étant donné une requête REST de création de séance à laquelle il manque un jour ou une heure de début
Lorsque l’adaptateur entrant traite la requête
Alors la requête est rejetée comme invalide
Et le cas d’utilisation de création de séance ne crée aucune séance

3. Exposer la liste des séances d’un groupe
Référence au domaine : D9
Scénario : une requête de consultation des séances d’un groupe inclut la séance créée
Étant donné qu’une séance a été créée pour un groupe
Lorsque l’adaptateur entrant traite une requête visant à consulter les séances de ce groupe
Alors la réponse inclut la séance créée

**Notes**
- Les routes exactes, les champs de réponse et les codes d’état HTTP ne sont pas spécifiés.
