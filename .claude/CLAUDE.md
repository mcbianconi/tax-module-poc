# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A tax calculation system POC implementing bitemporality with Domain-Driven Design (DDD) in Kotlin. The system calculates Brazilian taxes (ICMS, ISS, PIS, COFINS, CSLL, PCC) based on order items, actor tax info, and bitemporal validity (valid time + transaction time).

## Build & Test Commands

```bash
# Build the project
./gradlew build

# Run all tests
./gradlew test

# Clean build
./gradlew clean build
```

## Architecture

DDD-inspired structure with Spring-managed components:

- **core/order/**: Order domain model
  - `Order.kt`: Order entity with items

- **core/tax/**: Tax calculation domain
  - `ActorTaxInfo.kt`: Tax information for actors (entities)
  - `TaxType.kt`: Tax type enum (PIS, COFINS, CSLL, PCC, ISS, ICMS)
  - `TaxCalculatorRegistry.kt`: Spring component for bitemporal calculator lookup
  - **calculator/**: Tax calculator implementations
    - `TaxCalculator.kt`: Interface for all calculators
    - `PisCalculator.kt`: PIS (Pis2018 component)
    - `CofinsCalculator.kt`: COFINS (Confins2025 component)
    - `CsllCalculator.kt`: CSLL (Csll2025 component)
    - `PccCalculator.kt`: PCC composite (Pcc2025 component - sums PIS+COFINS+CSLL)
    - `IssCalculator.kt`: ISS calculator

- **containers/api/**: Spring Boot application entry point
  - `TaxApi.kt`: Main application class with `@SpringBootApplication`

## Key Concepts

### Bitemporality

The system uses **two time dimensions**:

1. **Valid Time** (`validFrom`/`validTo`): When information is valid in the real world
2. **Transaction Time** (`recordedAt`): When the system learned about this information

**Critical behavior**: Tax calculations use rules known at `calculationDate` (transaction time) but valid for `order.date` (valid time). This allows:
- Calculating old orders with current knowledge
- Recalculating with rules discovered later
- Historical accuracy and auditability

Extension function `TaxCalculator.isValidInBitemporalPeriod(validAt, knownAt)` checks both dimensions.

### Tax Calculator Registry Lookup

`TaxCalculatorRegistry.findCalculator(taxType, validAt, knownAt)`:
1. Groups all calculators by tax type (auto-injected by Spring)
2. Filters calculators valid in bitemporal period
3. Returns the one with the **most recent** `recordedAt` (latest knowledge)

Located in: core/tax/TaxCalculatorRegistry.kt:15

**Spring DI**: The registry receives `List<TaxCalculator>` via constructor injection, automatically collecting all `@Component` annotated calculators.

### PCC Composite Calculator

PCC (PIS + COFINS + CSLL) is implemented as a **composite calculator** in `Pcc2025`:
1. Uses `@Lazy` injection of `TaxCalculatorRegistry` to avoid circular dependency
2. At calculation time, looks up current PIS, COFINS, and CSLL calculators via registry
3. Sums the three component taxes
4. Returns `BelowThreshold` if sum < threshold, otherwise `Applicable`

This ensures PCC **always uses the correct bitemporal versions** of its components.

Located in: core/tax/calculator/PccCalculator.kt:15

### Calculator Implementations

All calculators are Spring `@Component` beans with bitemporal metadata:
- **Pis2018**: rate=0.06, valid from 2025-01-01
- **Confins2025**: rate=0.005, valid from 2025-01-01
- **Csll2025**: rate=0.005, valid from 2025-01-01
- **Pcc2025**: composite (sums PIS+COFINS+CSLL), threshold=10.00

Each calculator implements:
- `isApplicable(item, actorTaxInfo)`: Checks eligibility (may be simplified for composite components)
- `calculate(item, actorTaxInfo, orderDate, calculationDate)`: Returns `TaxCalculationResult`
- Bitemporal fields: `validFrom`, `validUntil`, `recordedAt`

## Testing

### Unit Tests

`PccBitemporalityTest` validates bitemporal behavior using Spring DI:
- Uses `@SpringBootTest` with `@TestConfiguration`
- Injects production calculators (`Pis2018`, `Confins2025`, `Csll2025`, `Pcc2025`)
- Defines test-only beans for historical periods (`pis2020`, `cofins2020`, `csll2020`)
- Verifies registry selects correct calculator version based on order date

Located in: src/test/kotlin/br/com/quintoandar/tax/core/tax/calculator/PccBitemporalityTest.kt

## Adding New Tax Calculator

1. Create calculator class in `core/tax/calculator/`
2. Implement `TaxCalculator` interface
3. Annotate with `@Component` for Spring auto-registration
4. Define bitemporal metadata (`validFrom`, `validUntil`, `recordedAt`)
5. Implement `isApplicable()` and `calculate()` methods
6. Spring automatically registers it in `TaxCalculatorRegistry` via DI

### Adding Historical Calculator Versions

To add a calculator for a different time period (e.g., `Pis2020` before `Pis2018`):
1. Create new `@Component` class with different validity period
2. Set `validFrom`/`validUntil` to define the valid time range
3. Set `recordedAt` to indicate when this version was known
4. Both versions coexist; registry selects based on bitemporal query
