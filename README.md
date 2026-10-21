# CPU Scheduling Simulator

A dependency-free Java console simulator for FCFS, non-preemptive SJF, preemptive SRTF, and Round Robin with multiple time quanta. Based on Logan McKinney's October 2025 program. The original class names, process model, algorithms, and interactive workflow are retained.

![Executed scheduling comparison](docs/gantt.svg)

The [generated example](docs/example.md) contains per-process statistics and text Gantt charts. The [change report](docs/CHANGE_REPORT.md) explains confirmed defects and verification evidence.

## Setup and running

Use a **JDK 17 or newer**, with `java` and `javac` on PATH or `JAVA_HOME` set to the JDK directory. No Maven, Gradle, network access, or external test dependencies are needed.

Windows PowerShell, from this project directory:

```powershell
.\build.ps1 test
.\build.ps1 run
.\build.ps1 run -Workload workloads/comparison.txt -Quanta 1,2,5,10
.\build.ps1 report
```

If a JDK is installed outside PATH, supply `-JavaHome 'C:\path\to\jdk'` to any command. The verified machine used `C:\Program Files\JetBrains\PyCharm 2024.3.2\jbr`.

On a POSIX shell:

```sh
sh build.sh test
sh build.sh run
sh build.sh run workloads/comparison.txt 1 2 5 10
sh build.sh report
```

The POSIX script is provided for portability; it was not executed on Linux/macOS during this verification. Both scripts compile with `--release 17` into `build/classes`. A direct JDK invocation is also supported:

```sh
javac --release 17 -d build/classes src/*.java tests/*.java tools/*.java
java -cp build/classes RegressionTests
java -cp build/classes EngineTests
java -cp build/classes scheduling_algorithms workloads/comparison.txt 1 2 5 10
java -cp build/classes GenerateReport
```

Interactive mode asks for a process count, then ID, arrival, and burst for each process. It runs all four algorithms, using Round Robin quanta 5 and 10 by default. File mode accepts one `ID arrival burst` triple per line, with blank lines and `#` comments allowed. Optional trailing integers replace the default quanta. Invalid input prints `Input error: ...` to stderr and exits with status 1.

## Scheduling policies

| Algorithm | Selection and tie policy |
|---|---|
| FCFS | Earliest arrival first; equal arrivals retain input order. Runs each process to completion. |
| SJF | At dispatch, choose the smallest burst among ready processes. Equal bursts use original input order, even if arrivals differ. No preemption. |
| SRTF | Choose the smallest remaining burst among ready processes. Equal remaining times use original input order, including the running process. A newly arrived earlier-input process can therefore preempt on an equal remainder. |
| Round Robin | FIFO ready queue using `ArrayDeque`. Admit by arrival time, then input order. Arrivals during a slice, including its endpoint, are queued before requeuing the expired process. |

SJF/SRTF input-order ties preserve the original implementation's scan policy. FCFS arrival order and Round Robin chronological admission correct confirmed defects. All algorithms jump across idle gaps and record explicit `IDLE` intervals. SRTF reconsiders at arrival/completion events instead of each millisecond; the selection rule is unchanged. Round Robin retains individual dispatches in its chart; SRTF merges adjacent intervals for the same process.

Intervals are half-open `[start, end)`. Start means the **first** execution time, not the most recent resumption. All result rows use input order so comparisons are straightforward.

## Statistics and executed example

For each process:

- Turnaround = completion - arrival.
- Waiting = turnaround - burst.
- Average waiting/turnaround = arithmetic mean across all processes.

Executed workload: A `(arrival 0, burst 5)`, C `(2, 1)`, B `(1, 2)`, D `(12, 3)`, in that input order. These values are copied from [actual console output](docs/comparison-output.txt); regenerate the Markdown/SVG using `report`.

| Algorithm | Average waiting (ms) | Average turnaround (ms) |
|---|---:|---:|
| FCFS | 2.250 | 5.000 |
| SJF | 2.000 | 4.750 |
| SRTF | 1.000 | 3.750 |
| RR, q=1 | 1.500 | 4.250 |
| RR, q=2 | 1.500 | 4.250 |
| RR, q=5 | 2.250 | 5.000 |
| RR, q=10 | 2.250 | 5.000 |

For example, FCFS runs `0 | A | 5 | B | 7 | C | 8 | IDLE | 12 | D | 15`. SRTF runs `0 | A | 1 | B | 2 | C | 3 | B | 4 | A | 8 | IDLE | 12 | D | 15`.

These are simulated workload statistics, not runtime benchmarks or a general ranking. In particular, changing input order can change SJF/SRTF ties.

## Tests and reproducibility

`test` compiles and runs two dependency-free Java test programs. Failures produce a nonzero exit code.

