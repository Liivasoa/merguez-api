# Fonctionnalités de Merguez
Merguez est une application de scoring de jiu-jitsu entre amis. Le MVP permet à un groupe de comptabiliser les résultats de combats pendant une séance, puis de calculer le solde de merguez entre ses membres.

## Fonctionnalités

### En tant qu'utilisateur, je veux créer un groupe

Règles :
- Un utilisateur peut créer un groupe
- Un groupe doit avoir obligatoirement un nom
- Un groupe doit avoir au moins un autre participant que le créateurs
- Un groupe peut avoir une description
- Une fois le groupe créé, l'utilisateur qui l'a créé devient l'admin du groupe
- Une fois le groupe créé, les utilisateurs ajoutés dans le groupe devieent des membres du groupe

### En tant qu'admin ou membre d'un groupe, je veux créer une séance

Règles :
- Un membre/admin peut ajouter des séances à son groupe
- Une séance peut avoir un nom (optionel)
- Une séance doit avoir un jour (ex: mardi, jeudi)
- Une séance doit avoir une heure de début
- Si le nom est vide, on le génére à partir du jour et l'heure
- Dans un même groupe, sur un jour et une heure pareil (ex: mardi 14h), il ne doit y avoir qu'une seule séance
- Une fois créer, la séance figure parmis la liste des séances du groupe

### En tant qu'admin ou membre d'un groupe, je veux démarrer une séance

Règles :
- Un membre/admin peut démarrer une séance
- Quand une séance est démarré, la date et heure de début doivent être enregistré

### En tant qu'admin ou membre d'un groupe, je veux ajouter des rounds dans une séance en cours

Règles :
- Il faut qu'une séance soit en cours pourqu'un utilisateur puisse y ajouter des rounds
- Un round doit avoir un numéros pour l'identifier
- Un round doit avoir un ou plusieurs combats
- Un combat doit avoir exactement 2 membres
- Un membre ne doit figurer que dans un seul combat pour un round donné
- Un combat doit avoir un membre gagnant ou non

### En tant qu'admin ou membre d'un groupe, je veux clôturer une séance

Règles :
- Seule une séance en cours peut être clôturer
- Seule une séance avec au moins un round peut être clôturer
- Le calcul des merguez(points) doit être lancé
- Chaque membre doit être notifié

Règles sur le calcul des merguez (points) :
- À la fin d'une séance, le score net d'un membre est son nombre de victoires moins son nombre de défaites.
- Un point net positif représente des merguez à recevoir ; un point net négatif représente des merguez à payer. Les transactions doivent solder exactement ces positions, conserver la somme totale des soldes et minimiser le nombre de paiements lorsque c'est possible.