# POS Payment Terminal

A production-grade Android application simulating a point-of-sale (POS) terminal — supporting Credit, Debit, Pix, and Voucher transactions with real-time input validation, local persistence, offline fallback, and a clean layered architecture.

> Built as a technical challenge. The acquirer integration is simulated via `FakePaymentService`, making the live API swap a single-line change in the DI module with no other class affected.
 
---

## Demo

| Payment flow | Receipt | Reactive validation |
|---|---|---|
| _<img width="283" height="623" alt="image" src="https://github.com/user-attachments/assets/ce0d9d45-aa8d-46e2-9062-a96f10e6a842" />_ | _<img width="283" height="623" alt="image" src="https://github.com/user-attachments/assets/29451b71-54ef-4bde-a3d1-43e3b01d2e87" />_ | _<img width="283" height="623" alt="Screen Recording 2026-04-28 at 18 12 21" src="https://github.com/user-attachments/assets/5edfb2e4-e57f-4b9f-b229-20b3c55c50cb" />_ |
 
---

## Architecture

The project follows **Clean Architecture** with three strict layers. The dependency rule is enforced throughout: **arrows always point inward toward Domain**. No outer layer is ever imported by an inner one.

<img width="727" height="673" alt="image" src="https://github.com/user-attachments/assets/36ceb187-cd48-4410-9c6d-502f5df03b6d" />


```
┌──────────────────────────────────────────────────────────────┐
│                       PRESENTATION                           │
│                                                              │
│   PaymentFragment                                            │
│        │  observes StateFlow<UiState>                        │
│        ▼                                                     │
│   PaymentViewModel ─────────────► ProcessPaymentUseCase      │
│        │  emits typed states (no Context, no R.string)       │
│        ▼                                                     │
│   PaymentResources.kt  ◄── resolves strings from R.string    │
└───────────────────────────┬──────────────────────────────────┘
                            │ depends on interfaces only
┌───────────────────────────▼──────────────────────────────────┐
│                         DOMAIN                               │
│                                                              │
│   ProcessPaymentUseCase                                      │
│        │  selects via Map<PaymentMethod, Strategy>           │
│        ▼                                                     │
│   PaymentStrategy  (Credit / Debit / Pix / Voucher)          │
│        │  enforces business rules per method                 │
│        ▼                                                     │
│   PaymentRepository  (interface only)                        │
│                                                              │
│   Pure Kotlin — zero Android / library imports               │
└───────────────────────────┬──────────────────────────────────┘
                            │ implements
┌───────────────────────────▼──────────────────────────────────┐
│                          DATA                                │
│                                                              │
│   PaymentRepositoryImpl                                      │
│        ├──► PaymentService ──► FakePaymentService (dev)      │
│        ├──► PaymentDao     ──► Room (local cache)            │
│        └──► PaymentDataMapper  (DTO ↔ Entity ↔ Domain)       │
└──────────────────────────────────────────────────────────────┘
```
 
---

## Features

- **Four payment methods** — Credit (with installments), Debit, Pix, and Voucher
- **Real-time currency formatting** — amount field formats as BRL on every keystroke via `MoneyFormatter` (pure Kotlin, no Android dependency, fully unit-tested)
- **Reactive field validation** — installment count and global amount boundaries update inline as the operator types; per-method transaction limits (Pix R$50k, Debit R$10k, Voucher R$1k) are enforced by the strategy and surface as a card error after submit
- **Installment preview** — per-installment value updates live (e.g. `12x R$ 50.00`) before the operator confirms
- **Offline fallback** — every authorized transaction is cached in Room; `getTransactionStatus` falls back to the local cache when the network is unavailable
- **Transaction cancellation** — approved transactions can be reversed from the receipt screen with a confirmation dialog
- **Typed UI states** — the ViewModel emits typed sealed states (`Idle`, `Loading`, `ValidationError`, `Success`, `Error`); the Fragment renders, never decides
- **Context-free ViewModel** — string resolution is delegated entirely to `PaymentResources.kt` extension functions called from the Fragment, keeping the ViewModel a pure unit-testable Kotlin class
---

## Tech stack

| Concern | Library |
|---|---|
| Language | Kotlin |
| UI | XML Views + View Binding + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Navigation | Jetpack Navigation + Safe Args |
| Async | Coroutines + StateFlow |
| Local DB | Room |
| Network | Retrofit + OkHttp + Gson |
| Testing | JUnit 4 + MockK + kotlinx-coroutines-test |
 
---

## Business rules

| Method | Rule |
|---|---|
| Credit | Installment plans (> 1x) require a minimum of **R$ 10.00** |
| Pix | Maximum **R$ 50,000.00** per transaction |
| Debit | Maximum **R$ 10,000.00** per transaction |
| Voucher | Maximum **R$ 1,000.00** per transaction |