- `RegressionTests`: 23 named cases written before scheduler fixes, exercised through the original public console methods. Each case runs in a separate JVM with a four-second timeout so legacy infinite loops cannot stall the suite. Covers out-of-order FCFS, queue exhaustion, gaps/late arrivals, equal times, zero/negative/empty input, invalid quantum, long burst with q=1, all four metrics, within-slice arrival order, overflow, and input preservation.
- `EngineTests`: 200 small workloads from seed `20261021`, tested with FCFS/SJF/SRTF and RR q=1/2/5/10 (1,400 comparisons) against an independent unit-tick reference. Compares the full timeline, every metric, averages, interval continuity, and unchanged inputs. Also exercises 41 invalid-input cases and eight large-time boundaries.
- `workloads/comparison.txt`, `ties.txt`, and `long-burst.txt` are human-readable reproducible examples. The long-burst example should be run with q=1 to exceed the original fixed queue capacity.

Verified on **2026-10-21**, Windows PowerShell, JetBrains OpenJDK **21.0.5** with Java 17 compilation target:

| Suite | Before fixes | After fixes |
|---|---|---|
| Named regressions | 9/23 passed; 14 failed, including one timeout | 23/23 passed |
| Seeded reference comparisons | Not applicable to original console-only API | 1,400/1,400 matched |
| Additional invalid input / large-time checks | Not run on original | 41/41 and 8/8 passed |

See [baseline output](docs/baseline-tests.txt), [fixed output](docs/fixed-tests.txt), and [verification commands](docs/VERIFICATION.md). To reproduce the expected failing baseline, use `./build.ps1 baseline` or `sh build.sh baseline`. The two original Java sources are stored byte-for-byte in `baseline/`. Baseline compilation goes to `build/baseline`, separately from the fixed application.

## Structure and API

- `src/Process.java`: original small process/state model, unchanged.
- `src/Scheduler.java`: pure scheduling entry point `Scheduler.schedule(input, algorithm, quantum)`; returns process rows, intervals, and averages without printing. Quantum is used only for RR. Every invocation validates and copies the input; returned process rows belong to that invocation. Lists are unmodifiable, while the original mutable `Process` model is retained.
- `src/scheduling_algorithms.java`: interactive/file input, console formatting, and original `fcfs`, `sjf`, `srtf`, `rr`, and `copyArray` entry points.
- `tools/GenerateReport.java`: generates the Markdown tables and SVG charts from execution results.
- `baseline/`: unmodified original Java sources for regression reproduction, excluded from normal compilation.

The copied Eclipse metadata and old `bin/` files are ignored by Git and unused by the build. Use `build/classes` when running the improved simulator.

## Complexity

Let n be the process count, K = sum of ceil(burst/quantum) for RR, and S be the number of recorded intervals. Bounds include the complete timeline retained for output.

| Algorithm | Time | Space |
|---|---|---|
| FCFS | O(n log n), stable arrival sort | O(n + S) |
| SJF | O(n squared), scanning ready processes | O(n + S) |
| SRTF | O(n squared), scanning at arrival/completion events | O(n + S) |
| RR | O(n log n + K), sort then deque operations | O(n + K), including trace; ready queue O(n) |

All paths currently build the arrival ordering once, including SJF/SRTF; the sort does not affect their tie policy or quadratic bound. Gantt formatting is linear in the number of intervals, using `StringBuilder`.

## Assumptions and limitations

A single CPU executes one known CPU burst per process. There is no I/O, context-switch cost, priority, multicore scheduling, or stochastic arrival generation during a run. Times and quanta are integer simulated milliseconds. At least one process is required; arrivals must be nonnegative and bursts/quanta positive. Zero bursts are rejected rather than silently completed.

IDs must be unique, case-sensitive ASCII letters/digits/underscore/hyphen, with `IDLE` reserved. Null inputs/entries are rejected at the scheduling boundary. Completion beyond `Integer.MAX_VALUE` is rejected with a clear error rather than wrapping; a completion exactly at the limit is supported. Input sizes are bounded by available memory. In particular, long bursts with tiny RR quanta create many trace entries and console output; the dynamic queue fixes the fixed-capacity defect but does not make trace storage unlimited. The test timeout is a hang guard, not a speed benchmark.

This remains a small educational default-package Java project. It uses a plain JDK build and test harness; there is no GUI, packaging installer, or hosted CI claim. Result rows retain mutable fields for compatibility, so callers should not edit them while inspecting averages. All workloads are finite; starvation behavior under an infinite stream of short jobs is outside the model.

## Preservation and release status

The source project at `C:\Users\mckin\Downloads\scheduling_algorithms` was only read. Work was performed in a separate `cpu-scheduling-simulator` directory. [SHA-256 manifest](docs/original-manifest.csv) verifies all eight original files against the preserved copy (Java sources against `baseline/`, other files against their copied paths). Ignored Eclipse metadata and compiled originals remain in the local working copy and are not uploaded to GitHub.

**This project is maintained in a private GitHub repository at the owner's request. Coursework publication eligibility is unconfirmed. Before any public release, the owner must confirm the course/instructor policy permits publication and that they have the rights to share the submission.** No public-release license has been selected pending that confirmation.
