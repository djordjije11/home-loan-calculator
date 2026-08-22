# AGENTS.md

This file defines repository-wide implementation and testing conventions for `home-loan-calculator`. Use it to capture durable, reusable rules. Do not store
one-off instructions for a single class, method, entity, or external-system edge case here unless the rule is truly architectural and should guide similar work
elsewhere.

## Scope And Goal

- Keep changes aligned with the current architecture and coding style.
- Prefer consistency with existing code over introducing new patterns.
- Preserve behavior unless a change request explicitly asks for behavior changes.
- Generalize rules whenever possible. Prefer conventions such as `validate invariants in the lowest owning layer` over type-specific instructions like
  `Class X must assert field Y`.
- Keep method-specific or system-specific details close to the owning code and tests unless the same rule genuinely applies across the repository.

## Architecture Rules

- Respect package and layer boundaries:
    - `*.application`: orchestration services only.
    - `*.domain`: entities, value objects, domain exceptions, factories, repository interfaces, and domain/application configuration records.
    - `*.infra`: inbound/outbound adapters and persistence-facing adapters, without domain business rules.
    - `*.query`: read-only projections and query repositories.
    - `ch.autoscout24.config`: Spring wiring and cross-cutting configuration.
    - `ch.autoscout24.integration.external.*`: external system clients and transport contracts.
- Keep read projections consumer-driven and minimal. Add only fields required by current consumers; do not mirror full aggregate state by default.
- When one module consumes another module in-process:
    - keep the consuming module dependent on its own port and projection, not provider-side types,
    - keep consumer-side ports and projections in the consuming module domain,
    - name consumer ports `*ServiceClient` and in-process adapters `*ServiceClientImpl`,
    - implement the bridge in `infra.inprocess`,
    - delegate to provider services or repositories and map provider projections to consumer projections.
    - for provider-side read models exposed across module boundaries, prefer `*.query` repositories over support repositories.
- Keep business rules in the domain layer, not in application services or infrastructure.
- Application services orchestrate: validate arguments, load and save aggregates, invoke domain behavior, and publish internal events when needed.
- Do not duplicate validation in the application layer when the owning entity, value object, DTO, or domain method already enforces the invariant.
- Keep cross-aggregate reactions inside the local application boundary. External or Kafka handlers should invoke local application services first; other
  aggregates should react to resulting internal application events rather than directly to external payloads.
- Keep external/message-consumer handlers separate from internal Spring listeners: use `*EventHandler` for external entry points and `*InternalEventHandler` for
  internal application events.
- Keep temporary or internal application events in the owning module package, not shared/global packages.

## Validation And Domain Modeling

- Validate at the lowest level that owns the invariant.
- Use `Assert.notNull` at public boundaries and in constructors, factories, and domain methods for required arguments.
- Let entities and value objects own invariants, state transitions, eligibility checks, and derived calculations.
- Keep idempotency as a default rule for entity state-change methods. After mandatory guard checks, repeated calls in already-applied state should return
  without extra mutation.
- In update operations, execute guard validations before no-op checks so invalid calls still fail as expected.
- Return early when an update would not change effective state, to avoid unnecessary audit/version churn.
- For upsert-style operations, make the create-vs-update decision in the application layer and keep mutations in dedicated domain methods.
- When one aggregate reacts to another aggregate's event, pass only the minimum identity or context needed and let the target aggregate compute its own derived
  state instead of copying foreign metadata.
- Keep value objects, projections, and helper records minimal: expose only fields and behavior used by production code.
- Do not embed value objects in query/persistence projections; keep projection fields flat and map to value objects in services when needed.
- For pure persistence or transport records that do not own business invariants, prefer simple constructors/accessors over custom guard-heavy logic.
- Keep persistent entity equality and hash code identity-based unless the domain explicitly requires a different semantic.

## REST, API, And Security

- REST controllers use `/api/v1` as the base path.
- Use `PUT` for idempotent state updates and `POST` for executable actions and commands.
- Controller methods must define `@PreAuthorize` rules that match the route scope.
- When authorization depends on seller identity, use seller-scoped routes (`/api/v1/sellers/{sellerId}/...`) and combine capability checks with seller ownership
  checks through `@authenticationChecker`.
- Global OpenAPI bearer auth is configured centrally; do not repeat method-level security declarations unless an endpoint intentionally differs.
- For request DTOs and controller parameters, prefer descriptive names over generic names like `dto` or `payload`.
- For monetary request fields in this service, use `@NotNull`, `@Positive`, and `@Max(value = 9999999)` unless a documented contract requires otherwise.