Rule violations are modeled as typed `BusinessException` subclasses. The presentation layer resolves them to localized strings via `Throwable.toErrorString(context)` from `PaymentResources.kt`, keeping the ViewModel free of Android Context.
 
---

## Running the project

```bash
git clone <repo-url>
cd pos-payment
./gradlew assembleDebug
```

Requires Android Studio Hedgehog (2023.1.1) or later, API 24+ emulator or device.

> No API keys or external credentials needed — the project runs entirely on `FakePaymentService`.
 
---

## Running the tests

```bash
# All unit tests + Room integration tests (no emulator required)
./gradlew test
```

| Test class | What it covers |
|---|---|
| `CreditPaymentStrategyTest` | Installment minimum boundary conditions |
| `DebitPaymentStrategyTest` | Transaction limit boundary |
| `PixPaymentStrategyTest` | Transaction limit boundary |
| `VoucherPaymentStrategyTest` | Transaction limit boundary |
| `ProcessPaymentUseCaseTest` | Strategy routing, unsupported method handling |
| `CancelTransactionUseCaseTest` | Cancellation delegation |
| `GetTransactionStatusUseCaseTest` | Status check delegation |
| `PaymentInputValidatorTest` | Zero/negative/global max/installment count |
| `PaymentDataMapperTest` | DTO↔Domain↔Entity conversions, cents rounding |
| `PaymentRepositoryImplTest` | Caching on success, offline fallback, cancel guard |
| `FakePaymentServiceTest` | Fake service contract stability |
| `MoneyFormatterTest` | BRL formatting, thousand separators, edge cases |
| `PaymentViewModelTest` | Full UiState machine, typed validation, confirm enablement |
| `PaymentDaoTest` | Room persistence, REPLACE-on-conflict, ordering _(in-memory DB)_ |

The `PaymentDaoTest` uses `Room.inMemoryDatabaseBuilder` — no mocks, no emulator. It proves that transactions are actually persisted, status updates overwrite correctly, and `getAll()` returns records in descending timestamp order.
 
---

## Key architectural decisions

### Why is `PaymentRepository` defined in the Domain layer?
The Domain dictates the contract it needs; the Data layer fulfills it. This is the Dependency Inversion Principle — high-level policy never depends on low-level detail. Swapping Retrofit for Ktor requires zero Domain changes.

### Why does the Strategy map live in the DI module?
If `ProcessPaymentUseCase` instantiated strategies directly, it would import concrete classes and defeat the pattern. Hilt assembles `Map<PaymentMethod, PaymentStrategy>` at compile time via `@IntoMap`. Adding a new payment method is a **three-file change**: new enum value, new strategy class, one line in `PaymentModule`. No existing class is modified.

### Why does `PaymentService` exist separately from `PaymentApi`?
`PaymentApi` is the raw Retrofit contract. `PaymentService` owns the translation of `Response<T>` into `Result<T>`, keeping `PaymentRepositoryImpl` free of HTTP status-code logic. Each class has a single reason to change.

### Why does the ViewModel have no Context?
A ViewModel that calls `context.getString()` is harder to unit-test and subtly couples business logic to Android resources. The ViewModel emits **typed results** (`AmountValidationResult`, `InstalmentsValidationResult`, `Throwable`). The Fragment resolves these to localized strings using extension functions in `PaymentResources.kt`. Tests assert on types, not on locale-specific strings — and no `mockk<Context>` is needed.

### Why does `PaymentUiMapper` receive labels as parameters?
For the same reason — the mapper formats data (currency, date, installment breakdown) but carries no Android dependency. Label strings for enums are resolved by the Fragment via `PaymentMethod.labelRes()` and passed into `toUiModel()`. The mapper is pure Kotlin and KMP-ready.

### Why are per-method transaction limits defined only in the strategies?
There is exactly one source of truth for each business rule. The `PaymentInputValidator` handles global boundaries (zero, negative, R$99,999.99 terminal max, 12-installment cap). Per-method limits — Pix R$50k, Debit R$10k, Voucher R$1k — live exclusively in the corresponding `PaymentStrategy`. The validator does not duplicate them. If a limit changes, exactly one class changes.
 
---

## Project structure

```
app/src/main/java/com/nubiaferr/pospayment/
│
├── domain/                         # Pure Kotlin — zero Android / library imports
│   ├── exception/                  # Sealed BusinessException hierarchy
│   ├── model/                      # Payment, Transaction, PaymentMethod, TransactionStatus
│   ├── repository/                 # PaymentRepository interface
│   ├── strategy/                   # PaymentStrategy + 4 implementations
│   ├── usecase/                    # ProcessPayment, CancelTransaction, GetTransactionStatus
│   └── validation/                 # PaymentInputValidator + typed result sealed classes
│
├── data/
│   ├── di/                         # NetworkModule, PaymentModule, PaymentMethodKey
│   ├── local/                      # Room — PaymentDatabase, PaymentDao, TransactionEntity
│   ├── mapper/                     # PaymentDataMapper (DTO ↔ Entity ↔ Domain)
│   ├── remote/                     # PaymentApi (Retrofit), PaymentService interface, FakePaymentService
│   └── repository/                 # PaymentRepositoryImpl
│
└── presentation/
    ├── fragment/                   # PaymentFragment, ReceiptFragment
    ├── mapper/                     # PaymentUiMapper (Transaction → TransactionUiModel)
    ├── model/                      # TransactionUiModel (Parcelable)
    ├── uistate/                    # PaymentUiState sealed class
    ├── util/                       # MoneyTextWatcher, MoneyFormatter, PaymentResources
    └── viewmodel/                  # PaymentViewModel
```
 
