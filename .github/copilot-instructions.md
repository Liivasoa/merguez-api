# Copilot instructions

## Build, test, and run

The project uses the Maven Wrapper, Spring Boot 4.1.1, and Java 25 (`java.version` in `pom.xml`). Prefer the wrapper so Maven 3.10.0 is used.

- Run tests: `./mvnw test` (Windows: `.\mvnw.cmd test`)
- Build the package: `./mvnw package` (Windows: `.\mvnw.cmd package`)
- Run a single test class: `./mvnw -Dtest=MerguezApiApplicationTests test`
- Run a single test method: `./mvnw -Dtest=MerguezApiApplicationTests#contextLoads test`
- Start the application: `./mvnw spring-boot:run`

There is no dedicated lint command configured in `pom.xml`.

## Architecture

This is currently a minimal Spring Boot application. `MerguezApiApplication` is the `@SpringBootApplication` entry point in `mg.msys.merguez_api`; `application.yaml` sets the Spring application name. The only test is a Spring context-load test in the same package. No API, domain, persistence, or service layers have been established yet.

The POM inherits dependency and plugin management from the Spring Boot parent. Docker Compose support is included as an optional runtime dependency, but `compose.yaml` currently declares no services; `HELP.md` notes this may prevent the application from starting.

## Repository conventions

- Keep Java sources and tests under the `mg.msys.merguez_api` package. The underscore is intentional; the generated `HELP.md` notes it replaces the invalid hyphenated package name.
- Keep Spring configuration in `src/main/resources/application.yaml`.
- Place tests under `src/test/java` in the matching package; the existing test uses JUnit Jupiter and `@SpringBootTest`.