## External Integrations

- For external REST integrations, use `RestClient` instead of `RestTemplate`.
- Wire a shared `RestClient` bean as `builder.build()` and inject it directly into clients.
- Keep endpoint, auth, and related config values as fields; set headers and content type per request instead of mutating the shared client.
- Use explicit request and response DTOs rather than map-based payload construction.
- Model timestamps with time types such as `ZonedDateTime` when the external contract represents timestamps.
- Keep DTOs and projections minimal and contract-driven.
- Preserve system-specific call order, required literals, endpoint paths, and returned identifiers in the integration code and its tests rather than hard-coding
  those details as global AGENTS rules unless they are reused across multiple integrations.
- For fixed external request defaults that intentionally apply to every call, encode the defaults in DTO constructors or dedicated factories instead of
  repeating literals throughout the client.
- Prefer one consistent response contract for related create and update operations when the external API supports it.
- Validate response content fields instead of adding redundant null-response checks when `RestClient` retrieval already handles transport-level concerns.
- Keep reusable endpoint path templates as dedicated fields instead of repeating string literals.
- Precompute reusable immutable request payloads or encoded URIs in singleton clients when they are configuration-derived and identical across calls.
- If a singleton client caches mutable auth or session state, make the cache thread-safe; do not keep unsynchronized mutable token state on the bean.
- Apply retries around individual remote calls, not around multi-step orchestration methods that could repeat already-successful calls.
- Reuse central resilience constants and configuration where available, and define retry instances in `application.yaml`.

## Coding Conventions

- Use constructor injection and keep dependency fields `final`.
- For class JavaDoc author tags, use the locally configured developer name, never assistant/tool names.
- For controller classes using Lombok constructor injection, place `@RequiredArgsConstructor` first in the annotation list.
- Avoid wildcard imports; keep imports explicit after any optimize-import action.
- Prefer `Optional.ofNullable(...).map(...).orElse(...)` over ternary null mapping where it improves readability.
- For simple presence checks, prefer `value != null` over `Objects.nonNull(value)`.
- Prefer method references for simple getter lambdas when readable.
- Prefer concise names when context is already explicit: `id` over repeated aggregate-specific names, `create(...)` or `update(...)` over repetitive helper
  names.
- Use `find...` for methods that return `Optional` and `get...` for methods that return the resolved object directly and throw when it is missing.
- Order private methods by first use in public methods; shared private helpers can appear below all public methods.
- Keep consecutive `Assert` guard statements contiguous and add a single blank line before the next non-assert statement.
- Inline temporary object creation when it exists only for `save(...)` or immediate handoff; keep locals only when they improve readability or are needed later.
- For `RestClient` calls where only the response body is used, prefer inlining `.retrieve().body(...)` or `.toBodilessEntity()` rather than creating temporary
  response variables.
- Keep service logging consistent: log before an operation and after successful completion, phrase success logs as `* successfully *`, and keep wording concise.
- Keep exception placement consistent: domain-specific exceptions in the domain layer, `NotFoundException` in the application layer for missing aggregates or
  read data.
- In handlers that branch by event payload type, prefer pattern-matching `switch` over `if` / `else if` chains.
- For aggregate-scoped events whose type already identifies the aggregate, prefer the field name `id`.
- Keep enum constants ordered alphabetically whenever touching an enum.
- In domain methods, leave a blank line before assigning `auditMetadata`.
- For small allow-lists of permitted enum statuses, prefer positive checks (`==` with `||`) wrapped in a single negation over chained `!=` checks.

## Time, Configuration, And Persistence

- Use `jakarta.validation.ClockProvider` in Spring configuration, resolve `Clock` in the service constructor, and pass it into domain operations that depend on
  current time.
- Use `@ConfigurationProperties` record classes for externalized domain and application configuration and keep properties under `autoscout24.*` in
  `src/main/resources/application.yaml`.
- Repository interfaces extend `org.springframework.data.repository.Repository` and declare only required methods explicitly.
- Keep aggregate repository and support repository responsibilities separate:
    - aggregate repositories load and save full aggregates by identity,
    - support repositories handle existence checks, projections, and collection/bulk lookups that return IDs or read models.
- For collection or bulk mutations, fetch IDs first via support repositories and then load each aggregate through the aggregate repository before mutating.
- Avoid native SQL when equivalent derived queries or JPQL are sufficient.
- Keep schema changes in Liquibase changelog files.
- For Liquibase changeSet IDs in this service, use `<fileVersion>.<year>.<quarter>.<order>`, where `fileVersion` matches the changelog file, `year` uses two
  digits, `quarter` is `1`-`4`, and `order` is zero-based within the quarter.
