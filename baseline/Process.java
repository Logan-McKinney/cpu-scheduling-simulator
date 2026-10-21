
public class Process {
	
	String pid;
    int arrivalTime, burstTime, startTime, completionTime, waitingTime, turnTime, remTime;

    Process(String pid, int at, int bt) {
        this.pid = pid;
        this.arrivalTime = at;
        this.burstTime = bt;
        this.remTime = bt;
    }
}
