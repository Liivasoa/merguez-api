# Ionic Angular Frontend Instructions

These instructions apply to the Ionic Angular application under `frontend/`. The frontend has not been scaffolded yet; when it is added, keep its package manifest, source, tests, and build configuration inside this directory.

## Boundaries

- Keep UI presentation, navigation, and client-side interaction in this application. The Spring API remains under `backend/` and owns server-side business rules and persistence.
- Call the API through explicit HTTP client services and typed frontend models. Do not access backend source code or MongoDB directly.
- Keep API base URLs in the frontend's environment/runtime configuration; do not hardcode machine-specific URLs in feature components.
- Keep Ionic/Angular dependencies and scripts in `frontend/package.json`. Do not add frontend dependencies to the Maven build.
- Keep web and native-target concerns within the Ionic/Capacitor frontend configuration; do not make the API serve the compiled UI unless the user requests that deployment design.

## Implementation and quality

- Organize UI code by user-facing capability, then by Angular responsibility. Avoid empty layers and generic catch-all folders.
- Follow the Angular and Ionic versions and conventions established when the frontend is scaffolded; do not assume a test runner or lint command that is not configured.
- Add or update frontend tests for behavior when the user asks for testing or the selected feature workflow requires them. Report exact commands and results.
- Keep frontend-specific decisions and instructions here. Put cross-application API contract decisions in root documentation or the relevant backend feature issue.
