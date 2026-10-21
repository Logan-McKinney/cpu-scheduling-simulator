import java.util.*;

/** Seeded comparisons against an independent, deliberately slow unit-tick oracle. */
public class EngineTests {
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Random random = new Random(20261021L);
        int runs = 0;
        for (int sample = 0; sample < 200; sample++) {
            Process[] input = new Process[1 + random.nextInt(6)];
            for (int i = 0; i < input.length; i++) input[i] = new Process("P" + i, random.nextInt(13), 1 + random.nextInt(8));
            for (Scheduler.Algorithm alg : Scheduler.Algorithm.values()) {
                for (int q : alg == Scheduler.Algorithm.RR ? new int[]{1,2,5,10} : new int[]{1}) {
                    verify(input, alg, q, sample); runs++;
                }
            }
        }
        for (Scheduler.Algorithm alg : Scheduler.Algorithm.values()) {
            reject(() -> Scheduler.schedule(null,alg,1));
            reject(() -> Scheduler.schedule(new Process[0],alg,1));
            reject(() -> Scheduler.schedule(new Process[]{null},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("A",0,0)},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("A",-1,1)},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("A",0,-1)},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("A",0,1),new Process("A",0,1)},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("IDLE",0,1)},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("bad id",0,1)},alg,1));
            reject(() -> Scheduler.schedule(new Process[]{new Process("A",Integer.MAX_VALUE,1)},alg,1));
            Scheduler.Result late = Scheduler.schedule(new Process[]{new Process("A",1_000_000_000,1)},alg,1);
            require(late.processes().get(0).completionTime == 1_000_000_001,"Late arrival failed");
            Scheduler.Result edge = Scheduler.schedule(new Process[]{new Process("A",0,Integer.MAX_VALUE)},alg,Integer.MAX_VALUE);
            require(edge.processes().get(0).completionTime == Integer.MAX_VALUE,"Maximum burst failed");
        }
        reject(() -> Scheduler.schedule(new Process[]{new Process("A",0,1)},Scheduler.Algorithm.RR,-1));
        System.out.println("PASS " + runs + " seeded oracle comparisons (200 workloads, seed 20261021)");
        System.out.println("PASS 41 invalid-input checks and 8 large-time boundary checks");
    }
    static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected input rejection");
    }
    static void verify(Process[] input, Scheduler.Algorithm alg, int quantum, int sample) {
        int n = input.length;
        int[] remaining = new int[n], start = new int[n], completion = new int[n];
        Arrays.fill(start,-1);
        for (int i = 0; i < n; i++) remaining[i] = input[i].burstTime;
        List<String> ticks = new ArrayList<>();
        Deque<Integer> ready = new ArrayDeque<>();
        int time = 0, done = 0, current = -1, used = 0, pending = -1;
        while (done < n) {
            // Observe every arrival at its actual tick, then requeue an expired slice.
            if (alg == Scheduler.Algorithm.RR) {
                for (int i = 0; i < n; i++) if (input[i].arrivalTime == time) ready.addLast(i);
                if (pending >= 0) { ready.addLast(pending); pending = -1; }
                if (current < 0 && !ready.isEmpty()) { current = ready.removeFirst(); used = 0; }
            } else if (current < 0 || alg == Scheduler.Algorithm.SRTF) {
                current = -1;
                for (int i = 0; i < n; i++) if (remaining[i] > 0 && input[i].arrivalTime <= time) {
                    int value = alg == Scheduler.Algorithm.FCFS ? input[i].arrivalTime : alg == Scheduler.Algorithm.SJF ? input[i].burstTime : remaining[i];
                    int best = current < 0 ? Integer.MAX_VALUE : alg == Scheduler.Algorithm.FCFS ? input[current].arrivalTime : alg == Scheduler.Algorithm.SJF ? input[current].burstTime : remaining[current];
                    if (value < best) current = i;
                }
            }
            ticks.add(current < 0 ? null : input[current].pid);
            if (current >= 0) {
                if (start[current] < 0) start[current] = time;
                remaining[current]--; used++;
                if (remaining[current] == 0) { completion[current] = time + 1; done++; current = -1; }
                else if (alg == Scheduler.Algorithm.RR && used == quantum) { pending = current; current = -1; }
            }
            time++;
        }
        Scheduler.Result actual = Scheduler.schedule(input,alg,quantum);
        String context = "sample=" + sample + " " + alg + " q=" + quantum;
        List<String> actualTicks = new ArrayList<>();
        int boundary = 0;
        for (Scheduler.Slice s : actual.slices()) {
            require(s.start() == boundary && s.end() > s.start(), "Invalid interval " + context);
            for (int t = s.start(); t < s.end(); t++) actualTicks.add(s.pid());
            boundary = s.end();
        }
        require(ticks.equals(actualTicks),"Timeline differs: " + context);
        double waitSum = 0, turnSum = 0;
        for (int i = 0; i < n; i++) {
            Process p = actual.processes().get(i);
            int turn = completion[i] - input[i].arrivalTime, wait = turn - input[i].burstTime;
            require(p.startTime == start[i] && p.completionTime == completion[i] && p.turnTime == turn && p.waitingTime == wait && p.remTime == 0,"Metrics differ: " + context);
            require(input[i].remTime == input[i].burstTime && input[i].completionTime == 0,"Input changed");
            waitSum += wait; turnSum += turn;
        }
        require(actual.averageWaiting() == waitSum/n && actual.averageTurnaround() == turnSum/n,"Average differs: " + context);
    }
}
