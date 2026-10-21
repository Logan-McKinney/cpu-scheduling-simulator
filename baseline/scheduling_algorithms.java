/*
 * Logan McKinney
 * 6 October 2025
 * This program takes in user input as processes with arrival times and burst times
 * It then runs FCFS, RR, SJF, and SRTF. It keeps track of all of the time
 * intervals for all the processes, then prints the gantt chart as well as AWT and ATAT
 */

import java.util.Scanner;

public class scheduling_algorithms {
	public static void main(String[] args) {
		// takes user input
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter number of processes: ");
        int n = sc.nextInt();

        Process[] processes = new Process[n];
        for (int i = 0; i < n; i++) {
            System.out.print("Enter Process ID: ");
            String pid = sc.next();
            System.out.print("Enter Arrival Time: ");
            int at = sc.nextInt();
            System.out.print("Enter Burst Time: ");
            int bt = sc.nextInt();
            processes[i] = new Process(pid, at, bt); // adds user entered processes into an array
        }
        // runs scheduling algorithms
        fcfs(processes);
        rr(processes, 5);
        rr(processes, 10);
        sjf(processes);
        srtf(processes);
        
        sc.close();
    }
		
    // -------------fcfs----------------------------
    public static void fcfs(Process[] o) {
        Process[] p = copyArray(o); // dont change original array
        int currentTime = 0;
        String gantt = "Gantt Chart:\n" + currentTime;

        System.out.println("\nAlgorithm: FCFS");
        System.out.println("PID\tStart\tCompletion\tWaiting\tTurnaround");

        for (int i = 0; i < p.length; i++) {
            if (currentTime < p[i].arrivalTime)
                currentTime = p[i].arrivalTime; // start at first proc. arrival time

            // runs fcfs algorithm. gets first proc./ runs through/ updates times
            p[i].startTime = currentTime;
            p[i].completionTime = currentTime + p[i].burstTime;
            p[i].waitingTime = p[i].startTime - p[i].arrivalTime;
            p[i].turnTime = p[i].waitingTime + p[i].burstTime;
            currentTime = p[i].completionTime;
            // updates gantt chart
            gantt = gantt + " | " + p[i].pid + " | " + currentTime;
            printRow(p[i]); //print finished process
        }

        printAverages(p);
        System.out.println(gantt);
    }

    //------------round robin----------------------
    public static void rr(Process[] o, int quantum) {
        Process[] p = copyArray(o); // dont change original array
        int n = p.length;
        int time = 0;
        String gantt = "Gantt Chart:\n" + time;

        int earliest = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++)
            if (p[i].arrivalTime < earliest) earliest = p[i].arrivalTime; 
        	//start with first arriving process
        //makes a queue holding indexes of process that need ran
        int[] queue = new int[n * 10];
        int front = 0, rear = 0;
        int nextIndex = -1; 

        //find first process that will run
        for (int i = 0; i < n; i++)
            if (p[i].arrivalTime == earliest) {
                nextIndex = i;
                break;
            }
        
        // index of first proc into queue and adjust time
        queue[rear++] = nextIndex;
        time = earliest;

        //dequeue next proc. to run, and continue until all complete
        while (front < rear) {
            int idx = queue[front++];
            Process proc = p[idx];

            //if process hasnt began, adjust to its arrival to force start
            if (proc.remTime == proc.burstTime && time < proc.arrivalTime)
                time = proc.arrivalTime;
            // keep track of start time
            if (proc.remTime == proc.burstTime)
                proc.startTime = Math.max(time, proc.arrivalTime);
            
            //make sure quantum isnt larger than remaining
            int exec = Math.min(quantum, proc.remTime);
            proc.remTime -= exec; //run for a quant and update gantt
            gantt = gantt + " | " + proc.pid;
            time += exec;
            gantt = gantt + " | " + time;

            //any arrived processes not in queue are added
            for (int i = 0; i < n; i++) {
                if (p[i].arrivalTime <= time && p[i].remTime > 0 && !isInQueue(queue, front, rear, i)) {
                    boolean alreadyCompleted = p[i].completionTime != 0; // checks if it was already completed
                    if (!alreadyCompleted && i != idx)
                        queue[rear++] = i; // added to queue
                }
            }

            if (proc.remTime > 0) // if process didnt finish, add back to queue
                queue[rear++] = idx;
            else { // if finished, mark down times
                proc.completionTime = time;
                proc.turnTime = proc.completionTime - proc.arrivalTime;
                proc.waitingTime = proc.turnTime - proc.burstTime;
            }
        }
        //prints result
        System.out.println("\nAlgorithm: Round Robin (" + quantum + "ms)");
        System.out.println("PID\tStart\tCompletion\tWaiting\tTurnaround");
        for (int i = 0; i < n; i++)
            printRow(p[i]);

