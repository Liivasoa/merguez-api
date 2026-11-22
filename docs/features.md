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
- Une seule séance peut être démarré pour un groupe donnée


### En tant qu'admin ou membre d'un groupe, je veux ajouter des rounds dans une séance en cours

Règles :
- Il faut qu'une séance soit en cours pourqu'un utilisateur puisse y ajouter des rounds
- Un round doit avoir un numéros pour l'identifier (commence par 1 et s'autoincrémente au fur et à mésure)
- Un round doit avoir un ou plusieurs combats au moment de sa création
- Un utilisateur peut ajouter autant de combat qu'il veut dans un round tant qu'il y a encore de membre non assigné à un combat
- Un combat doit avoir exactement 2 membres
- Un membre ne doit figurer que dans un seul combat pour un round donné
- Un combat doit avoir un membre gagnant ou non

### En tant qu'admin ou membre d'un groupe, je veux clôturer un round

Règles :
- Un round en cours passe au statut `CLOSED`.
- Les résultats indiquent un vainqueur ou aucun vainqueur pour chaque combat du round.
- Le vainqueur éventuel doit participer au combat.
- Un round clôturé ne peut plus recevoir de combat et un nouveau round peut être créé.

### En tant qu'admin ou membre d'un groupe, je veux clôturer une séance

Règles :
- Seule une séance en cours peut être clôturer
- Seule une séance avec au moins un round peut être clôturer
- Tous les rounds de la séance doivent être clôturés ; sinon, la clôture est refusée sans lancer le traitement
- Le calcul des merguez(points) doit être lancé
- La notification de chaque membre est reportée ; un log la remplace temporairement

Règles sur le calcul des merguez (points) :
- À la fin d'une séance, le score net d'un membre est son nombre de victoires moins son nombre de défaites.
- Un point net positif représente des merguez à recevoir ; un point net négatif représente des merguez à payer. Les transactions doivent solder exactement ces positions, conserver la somme totale des soldes et minimiser le nombre de paiements lorsque c'est possible.


### En tant qu'utilisateur, je veux consulter la liste de mes groupes

Règles :
- Je veux avoir la liste des groupes dans lesquels j'y figure (en tant que membre ou admin)
- Je veux avoir le nom du groupe, description, nombre des membres, nombres de sessions programmés, si je suis admin

## En tant qu'utilisateur, je veux récuperer la session en cours de mon groupe

Règles :
- Il peut y avoir une seule et unique session en cours pour un groupe donnée
- Une session en cours doit avoir son numéro
- Une session en cours doit avoir la session programmé qui lui correspond
- Une session en cours doit avoir la liste des rounds terminé
- Une session en cours doit avoir un et un seul round en cours
- Un round contient exactement 2 participants
- Un round peut avoir un vainqueur
- Un round doit avoir un numéro unique pour chaque round d'une même session

## En tant qu'utilisateur, je veux savoir s'il existe déjà une session en cours dans mon groupe




### NEXT
- proposition de combats à la création d'un round/début de séance
