# Créer un groupe : impact module Application

**Contexte**
Exposer la création d'un groupe : contrat d'entrée/sortie, validation des données reçues, orchestration de la création via le domaine et exposition des erreurs. Cette issue est dérivée de [domain_create-group-issue.md](./domain_create-group-issue.md) ; aucune nouvelle règle métier n'y est introduite.

**Critères d'acceptation**
Feature: Créer un groupe (couche application)
    In order to permettre la création d'un groupe
    As a utilisateur
    I want to appeler le cas d'usage de création de groupe

1. Création d'un groupe et mapping de la réponse
Ref Domain: D1, D2, D3, D8, D9
Scenario: Une demande valide crée le groupe et retourne son contenu
Given une demande de création contenant l'identifiant du créateur, un nom, une description optionnelle et la liste des identifiants des participants
When le cas d'usage de création de groupe est exécuté
Then le groupe est créé via le domaine
And la réponse contient l'identifiant du groupe, son nom et sa description (absente si non fournie)
And la réponse indique le créateur avec le rôle admin
And la réponse indique chaque participant ajouté avec le rôle membre

2. Validation du nom
Ref Domain: D4, D5
Scenario: Une demande dont le nom est absent, vide ou blanc est rejetée
Given une demande de création dont le nom est absent, vide ou composé uniquement d'espaces
When le cas d'usage de création de groupe est exécuté
Then la demande est rejetée avec une erreur de validation indiquant que le nom est obligatoire
And aucun groupe n'est créé

3. Validation des participants
Ref Domain: D6, D7
Scenario: Une demande sans autre participant que le créateur est rejetée
Given une demande de création dont la liste des participants est absente, vide ou ne contient que le créateur
When le cas d'usage de création de groupe est exécuté
Then la demande est rejetée avec une erreur de validation indiquant qu'au moins un autre participant que le créateur est requis
And aucun groupe n'est créé

4. Exposition des erreurs métier du domaine
Ref Domain: D4, D5, D6, D7
Scenario: Une erreur métier du domaine est traduite en erreur applicative
Given une demande qui contourne la validation d'entrée et que le domaine refuse
When le cas d'usage de création de groupe est exécuté
Then l'erreur du domaine est traduite en la même erreur de validation que celle des scénarios 2 et 3
And aucun groupe n'est persisté

**Notes**
- Hors périmètre : authentification/autorisation (l'identifiant du créateur est une donnée d'entrée du cas d'usage), gestion des identifiants de participants inexistants ou en doublon, codes HTTP et routes exacts (à définir à l'implémentation).
- Les scénarios D1, D2, D3, D8 et D9 partagent le même traitement applicatif (mapping commande → domaine → réponse) et sont donc regroupés.
