import java.util.*;

/** Scheduling without console I/O. Input processes are copied for every run. */
public final class Scheduler {
    private Scheduler() {}
    public enum Algorithm { FCFS, SJF, SRTF, RR }
    /** Half-open interval [start, end); null pid denotes an idle CPU. */
    public record Slice(String pid, int start, int end) {}
    /** Process rows are independent, caller-owned snapshots in input order. */
    public record Result(String algorithm, List<Process> processes, List<Slice> slices) {
        public double averageWaiting() { return processes.stream().mapToDouble(p -> p.waitingTime).average().orElseThrow(); }
        public double averageTurnaround() { return processes.stream().mapToDouble(p -> p.turnTime).average().orElseThrow(); }
    }
    public static Result schedule(Process[] input, Algorithm algorithm, int quantum) {
        if (algorithm == null) throw new IllegalArgumentException("Algorithm is required");
        if (algorithm == Algorithm.RR && quantum <= 0) throw new IllegalArgumentException("Quantum must be positive");
        Process[] p = copy(input);
        List<Slice> slices = new ArrayList<>();
        Integer[] arrivals = new Integer[p.length];
        for (int i = 0; i < p.length; i++) arrivals[i] = i;
        // Stable sort: equal arrivals retain input order.
        Arrays.sort(arrivals, Comparator.comparingInt(i -> p[i].arrivalTime));
        int time = 0;
        if (algorithm == Algorithm.FCFS) {
            for (int i : arrivals) {
                if (time < p[i].arrivalTime) {
                    add(slices, null, time, p[i].arrivalTime, false);
                    time = p[i].arrivalTime;
                }
                p[i].startTime = time;
                int end = advance(time, p[i].burstTime);
                add(slices, p[i].pid, time, end, false);
                time = end;
                finish(p[i], time);
            }
        } else if (algorithm == Algorithm.RR) {
            Deque<Integer> ready = new ArrayDeque<>();
            int next = 0, completed = 0;
            while (completed < p.length) {
                if (ready.isEmpty() && next < p.length && time < p[arrivals[next]].arrivalTime) {
                    int arrival = p[arrivals[next]].arrivalTime;
                    add(slices, null, time, arrival, false);
                    time = arrival;
                }
                while (next < p.length && p[arrivals[next]].arrivalTime <= time) ready.addLast(arrivals[next++]);
                int idx = ready.removeFirst();
                Process proc = p[idx];
                if (proc.remTime == proc.burstTime) proc.startTime = time;
                int exec = Math.min(quantum, proc.remTime);
                int end = advance(time, exec);
                add(slices, proc.pid, time, end, false);
                time = end;
                proc.remTime -= exec;
                // Arrivals at the boundary enter before the expired process.
                while (next < p.length && p[arrivals[next]].arrivalTime <= time) ready.addLast(arrivals[next++]);
                if (proc.remTime > 0) ready.addLast(idx);
                else { finish(proc, time); completed++; }
            }
        } else {
            int completed = 0;
            while (completed < p.length) {
                int idx = -1;
                for (int i = 0; i < p.length; i++) {
                    if (p[i].remTime > 0 && p[i].arrivalTime <= time &&
                        (idx == -1 || key(p[i], algorithm) < key(p[idx], algorithm))) idx = i;
                }
                if (idx == -1) {
                    int next = Integer.MAX_VALUE;
                    for (Process proc : p) if (proc.remTime > 0 && proc.arrivalTime > time) next = Math.min(next, proc.arrivalTime);
                    add(slices, null, time, next, false);
                    time = next;
                    continue;
                }
                Process proc = p[idx];
                if (proc.remTime == proc.burstTime) proc.startTime = time;
                int exec = proc.remTime;
                if (algorithm == Algorithm.SRTF) {
                    // Selection changes only at an arrival or completion.
                    for (Process candidate : p) if (candidate.remTime > 0 && candidate.arrivalTime > time)
                        exec = Math.min(exec, candidate.arrivalTime - time);
                }
                int end = advance(time, exec);
                add(slices, proc.pid, time, end, algorithm == Algorithm.SRTF);
                time = end;
                proc.remTime -= exec;
                if (proc.remTime == 0) { finish(proc, time); completed++; }
            }
        }
        String label = algorithm == Algorithm.RR ? "Round Robin (" + quantum + "ms)" : algorithm.name();
        return new Result(label, List.copyOf(Arrays.asList(p)), List.copyOf(slices));
    }
    private static int key(Process p, Algorithm algorithm) { return algorithm == Algorithm.SJF ? p.burstTime : p.remTime; }
    private static void finish(Process p, int time) {
        p.remTime = 0;
        p.completionTime = time;
        p.turnTime = time - p.arrivalTime;
        p.waitingTime = p.turnTime - p.burstTime;
    }
    private static int advance(int time, int duration) {
        if ((long) time + duration > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeline exceeds 32-bit integer range");
        return time + duration;
    }
    private static void add(List<Slice> slices, String pid, int start, int end, boolean merge) {
        if (merge && !slices.isEmpty()) {
            Slice last = slices.get(slices.size() - 1);
            if (Objects.equals(last.pid(), pid) && last.end() == start) {
                slices.set(slices.size() - 1, new Slice(pid, last.start(), end));
                return;
            }
        }
        slices.add(new Slice(pid, start, end));
    }
    static Process[] copy(Process[] input) {
        if (input == null || input.length == 0) throw new IllegalArgumentException("At least one process is required");
        Set<String> ids = new HashSet<>();
        Process[] copy = new Process[input.length];
        for (int i = 0; i < input.length; i++) {
            Process p = input[i];
            if (p == null || p.pid == null || !p.pid.matches("[A-Za-z0-9_-]+") || p.pid.equals("IDLE") || !ids.add(p.pid))
                throw new IllegalArgumentException("Process IDs must be unique letters/digits/underscore/hyphen; IDLE is reserved");
            if (p.arrivalTime < 0 || p.burstTime <= 0) throw new IllegalArgumentException("Arrival must be nonnegative and burst must be positive");
            copy[i] = new Process(p.pid, p.arrivalTime, p.burstTime);
        }
        return copy;
    }
}
