import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Dependency-free regression runner. Each case is isolated to bound legacy hangs. */
public class RegressionTests {
    static final String[] CASES = {"fcfs-order", "rr-capacity", "rr-late", "idle-fcfs", "idle-sjf", "idle-srtf", "idle-rr", "ties-fcfs", "ties-sjf", "ties-srtf", "ties-rr", "zero", "negative-arrival", "negative-burst", "empty", "quantum", "metrics-fcfs", "metrics-sjf", "metrics-srtf", "metrics-rr", "rr-arrival-order", "overflow", "unchanged"};
    public static void main(String[] args) throws Exception {
        if (args.length > 0) { check(args[0]); return; }
        int passed = 0;
        for (String name : CASES) {
            java.lang.Process child = new ProcessBuilder(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java", "-cp", System.getProperty("java.class.path"), "RegressionTests", name).redirectErrorStream(true).start();
            boolean ended = child.waitFor(4, TimeUnit.SECONDS);
            if (!ended) { child.destroyForcibly(); child.waitFor(); }
            String detail = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            boolean ok = ended && child.exitValue() == 0;
            System.out.println((ok ? "PASS " : "FAIL ") + name + (!ended ? " (timeout)" : ok ? "" : " " + detail));
            if (ok) passed++;
        }
        System.out.println(passed + "/" + CASES.length + " passed");
        if (passed != CASES.length) System.exit(1);
    }
    static Process p(String id, int a, int b) { return new Process(id, a, b); }
    static void require(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static String run(String algorithm, Process[] p) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(bytes));
            switch (algorithm) {
                case "fcfs": scheduling_algorithms.fcfs(p); break;
                case "sjf": scheduling_algorithms.sjf(p); break;
                case "srtf": scheduling_algorithms.srtf(p); break;
                default: scheduling_algorithms.rr(p, 1);
            }
        } finally { System.setOut(old); }
        return bytes.toString();
    }
    static void row(String out, String id, int start, int completion, int wait, int turn) {
        require(out.matches("(?s).*\\b" + id + "\\s+" + start + "\\s+" + completion + "\\s+" + wait + "\\s+" + turn + "\\b.*"), "Incorrect row for " + id + ": " + out);
    }
    static void invalid(Runnable r) {
        try { r.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected IllegalArgumentException");
    }
    static void check(String name) {
        if (name.equals("fcfs-order")) {
            String out = run("fcfs", new Process[]{p("A", 5, 2), p("B", 0, 3)});
            row(out, "B", 0, 3, 0, 3); row(out, "A", 5, 7, 0, 2);
        } else if (name.equals("rr-capacity")) {
            row(run("rr", new Process[]{p("A", 0, 1000)}), "A", 0, 1000, 0, 1000);
        } else if (name.equals("rr-late")) {
            row(run("rr", new Process[]{p("A", 0, 1), p("B", 10, 2)}), "B", 10, 12, 0, 2);
        } else if (name.startsWith("idle-")) {
            String out = run(name.substring(5), new Process[]{p("A", 2, 1), p("B", 8, 1)});
            row(out, "B", 8, 9, 0, 1); require(out.contains("IDLE"), "Missing explicit idle interval");
        } else if (name.startsWith("ties-")) {
            String out = run(name.substring(5), new Process[]{p("A", 0, 2), p("B", 0, 2)});
            if (name.equals("ties-rr")) { row(out, "A", 0, 3, 1, 3); row(out, "B", 1, 4, 2, 4); }
            else { row(out, "A", 0, 2, 0, 2); row(out, "B", 2, 4, 2, 4); }
        } else if (name.startsWith("metrics-")) {
            String alg = name.substring(8);
            String out = run(alg, new Process[]{p("A", 0, 5), p("B", 1, 2), p("C", 2, 1)});
            if (alg.equals("fcfs")) { row(out,"A",0,5,0,5); row(out,"B",5,7,4,6); row(out,"C",7,8,5,6); }
            if (alg.equals("sjf")) { row(out,"A",0,5,0,5); row(out,"B",6,8,5,7); row(out,"C",5,6,3,4); }
            if (alg.equals("srtf")) { row(out,"A",0,8,3,8); row(out,"B",1,3,0,2); row(out,"C",3,4,1,2); }
            if (alg.equals("rr")) { row(out,"A",0,8,3,8); row(out,"B",1,5,2,4); row(out,"C",3,4,1,2); }
        } else if (name.equals("rr-arrival-order")) {
            String out = run("rr", new Process[]{p("A",0,6), p("B",2,1), p("C",1,1)});
            // q=3: C arrives before B within A's slice.
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintStream old = System.out;
            try { System.setOut(new PrintStream(bytes)); scheduling_algorithms.rr(new Process[]{p("A",0,6),p("B",2,1),p("C",1,1)},3); } finally { System.setOut(old); }
            row(bytes.toString(),"C",3,4,2,3);
        } else if (name.equals("zero")) {
            invalid(() -> run("srtf", new Process[]{p("A",0,0)}));
        } else if (name.equals("negative-arrival")) {
            invalid(() -> run("fcfs", new Process[]{p("A",-1,2)}));
        } else if (name.equals("negative-burst")) {
            invalid(() -> run("fcfs", new Process[]{p("A",0,-1)}));
        } else if (name.equals("empty")) {
            invalid(() -> run("fcfs", new Process[0]));
        } else if (name.equals("quantum")) {
            invalid(() -> scheduling_algorithms.rr(new Process[]{p("A",0,1)},0));
        } else if (name.equals("overflow")) {
            invalid(() -> run("fcfs",new Process[]{p("A",Integer.MAX_VALUE,1)}));
        } else if (name.equals("unchanged")) {
            Process[] input = {p("A",0,5),p("B",1,2)};
            for (String alg : new String[]{"fcfs","sjf","srtf","rr"}) run(alg,input);
            require(input[0].remTime == 5 && input[0].completionTime == 0 && input[1].remTime == 2, "Input mutated");
        }
    }
}
