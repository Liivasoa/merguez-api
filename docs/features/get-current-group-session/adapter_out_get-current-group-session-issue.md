# Récupérer la session en cours d’un groupe : adaptateur sortant de persistance

**Contexte**
Permettre au cas d’utilisation de charger par identifiant de groupe la session en cours, sa session programmée associée, ses rounds terminés et son unique round en cours avec son numéro et sa liste de combats. Cette issue dérive des invariants du domaine sans ajouter de règle métier.

**Critères d'acceptation**
Feature: récupérer la session en cours d’un groupe (adaptateur sortant)

1. Charger et reconstruire la session en cours complète du groupe
Ref Domain: D1, D2, D3, D4, D5, D6
Scenario: le dépôt retourne la session active et ses données associées pour le groupe demandé
Given les données persistées d’une session en cours et de ses rounds pour un groupe
When le port de chargement est appelé avec l’identifiant de ce groupe
Then l’adaptateur retrouve la session en cours unique associée au groupe
And il reconstruit la référence à la session programmée et la liste des rounds terminés
And il restaure l’unique round en cours avec son numéro et sa liste de combats

**Notes**
- Le résultat du chargement lorsqu’aucune session n’est en cours n’est pas spécifié.