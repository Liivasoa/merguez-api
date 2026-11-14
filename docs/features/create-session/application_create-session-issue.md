# Créer une séance : impact sur le module applicatif

**Contexte**
Mettre en œuvre le flux applicatif de création d’une séance dans un groupe et de récupération des séances de ce groupe. Le cas d’utilisation orchestre la création par le domaine et la persistance sans introduire de nouvelles règles métier. Cette issue découle de [domain_create-session-issue.md](./domain_create-session-issue.md).

**Critères d’acceptation**
Fonctionnalité : créer une séance (couche applicative)

1. Orchestrer la création d’une séance valide
Référence au domaine : D1, D2, D3, D4
Scénario : une requête valide crée une séance pour un membre ou un administrateur du groupe
Étant donné une commande de création de séance pour un groupe et l’un de ses membres ou administrateurs
Et que la commande contient un jour et une heure de début, avec un nom facultatif
Lorsque le cas d’utilisation de création de séance est exécuté
Alors il crée la séance via le domaine et l’enregistre pour ce groupe
Et un nom absent ou vide est généré à partir du jour et de l’heure de début

2. Rejeter une séance à laquelle il manque une information de planification obligatoire
Référence au domaine : D5, D6
Scénario : une commande sans jour ou heure de début ne crée pas de séance
Étant donné une commande de création de séance à laquelle il manque un jour ou une heure de début
Lorsque le cas d’utilisation de création de séance est exécuté
Alors la requête est rejetée comme invalide
Et aucune séance n’est enregistrée

3. Garantir l’unicité dans un groupe, mais pas entre les groupes
Référence au domaine : D7, D8
Scénario : le créneau existant d’un groupe n’est pas dupliqué, tandis qu’un autre groupe peut l’utiliser
Étant donné qu’un groupe possède déjà une séance le mardi à 14 h 00
Et qu’un autre groupe n’a pas de séance le mardi à 14 h 00
Lorsqu’une séance le mardi à 14 h 00 est demandée dans chacun des groupes
Alors le premier groupe ne possède toujours qu’une séance pour ce créneau
Et la séance peut être créée pour l’autre groupe

4. Récupérer les séances d’un groupe
Référence au domaine : D9
Scénario : la consultation des séances d’un groupe inclut une séance nouvellement créée
Étant donné qu’une séance a été créée pour un groupe
Lorsque l’application récupère les séances de ce groupe
Alors la liste renvoyée inclut la séance créée

**Notes**
- Les routes HTTP et les codes d’état exacts ne sont pas spécifiés.