---

## Intentional simplifications:
- **No real API.** `FakePaymentService` simulates the acquirer. `PaymentApi` and the Retrofit `PaymentService` wrapper are fully implemented — swapping them in is one line in `NetworkModule`.
- **No card reader SDK.**
- **No operator authentication.** Session management is out of scope.
  **Implemented but dormant — product scope, not demo scope:**

Some pieces of the codebase are fully built but not exercised in this demo. They exist because they represent realistic product scope for a real POS terminal, not because the challenge required them:

| Component | Why it exists | Why it's dormant in this demo |
|---|---|---|
| `GetTransactionStatusUseCase` | Polls a pending Pix until the QR is confirmed (`PENDING` → `APPROVED`) | `FakePaymentService` always returns `approved` synchronously — there is no pending state to poll |
| `PaymentDao.getAll()` | Backs a transaction history screen ordered by recency | No history UI in this demo |
| `CancelTransactionUseCase` | Calls the acquirer reversal endpoint | Wired end-to-end and fully tested; `FakePaymentService` always succeeds |
 
---

## Modularization approach

This project is intentionally a **single-module app**. For a challenge of this scope, splitting into Gradle modules would add build complexity without meaningful benefit.

That said, the package structure mirrors a multi-module layout exactly — if the project were to grow, the extraction would be mechanical with zero refactoring:

```
:domain        →  domain/           (pure Kotlin, no Android — extract as-is)
:data          →  data/             (Room, Retrofit, DI)
:presentation  →  presentation/     (Fragments, ViewModels, UI)
:app           →  App, MainActivity, nav_graph
```

The reason this works cleanly is the dependency rule already enforced in code: `domain` imports nothing from `data` or `presentation`. `PaymentUiMapper`, `MoneyFormatter`, and the validation layer are also already free of Android dependencies, making them candidates for a shared `:core` module or `commonMain` in a KMP setup.

## What I'd prioritize next:

1. **Compose Multiplatform / KMP migration** — the Domain layer and `PaymentUiMapper` are already free of Android dependencies; `domain/` and `data/remote/dto/` can move to `commonMain` as-is. See KMP readiness table below.
2. **Pix polling** — a real Pix flow requires polling until the QR code is confirmed; I'd implement this as a `Flow`-based loop with exponential backoff inside `GetTransactionStatusUseCase`.
3. **Transaction history screen** — `PaymentDao.getAll()` is already implemented and ordered; the UI is the missing piece.
4. **UI tests** — Espresso or Maestro end-to-end flows for the payment and cancellation paths.
5. **CI/CD** — GitHub Actions running `./gradlew test` on every PR, with lint and detekt.
6. **ProGuard rules** — `isMinifyEnabled = false` is fine for a demo; production needs rules to preserve Retrofit and Room models.
7. **Crash reporting** — Firebase Crashlytics integration for production observability.
---

## KMP readiness

| Component | Ready today? | Notes |
|---|---|---|
| `domain/` — models, use cases, strategies, exceptions, validation | ✅ Yes | Zero Android deps — move to `commonMain` as-is |
| `PaymentService` interface | ✅ Yes | No Retrofit imports |
| `PaymentDataMapper` | ✅ Yes | Pure Kotlin |
| `data/remote/dto/` | ✅ Yes | Data classes only |
| `MoneyFormatter` | ✅ Yes | Pure Kotlin, no Android |
| `PaymentUiMapper` | ✅ Yes | Pure Kotlin — Context removed |
| `PaymentRepositoryImpl` | ⚠️ Mostly | Depends on Room → swap for SQLDelight in `commonMain` |
| `PaymentViewModel` | ⚠️ Mostly | Uses `androidx.lifecycle` → use KMP Lifecycle artifact |
| `PaymentFragment` / `ReceiptFragment` | ❌ Android-only | Replace with Compose Multiplatform for iOS parity |

**Migration path:** extract `domain/` and `data/remote/dto/` to a `shared` KMP module first, then migrate persistence to SQLDelight, then adopt Compose Multiplatform for the UI layer.
 
---

## Author

[Nubia Ferreira](https://www.linkedin.com/in/nubia-ferreira/)

