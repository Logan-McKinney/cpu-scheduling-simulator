# Change report

Original behavior was inspected in both `Process.java` and `scheduling_algorithms.java`. `Process` is a mutable record of input and computed integer times; it initializes remaining time from burst but performs no validation. Each original algorithm copies process inputs before scheduling. Existing waiting/turnaround formulas were correct on the normal hand-calculated workload; the defects were ordering, progress, validation, and chart representation.

| Confirmed before | After | Regression evidence |
|---|---|---|
| FCFS executes input sequence, including waiting for a later first row while an earlier process is ready | Stable arrival sort; equal arrivals retain input order | `fcfs-order` failed before, passes after |
| RR uses a cumulative `n * 10` array and never reclaims consumed slots | `ArrayDeque` with one-time chronological admission and unfinished-process requeue | `rr-capacity`: one 1,000-unit process, q=1, threw at array index 10 before |
| RR exits as soon as its queue empties, leaving future arrivals unfinished | Continue until every process completes, jumping to the next arrival when idle | `rr-late`, `idle-rr` |
| RR admits arrivals within a slice in input order | Admit by actual arrival, then input order; boundary arrivals precede expired process | `rr-arrival-order`: C arriving at 1 now precedes B arriving at 2 during q=3 |
| Charts omit idle intervals; SRTF transition strings can be ambiguous | Structured half-open intervals with explicit `IDLE`, consistent boundary rendering | Four `idle-*` cases; complete timeline oracle comparisons |
| Zero SRTF burst never completes; empty input produces NaN; negatives accepted; q=0 fails by queue overflow | Validate centrally with `IllegalArgumentException`; CLI reports error with exit 1 | `zero` timed out before; all invalid regression cases now pass |
| Integer time overflow silently wraps | Check each clock advance and reject completion outside signed 32-bit range | `overflow`, eight large-time checks |
| SJF/SRTF increment across idle gaps; SRTF scans each time unit | Jump idle gaps; SRTF evaluates at arrivals/completions with the same tie rule | Oracle matches 1,400 schedules; billion-unit idle and maximum-burst boundaries pass |

The original `Process.java` is unchanged. Scheduling was extracted into one `Scheduler` class to make timelines and statistics testable without parsing console output; the original algorithm entry points remain as printing wrappers. Input-order ties for SJF/SRTF are intentionally preserved. The original queue-membership helper became unnecessary after one-time admission. File-based input, comparison output, reproducible workload files, and an execution-derived SVG/Markdown generator were added. No scheduling framework or third-party dependency was introduced.

The 23 regression tests were written and executed against the preserved original sources before changes: **9 passed, 14 failed**. The same test file then passed **23/23** against the fixed implementation. Later verification added **1,400 oracle comparisons**, **41 input validation checks**, and **eight large-time checks**, all passing. Raw evidence and exact commands are linked from `VERIFICATION.md`.

All eight original files match preserved copies by SHA-256. The original Downloads directory was not modified. No push, publication, or public license was performed. Coursework publication eligibility remains a prerequisite for release.

Subsequently, the owner authorized a private GitHub repository. The working project is now prepared for private hosting; the public-release eligibility prerequisite remains unchanged.