- Keep entity mappings, Java fixtures, and SQL fixtures aligned.
- For relational persistence of small structured value objects, prefer explicit columns over JSON unless JSON is a deliberate requirement.
- For simple persistence projections that only group columns, prefer generated constructors/accessors over custom guard logic unless the type owns business
  invariants.

## Testing Strategy

- Add or update both unit and integration tests when behavior, persistence wiring, time handling, configuration, or integration contracts change.
- Always add integration tests for repository classes.
- Use `@SpringBootTest(webEnvironment = NONE)` unless web behavior is under test.
- For REST controllers, use Spring Boot integration tests rather than standalone `MockMvcBuilders.standaloneSetup(...)`.
- For external REST clients, add:
    - unit tests for argument validation, mapping, retries, and call ordering,
    - integration tests with mocked HTTP servers such as WireMock that verify endpoint path, auth header, payload JSON, and required call order.
- When client logic intentionally handles the same failure through both typed exceptions and generic status-based exceptions, add test coverage for each
  supported branch.
- For application and domain integration tests, focus on happy-path scenarios. Cover guard clauses and negative branches primarily in unit tests unless the user
  explicitly asks otherwise.
- In module-scoped integration tests, mock collaborators from other modules instead of reaching across module boundaries for implementation details or fixtures.
- Use shared SQL fixtures with `@Sql(... BEFORE_TEST_METHOD)` for setup and `@Sql(... AFTER_TEST_METHOD)` for cleanup. Use `@SqlMergeMode(MergeMode.MERGE)` for
  extra per-test SQL.
- If production repositories intentionally filter data, use `EntityManager` in tests for raw fetches instead of adding test-only repository methods.
- When deterministic IDs are available in integration tests, prefer `entityManager.find(...)` over ad hoc JPQL.
- In tests, inline one-use repository/entity fetches instead of extracting single-use private helper methods.
- Never reset database sequences in tests. When generated IDs are not deterministic, use stable business keys for setup/cleanup and avoid exact generated-id
  assertions.
- For generated primary key assertions, prefer meaningful checks such as `isGreaterThan(1000L)` over `isNotNull()`.
- For time-dependent integration tests, provide a fixed clock through test configuration by overriding `ClockProvider`.
- For controller integration tests:
    - compose API path constants from reusable base segments,
    - avoid one-off intermediate path constants,
    - use different example values for different route identifiers,
    - use the shared public API example ID namespace in controller and contract fixtures, and keep different resource types on different example IDs such as
      `sellerId = 1001`, `listingId = 1101`, and nested listing-scoped child resource IDs in a separate range like `1201+`, unless the fixture already follows
      an established alternative,
    - keep identifier values consistent across URL placeholders, auth claims, test inputs, and service verifications,
    - for endpoints without request DTOs or JSON bodies, keep controller integration tests focused on the happy path; add invalid-request `400` coverage only
      for endpoints that accept request DTOs or JSON bodies,
    - for successful controller GET-response assertions, load the expected JSON from the matching contract fixture helper instead of inlining response JSON in
      the test,
    - keep request payloads in contract fixtures rather than inline text blocks when the repository already uses contract data fixtures,
    - keep Spring Cloud Contract request and response bodies in adjacent `*_request.json` and `*_response.json` fixture files referenced via `bodyFromFile`
      rather than embedding JSON bodies inline in YAML.
- In `*ControllerBase` contract tests, keep standard mocked service return values in the matching `*ControllerContractDataFixture` and reuse them from both the
  base class and related controller tests instead of constructing those return objects inline.
- Contract fixtures live under `src/test/resources/fixture/contract/<controllerName>/`, and the matching Spring Cloud Contract base class lives under
  `src/test/java/ch/autoscout24/contract/` as `<ControllerName>Base`.

### Unit Test Conventions

