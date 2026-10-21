# Testing

Run `./build.ps1 test` in PowerShell or `sh build.sh test` with JDK 17+.

- **RegressionTests:** 23 cases covering arrival order, idle gaps, ties, Round Robin queue handling, metrics, invalid inputs, and input preservation. Each runs in a separate JVM with a four-second timeout.
- **EngineTests:** 200 workloads from seed `20261021`, each checked against an independent unit-tick reference for FCFS, SJF, SRTF, and Round Robin quanta 1/2/5/10: **1,400 comparisons** of timelines and metrics. Also includes 41 invalid-input and eight large-time checks.

The saved October 21, 2026 run passed all of these checks using OpenJDK 21 with Java 17 compilation target.

## Comparing the original implementation

`baseline/` preserves the original two Java sources. Run `./build.ps1 baseline` or `sh build.sh baseline` to compile them separately and run the same 23 regressions. The recorded baseline passed 9/23; its 14 failures illustrate issues such as arrival ordering, fixed queue capacity, and zero-burst handling. A nonzero exit code is expected. Normal builds use only `src/`.

Run the `report` task to regenerate `docs/example.md` and `docs/gantt.svg` from `workloads/comparison.txt`. These are scheduling statistics, not wall-clock benchmarks.
