# Créer un groupe : impact module Infrastructure

**Contexte**
Persister un groupe créé et ses membres (avec leur rôle) via un adapter de persistance. Cette issue est dérivée de [domain_create-group-issue.md](./domain_create-group-issue.md) ; aucune nouvelle règle métier n'y est introduite.

**Critères d'acceptation**
Feature: Créer un groupe (couche infrastructure)
    In order to conserver les groupes créés
    As a système
    I want to persister un groupe, sa description et les rôles de ses participants

1. Persistance du groupe et des rôles
Ref Domain: D1, D2, D3, D8, D9
Scenario: Un groupe créé est persisté avec son créateur admin et ses membres
Given un groupe valide avec un nom, une description optionnelle, un créateur admin et des participants membres
When l'adapter de persistance enregistre le groupe
Then le groupe est enregistré avec son nom et sa description (vide si non fournie)
And le créateur est enregistré avec le rôle admin
And chaque participant est enregistré avec le rôle membre
And la relecture du groupe restitue les mêmes données et les mêmes rôles

2. Échec technique de persistance
Ref Domain: D1, D8, D9
Scenario: Un échec d'enregistrement ne laisse aucune donnée partielle
Given un groupe valide à enregistrer
And le stockage échoue pendant l'enregistrement
When l'adapter de persistance enregistre le groupe
Then une erreur technique est remontée à l'appelant
And ni le groupe ni aucun de ses membres n'est persisté

**Notes**
- Hors périmètre : migrations, contraintes d'unicité du nom, retries, observabilité.
- Les scénarios D2, D3 (description optionnelle) et D8, D9 (rôles) partagent le même mapping de persistance et sont regroupés dans le scénario 1.