- Domain unit tests cover invalid arguments, valid state transitions, and rule violations/exceptions.
- Application service unit tests use Mockito, verify orchestration interactions, and include invalid argument coverage.
- Prefer class-level `@Mock` fields with `@ExtendWith(MockitoExtension.class)` and `@InjectMocks` when it keeps setup shorter and clearer.
- For retry-orchestrated code, mock the `RetryRegistry` but return a real retry instance instead of manually executing retry lambdas with `doAnswer(...)`.
- For enum-based allow/deny rules, prefer parameterized tests with one test for allowed values and one for disallowed values.
- In guard-clause tests, set only the minimum state needed to reach the target guard.
- Use `0L` for wrong/non-existing ID scenarios in tests.
- Test naming:
    - if a production method has exactly one test in a class, use the production method name,
    - otherwise use `<method>_when..._should...`,
    - use `create` as the method prefix for constructor tests,
    - use `..._shouldDoNothing` for no-op scenarios.
    - in integration tests, when an operation has a single happy-path test in that class, name it with just the operation or method name; add scenario suffixes
      only for additional variants of that same operation.
- Test ordering inside a class:
    - invalid-arguments test first,
    - valid/happy-path test second,
    - remaining tests in the same order as production guards and branches.
- Exception: for controller tests with valid/invalid request pairs, place the valid request test first.
- For branch-heavy operations, make sure each meaningful success and error branch has explicit coverage.

### Fixture And Assertion Conventions

- Prefer shared fixture objects over duplicated inline object construction when a common baseline already exists.
- Never use static imports for fixture methods; reference fixture helpers through their fixture class name.
- Keep one standard happy-path fixture per aggregate; create variant states directly in test setup rather than multiplying near-duplicate baseline fixtures.
- When a test needs a record value that differs from the standard fixture record, create that record inline in the test with hardcoded values; do not copy
  values out of the standard fixture record.
- Do not extract one-off variant record creation in tests into private helper methods; keep such record construction inline in the test method.
- Keep Java fixtures and SQL fixtures synchronized.
- For DTO constructor tests, prefer `hasAllNullFieldsOrPropertiesExcept(...)` on the created DTO and then assert populated fields explicitly instead of
  asserting the whole DTO via recursive comparison.
- Compare entities with AssertJ recursive comparison.
- For entity-operation tests, compare against a shared baseline, ignore only expected differences, and assert those ignored fields separately.
- When only part of an embedded object changed, ignore only those nested fields and assert them separately.
- After a recursive comparison with `ignoringFields(...)`, follow up only with assertions for the ignored fields.
- In tests that verify audit updates, assert meaningful timestamp or version changes instead of merely asserting non-null audit metadata.
- Ignore fields that are expected to differ (`id`, `version`, `auditMetadata`, timestamps, etc.), then assert business-critical changes explicitly.
- For time fields in recursive comparisons, use a comparator based on `ZonedDateTime::toInstant`.
- Avoid temporary locals used only once in assertions unless they improve business readability.
- Do not add blank lines between consecutive assertion statements.
- For aggregates that register domain events, ignore `domainEvents` in recursive comparisons, place it last in `ignoringFields(...)`, and assert events
  separately by count and type only.

### Integration Test Infrastructure

- Before running non-unit tests that require infrastructure, start backing services with `scripts/start-backing-services.sh` and stop them afterward with
  `scripts/stop-backing-services.sh`.
- Use the scripts-based lifecycle when integration tests change: start services, run targeted tests, then stop services.
- For Kafka event-handler integration tests, enable the handler via test properties, drive the binding through `StreamBridge`, and use `TestRebalanceListener`
  for assignment.
- Provision required Kafka topics in `scripts/wait-and-setup-backing-services.sh`; do not rely on binder auto-create topics.
- For outbound Kafka events, follow the `StreamBridge` + `@TransactionalEventListener` publisher pattern and keep producer bindings configured for synchronous
  sends.

## Change Checklist

- Update code in the correct layer.
- Add or update unit tests for logic and guard clauses.
- Add or update integration tests when wiring, persistence, time, external contract, or Spring configuration behavior changes.
- Update shared fixtures and SQL fixtures together when data shape changes.
- Run targeted tests for touched components first.
- Run `./gradlew test ...` when unit-test code changes, starting with the touched tests.
- Before requesting review readiness, run `./gradlew build` so Checkstyle and other quality gates are covered.
- Run `./scripts/build.sh` right before committing only when a full-repository verification is needed, and always before any user-requested commit or push.
- Before committing, format code and optimize imports for touched files.
- When extracting a feature from another branch, bring runtime code, schema changes, fixtures, and tests together.
- Stage all newly created files in VCS after implementing the change.
- Pull request titles must include the Jira ticket in square brackets, for example `[PB-138] Register auction upserted integration event`.
- When a user asks to push, commit with an appropriate message, push the current branch, and re-request PR review.
- After a change or review cycle reveals a durable repository convention, update `AGENTS.md` with the generalized rule rather than adding another one-off note.