        printAverages(p);
        System.out.println(gantt);
    }
    //returns a boolean whether or not a proces is in the queue
    public static boolean isInQueue(int[] q, int f, int r, int val) {
        for (int i = f; i < r; i++)
            if (q[i] == val) return true;
        return false;
    }

    // -------------sjf--------------------------
    public static void sjf(Process[] o) {
        Process[] p = copyArray(o); // dont change original array
        int n = p.length;
        boolean[] done = new boolean[n]; // indicies of which are finished
        int completed = 0;
        int time = 0;
        String gantt = "Gantt Chart:\n" + time;

        System.out.println("\nAlgorithm: SJF");
        System.out.println("PID\tStart\tCompletion\tWaiting\tTurnaround");

        while (completed < n) { // run for all proc.
            int idx = -1;
            int minBurst = Integer.MAX_VALUE; // find lowest burst that isnt done and arrived
            for (int i = 0; i < n; i++) {
                if (!done[i] && p[i].arrivalTime <= time && p[i].burstTime < minBurst) {
                    minBurst = p[i].burstTime;
                    idx = i;
                }
            }
            //makes sure code doesnt break. if no index sets i to >0, just add to 
            //time until proc. arrives
            if (idx == -1) {
                time++;
                continue;
            }
            //record times
            p[idx].startTime = time;
            p[idx].completionTime = time + p[idx].burstTime;
            p[idx].turnTime = p[idx].completionTime - p[idx].arrivalTime;
            p[idx].waitingTime = p[idx].turnTime - p[idx].burstTime;
            time += p[idx].burstTime; // update curr time
            done[idx] = true;
            completed++;

            //print results
            gantt = gantt + " | " + p[idx].pid + " | " + time;
            printRow(p[idx]);
        }

        printAverages(p);
        System.out.println(gantt);
    }

    // --------------srtf-------------------------
    public static void srtf(Process[] o) {
        Process[] p = copyArray(o); // dont change original array
        int n = p.length;
        int completed = 0;
        int time = 0;
        Process current = null; // keep track of whats runninng
        String gantt = "Gantt Chart:\n" + time;

        System.out.println("\nAlgorithm: SRTF");
        System.out.println("PID\tStart\tCompletion\tWaiting\tTurnaround");

        while (completed < n) {
            int idx = -1; // similar logic to last with finding what process to start first
            int minRem = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (p[i].arrivalTime <= time && p[i].remTime > 0 && p[i].remTime < minRem) {
                    minRem = p[i].remTime;
                    idx = i; // index of first proc
                }
            }
            if (idx == -1) { // if nothing ready, just add to time and recheck
                time++;
                continue;
            }
            if (current != p[idx]) { // cpu switch processes
                gantt = gantt + " | " + p[idx].pid + " | " + time;
            }
            current = p[idx];
            //if first time running record start time
            if (p[idx].remTime == p[idx].burstTime) 
                p[idx].startTime = time;
            p[idx].remTime--;
            time++; //increase clock by 1 to recheck for new shortest time

            if (p[idx].remTime == 0) { // if process completed, record times
                p[idx].completionTime = time;
                p[idx].turnTime = p[idx].completionTime - p[idx].arrivalTime;
                p[idx].waitingTime = p[idx].turnTime - p[idx].burstTime;
                completed++;
                gantt = gantt + " | " + time;
            }
        }

        for (int i = 0; i < n; i++) //prints
            printRow(p[i]);

        printAverages(p);
        System.out.println(gantt);
    }

    //---------helper methods
    //copies an array of processes so the original isnt changed
    public static Process[] copyArray(Process[] arr) {
        Process[] copy = new Process[arr.length];
        for (int i = 0; i < arr.length; i++)
            copy[i] = new Process(arr[i].pid, arr[i].arrivalTime, arr[i].burstTime);
        return copy;
    }
    //prints st, ct, wt, and TAT for a process
    public static void printRow(Process p) {
        System.out.println(p.pid + "\t" + p.startTime + "\t" + p.completionTime + "\t\t" + p.waitingTime + "\t" + p.turnTime);
    }
    
    //calculated AWT and ATAT and prints it
    public static void printAverages(Process[] p) {
        double totalWT = 0, totalTT = 0;
        for (int i = 0; i < p.length; i++) {
            totalWT += p[i].waitingTime;
            totalTT += p[i].turnTime;
        }
        System.out.println("Average Waiting Time: " + (totalWT / p.length));
        System.out.println("Average Turnaround Time: " + (totalTT / p.length));
    }
}

