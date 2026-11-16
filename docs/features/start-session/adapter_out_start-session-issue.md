# Démarrer une séance : impact sur l’adaptateur sortant

**Contexte**
Persister chaque démarrage comme un document d’occurrence distinct de la séance planifiée, conserver sa date et son heure de début, et garantir l’unicité des occurrences actives par groupe. Cette issue découle de [domain_start-session-issue.md](./domain_start-session-issue.md) et ne définit aucune règle métier supplémentaire.

**Critères d'acceptation**
Feature: démarrer une séance (adaptateur sortant)

1. Persister l’état démarré et l’instant de début
Ref Domain: D1, D2, D3
Scenario: une occurrence démarrée est enregistrée indépendamment de la séance planifiée
Given une séance planifiée démarrée par un membre ou un administrateur de son groupe
When l’adaptateur de persistance enregistre le démarrage
Then un nouveau document d’occurrence est créé dans la collection des occurrences de séance
And il contient son propre identifiant, le `groupId`, le `scheduleId`, le statut `IN_PROGRESS` et le `startedAt`
And `startedAt` est conservé comme date BSON représentant un instant UTC
And la séance planifiée reste un document distinct et inchangé

2. Garantir l’unicité des séances démarrées par groupe
Ref Domain: D4, D5
Scenario: un index unique partiel limite les occurrences actives à une par groupe
Given un groupe qui a déjà une séance démarrée
And un autre groupe sans séance démarrée
When l’adaptateur tente d’enregistrer une deuxième occurrence `IN_PROGRESS` dans le premier groupe et une occurrence `IN_PROGRESS` dans le second
Then un index unique partiel sur `groupId`, filtré par `status: IN_PROGRESS`, empêche deux occurrences actives dans le premier groupe
And l’occurrence du second groupe est enregistrée
And la contrainte est appliquée par MongoDB même lorsque les tentatives sont concurrentes

**Notes**
- Ajouter un index non unique `{ groupId: 1, startedAt: -1 }` pour retrouver efficacement l’historique d’un groupe.
- Les rounds référenceront l’occurrence par `sessionRunId`, pas la séance planifiée ; leur stockage et leur indexation sont hors périmètre de cette issue.
- `endedAt` peut être renseigné lors de la clôture de l’occurrence ; cette issue couvre uniquement son démarrage.
