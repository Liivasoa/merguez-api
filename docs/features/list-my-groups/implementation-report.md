# Rapport d'implémentation : list-my-groups

## Périmètre

La feature expose les groupes dont l'utilisateur est membre ou admin, avec leur nom, description, nombre de personnes, nombre de sessions programmées et indicateur admin.

## Couches

- **Domain — complète** ([issue](./domain_list-my-groups-issue.md) : D1–D4). `Group.summaryFor` produit les résumés et écarte les groupes sans association.
- **Application — complète** ([issue](./application_list-my-groups-issue.md) : A1–A2). `ListMyGroupsUseCase` délègue au port `LoadGroupSummariesPort` et retourne les résultats, y compris une liste vide.
- **Adapter sortant — complet** ([issue](./adapter_out_list-my-groups-issue.md) : recherche et liste vide). Mongo recherche par identifiant utilisateur et projette chaque groupe en résumé à travers le domaine.
- **Adapter REST entrant — complet** ([issue](./adapter_in_list-my-groups-issue.md) : retour des résumés et collection vide). `GET /api/v1/groups?userId=…` appelle le cas d'usage et renvoie les champs du résumé.

## Vérification

- `mvn.cmd '-Duser.home=C:\Users\livar' -o '-Dtest=ListMyGroupsServiceTest,ListMyGroupsTest' test` : 6 tests réussis.
- `mvn.cmd '-Duser.home=C:\Users\livar' -o '-Dtest=MongoGroupRepositoryUnitTest' test` : 3 tests réussis.
- `mvn.cmd '-Duser.home=C:\Users\livar' -o '-Dtest=GroupControllerTest' test` : 74 tests réussis.
- `mvn.cmd '-Duser.home=C:\Users\livar' -o '-Dtest=GroupConfigurationTest' test` : 9 tests réussis.
- `mvn.cmd '-Duser.home=C:\Users\livar' -o test` : suite complète réussie, 252 tests, aucune erreur. Les tests Mongo Testcontainers ont également passé avec Docker disponible.
- `git diff --check` : réussi.
- Le validateur d'issues n'a pas pu être exécuté : aucun lanceur `py`, `python` ou `python3` n'est disponible. La structure Gherkin, l'ordre des étapes, les références D1–D4 et les scénarios ont été contrôlés manuellement.

## Commits

Les commits de la feature sur `feature/list-my-groups` sont :

- Domain et Application : `e8f0b09`, `09ac291`, `29be321`, `3e7b78e`, `aa2e07c`, `99e980d`, `72bcfb9`.
- Adapter sortant : `2e9b50f`, `d95eefc`, `0638532`.
- Adapter REST entrant et câblage : `d683191`, `7fe203d`, `05e553b`, `b92f3f1`, `9d1f888`.

Le commit `b92f3f1` a ajouté le cas d'usage à la vérification du câblage Spring; `9d1f888` a fourni le port de lecture mocké au test de configuration.

## Notes

Le changement local non commité de `src/test/java/mg/msys/merguez_api/group/application/usecase/ListMyGroupsServiceTest.java` a été conservé et n'a été inclus dans aucun commit de feature. Il ajoute un test pour la liste vide.

Le paramètre `userId` est transmis par l'appelant; l'authentification et la détermination de l'utilisateur courant sont hors périmètre des issues. La branche est locale : la connexion SSH à `origin` a échoué (`Permission denied`), donc aucun push ni pull request n'a été créé.
