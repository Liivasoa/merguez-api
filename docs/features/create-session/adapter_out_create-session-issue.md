# Créer une séance : impact sur l’adaptateur sortant

**Contexte**
Persister les séances avec leur association au groupe et mettre les séances d’un groupe à la disposition de l’application. Cette issue découle de [domain_create-session-issue.md](./domain_create-session-issue.md) ; elle ne définit aucune règle métier supplémentaire.

**Critères d’acceptation**
Fonctionnalité : créer une séance (adaptateur sortant)

1. Persister la séance avec son groupe et ses informations de planification
Référence au domaine : D1, D2, D3, D4
Scénario : une séance créée est enregistrée avec son groupe, son jour, son heure de début et son nom
Étant donné une séance valide pour un groupe, avec un jour et une heure de début
Et que son nom est explicite ou a été généré à partir du jour et de l’heure de début
Lorsque l’adaptateur de persistance enregistre la séance
Alors la séance est associée à ce groupe
Et son jour, son heure de début et son nom sont conservés

2. Limiter l’unicité du créneau de planification à son groupe
Référence au domaine : D7, D8
Scénario : la persistance n’enregistre pas de créneau en double dans un groupe et ne le bloque pas dans un autre
Étant donné qu’un groupe a une séance le mardi à 14 h 00
Lorsqu’une autre séance pour ce créneau est enregistrée dans le même groupe
Et qu’une séance pour ce créneau est enregistrée dans un autre groupe
Alors le même groupe ne possède pas de deuxième séance le mardi à 14 h 00
Et l’autre groupe peut avoir sa propre séance pour ce créneau

3. Charger les séances associées à un groupe
Référence au domaine : D9
Scénario : la consultation des séances d’un groupe renvoie la séance qui y a été créée
Étant donné qu’une séance a été enregistrée pour un groupe
Lorsque l’adaptateur de persistance charge les séances de ce groupe
Alors les séances renvoyées incluent la séance enregistrée

**Notes**
- Aucune technologie de stockage ni aucun mécanisme de persistance particulier n’est imposé.
