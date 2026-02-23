# Copilot / AI Agent Instructions for PS-2025 (EscenaLocal)

This repository contains a combined Angular frontend and Spring Boot backend under `EscenaLocal`. The goal of these instructions is to help AI coding agents be immediately productive when making focused edits.

1) Big picture
- Frontend: Angular 19 app located in [EscenaLocal](EscenaLocal) (entry: [EscenaLocal/src/main.ts](EscenaLocal/src/main.ts)). Uses standalone components and the new `bootstrapApplication`/`provideRouter` APIs.
- Backend: Spring Boot (Java 17) in the same folder; build via Maven ([EscenaLocal/pom.xml](EscenaLocal/pom.xml)). Config lives in [EscenaLocal/src/main/resources/application.yml](EscenaLocal/src/main/resources/application.yml).

2) How to run / build
- Frontend dev: run `npm install` then `npm start` from the `EscenaLocal` folder (or `ng serve --proxy-config proxy.conf.json`). See [EscenaLocal/package.json](EscenaLocal/package.json).
- Backend dev: use the included Maven wrapper: `./mvnw spring-boot:run` (on Windows `mvnw.cmd spring-boot:run`) from `EscenaLocal`.
- Typical local flow: start backend first, then frontend (frontend uses `proxy.conf.json` to forward API calls to the backend).

3) Project-specific patterns & conventions
- Standalone components & routing: Routes are centrally declared in [EscenaLocal/src/app/app.routes.ts](EscenaLocal/src/app/app.routes.ts). When adding a new route create a standalone component and add it to `routes`.
- App-level providers: DI and interceptors are registered in [EscenaLocal/src/app/app.config.ts](EscenaLocal/src/app/app.config.ts). To add an HTTP interceptor, register it there and ensure `provideHttpClient(withInterceptorsFromDi())` remains present.
- Auth: Frontend uses an `AuthInterceptor` and `AuthGuard`/`RoleGuard` patterns (`EscenaLocal/src/app/auth.interceptor.ts`, `EscenaLocal/src/app/role.guard.ts`) — keep JWT handling consistent with backend tokens.
- Services: Business logic / API wrappers live in `EscenaLocal/src/app/services/` (examples: `auth.service.ts`, `event.service.ts`, `usuario.service.ts`). Use `HttpClient` + RxJS observables; controllers expect JSON payloads.
- State & change detection: `provideZoneChangeDetection({ eventCoalescing: true })` is enabled — avoid forcing global change detection resets unless necessary.

4) Integrations and external deps to be careful with
- Payments: frontend `@mercadopago/sdk-js` and backend `com.mercadopago:sdk-java` are used. Tests or local changes touching payments should avoid hitting production endpoints; use sandbox keys.
- DB: production uses PostgreSQL (runtime) and H2 is present for tests. DB config: [EscenaLocal/src/main/resources/application.yml](EscenaLocal/src/main/resources/application.yml).
- Security: backend uses `jjwt` for tokens (see `EscenaLocal/pom.xml`). When changing auth flows update both `AuthInterceptor` and server token generation/validation.

5) Tests & CI
- Frontend unit tests: `ng test` (Karma) — see `package.json` script. Backend tests: `mvn test`.

6) Common maintenance tasks (examples)
- Add a new API call: update backend controller + service, update frontend `event.service.ts` (or appropriate service), update any components that consume it, and add route(s) if a new page is required.
- Add an interceptor: implement class under `src/app/`, then register it in [EscenaLocal/src/app/app.config.ts](EscenaLocal/src/app/app.config.ts).
- Add a route/component: generate standalone component, wire into [EscenaLocal/src/app/app.routes.ts](EscenaLocal/src/app/app.routes.ts), and ensure guards (if needed) are configured.

7) Files to inspect first when troubleshooting
- Frontend bootstrap/config: [EscenaLocal/src/main.ts](EscenaLocal/src/main.ts), [EscenaLocal/src/app/app.config.ts](EscenaLocal/src/app/app.config.ts), [EscenaLocal/src/app/app.routes.ts](EscenaLocal/src/app/app.routes.ts).
- API services: [EscenaLocal/src/app/services/](EscenaLocal/src/app/services/).
- Security & integration: [EscenaLocal/src/app/auth.interceptor.ts](EscenaLocal/src/app/auth.interceptor.ts), [EscenaLocal/pom.xml](EscenaLocal/pom.xml), [EscenaLocal/src/main/resources/application.yml](EscenaLocal/src/main/resources/application.yml).

8) Behavior expectations for PRs by AI agents
- Make minimal, focused changes with tests where applicable. When an API or contract changes, update both client and server code in the same PR and include a short note about required runtime env vars (DB, MercadoPago keys, JWT secrets).
- Preserve the Angular standalone-component strategy and `provideRouter` pattern. Avoid converting the app to NgModules.

9) When you need human input
- Missing credentials (DB, MercadoPago sandbox keys, JWT secret) — ask instead of committing secrets.
- Ambiguous API behavior — request the expected request/response shape or sample payloads.

If anything above is unclear or you want the file adapted to different agent behavior (e.g., stricter tests, inline examples), tell me which section to expand.
