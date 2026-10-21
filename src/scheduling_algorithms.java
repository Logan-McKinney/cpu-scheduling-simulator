/*
 * Logan McKinney - original scheduler, 6 October 2025.
 * Console interface retained; scheduling returns independently testable results.
 */
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class scheduling_algorithms {
    public static void main(String[] args) {
        try {
            Process[] processes;
            int[] quanta = {5, 10};
            if (args.length > 0) {
                processes = load(Path.of(args[0]));
                if (args.length > 1) {
                    quanta = new int[args.length - 1];
                    for (int i = 1; i < args.length; i++) {
                        quanta[i - 1] = Integer.parseInt(args[i]);
                        if (quanta[i - 1] <= 0) throw new IllegalArgumentException("Quantum must be positive");
                    }
                }
            } else {
                Scanner sc = new Scanner(System.in);
                System.out.print("Enter number of processes: ");
                int n = sc.nextInt();
                if (n <= 0) throw new IllegalArgumentException("Process count must be positive");
                processes = new Process[n];
                for (int i = 0; i < n; i++) {
                    System.out.print("Enter Process ID: "); String id = sc.next();
                    System.out.print("Enter Arrival Time: "); int at = sc.nextInt();
                    System.out.print("Enter Burst Time: "); int bt = sc.nextInt();
                    processes[i] = new Process(id, at, bt);
                }
            }
            List<Scheduler.Result> results = new ArrayList<>();
            results.add(Scheduler.schedule(processes, Scheduler.Algorithm.FCFS, 0));
            results.add(Scheduler.schedule(processes, Scheduler.Algorithm.SJF, 0));
            results.add(Scheduler.schedule(processes, Scheduler.Algorithm.SRTF, 0));
            for (int quantum : quanta) results.add(Scheduler.schedule(processes, Scheduler.Algorithm.RR, quantum));
            for (Scheduler.Result result : results) print(result);
            System.out.println("\nComparison (same workload; times in ms)");
            System.out.println("Algorithm\tAverage waiting\tAverage turnaround");
            for (Scheduler.Result r : results) System.out.printf(Locale.ROOT, "%s\t%.3f\t%.3f%n", r.algorithm(), r.averageWaiting(), r.averageTurnaround());
        } catch (IOException | IllegalArgumentException | NoSuchElementException ex) {
            System.err.println("Input error: " + ex.getMessage());
            System.exit(1);
        }
    }
    /** Whitespace-separated ID arrival burst; blank lines and # comments allowed. */
    static Process[] load(Path path) throws IOException {
        List<Process> processes = new ArrayList<>();
        int number = 0;
        for (String line : Files.readAllLines(path)) {
            number++;
            line = line.split("#", 2)[0].trim();
            if (line.isEmpty()) continue;
            String[] fields = line.split("\\s+");
            if (fields.length != 3) throw new IllegalArgumentException("Expected ID arrival burst on line " + number);
            processes.add(new Process(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
        }
        return processes.toArray(new Process[0]);
    }
    public static void fcfs(Process[] p) { print(Scheduler.schedule(p, Scheduler.Algorithm.FCFS, 0)); }
    public static void sjf(Process[] p) { print(Scheduler.schedule(p, Scheduler.Algorithm.SJF, 0)); }
    public static void srtf(Process[] p) { print(Scheduler.schedule(p, Scheduler.Algorithm.SRTF, 0)); }
    public static void rr(Process[] p, int quantum) { print(Scheduler.schedule(p, Scheduler.Algorithm.RR, quantum)); }
    public static Process[] copyArray(Process[] p) { return Scheduler.copy(p); }
    public static void printRow(Process p) {
        System.out.println(p.pid + "\t" + p.startTime + "\t" + p.completionTime + "\t" + p.waitingTime + "\t" + p.turnTime);
    }
    public static void print(Scheduler.Result result) {
        System.out.println("\nAlgorithm: " + result.algorithm());
        System.out.println("PID\tStart\tCompletion\tWaiting\tTurnaround");
        for (Process p : result.processes()) printRow(p);
        System.out.println("Average Waiting Time: " + result.averageWaiting());
        System.out.println("Average Turnaround Time: " + result.averageTurnaround());
        StringBuilder gantt = new StringBuilder("Gantt Chart:\n0");
        for (Scheduler.Slice slice : result.slices()) gantt.append(" | ").append(slice.pid() == null ? "IDLE" : slice.pid()).append(" | ").append(slice.end());
        System.out.println(gantt);
    }
}
