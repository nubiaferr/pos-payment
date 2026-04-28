# POS Payment — Android Technical Challenge

A production-grade Android POS payment module built with Clean Architecture, MVVM, Strategy pattern, Hilt, Coroutines/Flow, Retrofit, and Room.

---

## Architecture overview
<img width="1400" height="960" alt="image" src="https://github.com/user-attachments/assets/0e010c27-b85d-4dae-a4d2-8e54ec606cc3" />


The project is organized in three strict layers. The dependency rule is enforced throughout: **arrows always point inward toward Domain**. No layer imports anything from a layer further out.

```
┌─────────────────────────────────────────────┐
│              PRESENTATION                   │
│   Fragment  ──►  ViewModel  ──►  UseCase    │
│        (StateFlow<UiState>)                 │
└────────────────────┬────────────────────────┘
                     │ depends on
┌────────────────────▼────────────────────────┐
│                  DOMAIN                     │
│   UseCase  ──►  Strategy  ──►  Repository   │
│              (interface only)               │
│          Pure Kotlin — zero Android deps    │
└────────────────────┬────────────────────────┘
                     │ implements
┌────────────────────▼────────────────────────┐
│                   DATA                      │
│   RepositoryImpl  ──►  Service  ──►  API    │
│                   └──►  DAO  ──►  Room      │
│                   └──►  Mapper              │
└─────────────────────────────────────────────┘
```

---

## Design patterns

### Strategy pattern

Each payment method (Credit, Debit, Pix, Voucher) has a dedicated `PaymentStrategy` implementation that encapsulates its own business rules:

| Strategy | Rule enforced |
|---|---|
| `CreditPaymentStrategy` | Instalments require amount ≥ R$10.00 |
| `PixPaymentStrategy` | Single transaction limit of R$50,000.00 |
| `DebitPaymentStrategy` | No extra rules — acquirer handles PIN/balance |
| `VoucherPaymentStrategy` | No extra rules — routed to voucher network |

The `ProcessPaymentUseCase` receives a `Map<PaymentMethod, PaymentStrategy>` injected by Hilt and selects the correct strategy at runtime — no `when` branching on the payment method. Adding a new method requires only a new strategy class and a single line in `PaymentModule`.

### Repository pattern with offline-first fallback

`PaymentRepositoryImpl` coordinates between the remote API (`PaymentService`) and local storage (`PaymentDao`). Every authorised transaction is cached in Room. If the network is unavailable, `getTransactionStatus` falls back to the local cache — critical for POS environments with unstable connectivity.

---

## Project structure

```
com.btg.pos/
│
├── domain/                         # Pure Kotlin — no Android/library imports
│   ├── model/
│   │   ├── Payment.kt
│   │   ├── Transaction.kt
│   │   ├── PaymentMethod.kt
│   │   └── TransactionStatus.kt
│   ├── exception/
│   │   └── BusinessException.kt    # Sealed hierarchy of domain rule violations
│   ├── repository/
│   │   └── PaymentRepository.kt    # Interface — implemented in data layer
│   ├── strategy/
│   │   ├── PaymentStrategy.kt      # Interface
│   │   ├── CreditPaymentStrategy.kt
│   │   ├── DebitPaymentStrategy.kt
│   │   ├── PixPaymentStrategy.kt
│   │   └── VoucherPaymentStrategy.kt
│   └── usecase/
│       ├── ProcessPaymentUseCase.kt
│       ├── CancelTransactionUseCase.kt
│       └── GetTransactionStatusUseCase.kt
│
├── data/
│   ├── remote/
│   │   ├── service/
│   │   │   ├── PaymentApi.kt       # Retrofit interface
│   │   │   └── PaymentService.kt   # Wraps API calls in Result<T>
│   │   └── dto/
│   │       └── PaymentDtos.kt
│   ├── local/
│   │   ├── PaymentDatabase.kt      # Room database
│   │   ├── dao/PaymentDao.kt
│   │   └── entity/TransactionEntity.kt
│   ├── mapper/
│   │   └── PaymentDataMapper.kt    # DTO ↔ domain entity
│   ├── repository/
│   │   └── PaymentRepositoryImpl.kt
│   └── di/
│       ├── PaymentMethodKey.kt     # Custom Hilt @MapKey
│       ├── PaymentModule.kt        # Binds strategies + repository
│       └── NetworkModule.kt        # Provides Retrofit, OkHttp, Room
│
└── presentation/
    ├── fragment/
    │   ├── PaymentFragment.kt
    │   └── ReceiptFragment.kt
    ├── viewmodel/
    │   └── PaymentViewModel.kt
    ├── uistate/
    │   └── PaymentUiState.kt       # Idle / Loading / AwaitingCard / Success / Error
    ├── model/
    │   └── TransactionUiModel.kt   # Pre-formatted strings for the UI
    └── mapper/
        └── PaymentUiMapper.kt      # Transaction → TransactionUiModel
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Fragments + View Binding |
| State management | `StateFlow` + `repeatOnLifecycle` |
| Dependency injection | Hilt |
| Async | Coroutines |
| Networking | Retrofit + OkHttp |
| Local storage | Room |
| Testing | JUnit 4 + MockK + `kotlinx-coroutines-test` |

---

## Running the tests

```bash
./gradlew test
```

Test coverage includes:

- `CreditPaymentStrategyTest` — instalment rule boundary conditions
- `PixPaymentStrategyTest` — Pix transaction limit boundary conditions
- `ProcessPaymentUseCaseTest` — strategy selection and unsupported method handling
- `PaymentViewModelTest` — UiState emission sequence for success, business error, and network error

---

## Key architectural decisions

**Why is the `PaymentRepository` interface in the Domain layer?**
The Domain dictates the contract it needs. The Data layer implements it. This is the Dependency Inversion Principle — high-level policy does not depend on low-level detail. Swapping Retrofit for another HTTP client requires zero changes to the Domain or Presentation.

**Why does the Strategy map live in the DI module and not in the UseCase?**
If the UseCase instantiated strategies directly, it would import concrete classes — creating coupling that defeats the pattern. Hilt assembles the `Map<PaymentMethod, PaymentStrategy>` at compile time via `@IntoMap`. The UseCase only ever sees the `PaymentStrategy` interface.

**Why is `PaymentUiMapper` in the Presentation layer and not in the Domain?**
Formatting is a UI concern. Currency formatting (locale, symbol), date patterns, and label translations belong to the Presentation. If the app ever supports multiple locales, only `PaymentUiMapper` changes.

**Why does `PaymentService` exist separately from `PaymentApi`?**
`PaymentApi` is the raw Retrofit contract. `PaymentService` owns the translation of `Response<T>` into `Result<T>`, keeping `PaymentRepositoryImpl` free of HTTP status code logic. Each class has one reason to change.

---

## Adding a new payment method

1. Add the value to `PaymentMethod` enum.
2. Create `NewMethodPaymentStrategy : PaymentStrategy` in `domain/strategy/`.
3. Add a `@Binds @IntoMap @PaymentMethodKey(PaymentMethod.NEW_METHOD)` entry in `PaymentModule`.
4. Add the corresponding method to `PaymentRepository` interface and implement it in `PaymentRepositoryImpl`.

No existing class needs to be modified. ✓
