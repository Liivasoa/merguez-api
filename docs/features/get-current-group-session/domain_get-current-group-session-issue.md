# Récupérer la session en cours d’un groupe : domaine

**Contexte**
En tant qu’utilisateur, je veux récupérer la session en cours de mon groupe à partir de l’identifiant du groupe. La session en cours conserve sa session programmée associée, ses rounds terminés et son unique round en cours, lequel expose son numéro et sa liste de combats.

**Couverture des règles**
| Règle | Scénarios |
|---|---|
| La récupération s’effectue par l’identifiant du groupe | D1 |
| Un groupe ne peut avoir qu’une session en cours | D2 |
| La session en cours référence la session programmée correspondante | D3 |
| La session en cours expose la liste des rounds terminés | D4 |
| La session en cours possède un seul round en cours | D5 |
| Le round en cours est identifié par son numéro et possède une liste de combats | D6 |

**Critères d'acceptation**
Feature: récupérer la session en cours d’un groupe

1. Récupérer la session à partir de l’identifiant du groupe
Scenario: la session en cours est recherchée pour le groupe demandé
Given un identifiant de groupe
When la session en cours est récupérée
Then la recherche porte sur ce groupe

2. Garantir l’unicité de la session en cours par groupe
Scenario: un groupe ne possède qu’une seule session en cours
Given un groupe avec une session en cours
When la session en cours de ce groupe est récupérée
Then une seule session en cours est associée à ce groupe

3. Conserver la session programmée correspondante
Scenario: la session en cours référence sa session programmée
Given une session en cours issue d’une session programmée du groupe
When la session en cours est récupérée
Then la session programmée correspondante est disponible dans le résultat

4. Exposer les rounds terminés
Scenario: la session en cours expose la liste de ses rounds terminés
Given une session en cours avec des rounds terminés
When la session en cours est récupérée
Then la liste des rounds terminés est disponible

5. Exposer un unique round en cours
Scenario: la session en cours possède exactement un round en cours
Given une session en cours avec un round en cours
When la session est récupérée
Then elle expose un et un seul round en cours

6. Exposer le numéro et la liste de combats du round en cours
Scenario: le round en cours est identifié par son numéro et expose ses combats
Given une session en cours avec un round en cours ayant un numéro et une liste de combats
When le round en cours est récupéré
Then son numéro est disponible pour l’identifier
And sa liste de combats est disponible

**Notes**
- L’absence d’une occurrence active est traduite en erreur applicative par le cas d’utilisation; aucune règle de récupération supplémentaire n’est requise du domaine.
