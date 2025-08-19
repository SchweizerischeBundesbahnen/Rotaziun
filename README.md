# Rotaziun

Rotaziun, provided by SBB (Swiss Federal Railways), is a linear program to solve the vehicle rostering problem. (Ger.: Strategische Rollmaterialeinsatzplanung)

#### Table Of Contents

- [Introduction](#introduction)
    - [Overview](#overview)
    - [Input/Output](#input-output)
- [Modules](#modules)
    - [Basic Problem](#basic-problem)
    - [One-Block](#one-block)
    - [Coupling Decoupling](#coupling-decoupling)
    - [Sidings](#sidings)
    - [Maintenance](#maintenance)
- [Input](#input)
    - [Internal Parameters](#internal-parameters)
    - [Default Values](#default-values)
- [Output](#output)
  - [Section Chains](#section-chains)
  - [Solution Status](#solution-status)
  - [Metrics](#metrics)
- [Getting Started](#Getting-Started)
- [Contributing](#Contributing)
- [Documentation](#Documentation)
- [Code of Conduct](#code-of-conduct)
- [Coding Standards](#coding-standards)
- [License](#License)

## Introduction

### Overview

// todo add over view text

![Rotations computed by Rotaziun](images/overview.png)

#### Advantages
- **Interactive Planner-Solver:** Enables seamless collaboration between the planner and the solver.
- **Integration of Constraints:** Easily integrates various operational constraints.
- **Fast Computation:** Optimized to deliver quick results.

#### Functionality

Rotaziun can handle constraints related to sidings, maintenance windows, and coupling.

### Input-Output

#### Input

- **Commercial Sections:** A list of commercial sections.
- **Maintenance Windows:** A list of maintenance windows.

#### Output

- **Rotations:** A list of vehicle rotations/ blocks (germ.: Umläufe).

#### Commercial Section

- **Departure & Arrival Time**
- **Departure & Arrival Station**
- **Vehicle Type**
- **Core/ Reinforcement (Stamm/ Verstärker)**

#### Maintenance Window

- **Time Window**
- **Location**

## Modules

The solver is built in a modular way. Depending on the required constraints, specific modules of the solver are activated.

![Modules of Rotaziun](images/modules.png)

### Basic Problem

#### Optimization Criteria

- **vehicle cost per day:** Cost per block day [s]
- **empty trip costs** Cost per empty trip per km [s]
- **cost per stamm change:** Cost incurred when a vehicle changes its service type from Core (Stamm) to Reinforcement (Enforcement) [s]
- **cost per siding:** Cost for placing a vehicle in a siding between two chained sections [s]
- **cost per debicode change:** Cost for changing debicode between two chained sections [s]

#### Constraints

- **min turn time:** Minimum time required between two chained sections [s]
- **min siding duration:** time after which a standstill is considered a siding [s]
- **required section chains:** Set of section pairs (specified by section objects) that must be chained directly together.
- **prohibited section chains:** Set of section pairs (specified by section objects) that must not be chained together.
- **prohibited empty trips map:** Map specifying, for a given station (by station ID), which other stations are not allowed for empty trips.
- **prohibited siding sites:** Set of stations (specified by station ID) where sidings are not allowed.

#### One-Block

- **onlyOneBlock:** Enforces the use of only one block.

### Coupling-Decoupling

#### Objective

- **cost per coupling/decoupling:** Cost for one coupling or decoupling of two vehicles [s]

#### Constraints

- **min time for decoupling:** Minimum time required for Reinforcement after decoupling from Core to continue with the next service [s]
- **min time for coupling:** Minimum time required for Reinforcement, after ending its previous service, to couple back to its Core [s]
- **prohibited coupling/decoupling stations:** Set of stations (specified by station ID) where coupling or decoupling is not allowed.

### Sidings

- **siding evaluation time:** Evaluation time at which siding capacity restrictions are checked [s]
- **siding capacity map:** Map specifying the maximum available siding length (in meters) for each station (by station ID)


### Maintenance

This module ensures that items are distributed evenly across the entire block.
- **maintenance window distribution tolerance:** A value in the range [0, 1] — where 0 means a perfectly even distribution and 1 allows for any placement.

## Input

### Internal-Parameters

- **precisionLevel:** Determines the trade-off between computation speed and solution quality → 0: fast solve, 1: high solution quality, 2: very high solution quality.
- **numOfWorkers:** CP-SAT optimizer can use parallel computing; numOfWorkers defines how many solver instances are used.

### Default-Values

If one of the input parameters is not set by the user, then the solver will automatically choose the default value for this parameter.
The solver works, even in none of the input parameters is set by the user. Then, all input parameters are set to their default value.
All sets and maps are set by default to be empty.

| Parameter                   | Value    |
|-----------------------------|----------|
| precisionLevel              | 0        |
| minTurnTime                 | 180      |
| vehicleCostPerDay           | 100*3600 |
| costPerStammChange          | 10*60    |
| costPerSiding               | 60       |
| costPerDebicodeChange       | 0        |
| onlyOneBlock                | true     |
| costForCouplingDecoupling   | 4*60     |
| minTimeForDecoupling        | 0        |
| minTimeForCoupling          | 0        |
| minSidingDuration           | 3*3600   |
| sidingEvaluationTime        | 3*3600   |

# Output

The output is stored in the Rotaziun Result Parameters.

## Section Chains

### sectionChainMap
- **Type:** Map&lt;MikadoSectionDto, MikadoSectionDto&gt;
- **Description:** Indicates for each section which one comes next.

### sidingBeforeEmptyTrip
- **Type:** Map&lt;MikadoSectionDto, Boolean&gt;
- **Description:** By default, if an empty trip takes place between two chained sections, the empty trip happens directly after the first section. If the `sidingBeforeEmptyTrip` attribute is true for one section, then the empty trip takes place briefly before the second section in the chain. The placement of the empty trip depends on the siding capacity.

## Solution Status

### resultStatus
- **Type:** ResultStatus
- **Description:** Indicates whether the found solution is optimal, only feasible, or if the problem was infeasible.

### solved
- **Type:** boolean
- **Description:** Indicates whether the solver could find any solution (feasible or optimal).

## Metrics

- **objectiveValue**
- **emptyTripDuration**
- **numOfStammChanges**
- **numOfBlockDays**
- **numOfDebicodeChanges**
- **numOfSidings**
- **numOfDecoupling**

## Documentation

Links to all relevant documentation files, including:

- [CODING_STANDARDS.md](CODING_STANDARDS.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [LICENSE.md](LICENSE.md)

<a id="License"></a>

## License

This project is licensed under the GPL (General Public License) .

<a id="Contributing"></a>

## Contributing

This repository includes a [CONTRIBUTING.md](CONTRIBUTING.md) file that outlines how to contribute
to the project, including how to submit bug reports, feature requests, and pull requests.

<a id="coding-standards"></a>

## Coding Standards

This repository includes a [CODING_STANDARDS.md](CODING_STANDARDS.md) file that outlines the coding
standards that you should follow when contributing to the project.

<a id="code-of-conduct"></a>

## Code of Conduct

This repository includes a [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) file.
