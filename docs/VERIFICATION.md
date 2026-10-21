# Verification record

Date: 2026-10-21 (America/New_York). Working directory: the separate `cpu-scheduling-simulator` project. Runtime/compiler: JetBrains OpenJDK 21.0.5+8-631.30; compilation target Java 17.

Exact PowerShell commands used (paths relative to this project unless absolute):

```powershell
& 'C:\Program Files\JetBrains\PyCharm 2024.3.2\jbr\bin\java.exe' -version
.\build.ps1 baseline -JavaHome 'C:\Program Files\JetBrains\PyCharm 2024.3.2\jbr' *> docs\baseline-tests.txt
.\build.ps1 test -JavaHome 'C:\Program Files\JetBrains\PyCharm 2024.3.2\jbr' *> docs\fixed-tests.txt
& 'C:\Program Files\JetBrains\PyCharm 2024.3.2\jbr\bin\java.exe' -cp build/classes scheduling_algorithms workloads/comparison.txt 1 2 5 10 | Set-Content docs\comparison-output.txt
.\build.ps1 report -JavaHome 'C:\Program Files\JetBrains\PyCharm 2024.3.2\jbr'
```

The initial baseline was captured before fixes, using the first version of `build.ps1`, which compiled originals into `build/classes`. The finalized baseline task uses `build/baseline` to prevent stale-class interference. Both compile the same preserved sources and same 23 tests. Baseline returns status 1 as expected: 9/23 passed, 14 failed, with the zero-burst case stopped after four seconds. The fixed suite returns status 0: 23/23 regressions, 1,400/1,400 seeded timeline/metric comparisons, 41/41 additional invalid inputs, and 8/8 large-time checks.

`comparison-output.txt` is complete console output from the executed workload and quanta. `example.md` and `gantt.svg` are generated from the same workload and scheduler. The reported averages are simulation statistics; no wall-clock performance benchmark was run.

`original-manifest.csv` contains SHA-256 hashes for every original file and confirms equality with preserved copies. Original Java sources map to `baseline/`; the Eclipse metadata and compiled originals map to the corresponding copied paths.

The test harness launches one JVM per regression and captures its exit status; timeout failures kill the child. The oracle uses unit ticks while the production SRTF uses events. It compares all occupied/idle ticks and all per-process times. POSIX commands and other JDK versions are documented but were not executed here.

Additional CLI smoke checks passed: zero count, nonnumeric count, zero burst, malformed file, and q=0 each exited 1; a valid interactive run exited 0. Results are recorded in `cli-checks.txt`. These used the compiled main class via `java -cp build/classes scheduling_algorithms`, supplying either piped console input or workload arguments. The fixed build's file-mode command with `-Workload workloads/comparison.txt -Quanta 1,2,5,10` also ran successfully and generated the saved console output.

Regenerating the report with `java -cp build/classes GenerateReport` produced identical SHA-256 hashes for both output files. The SVG parsed successfully as XML (47 interval rectangles). The chart has not been checked in a graphical browser during this run.
