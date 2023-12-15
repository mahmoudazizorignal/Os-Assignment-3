import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter # of processes: ");
        int processNum = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter round robin time quantum: ");
        int quantumT = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter context switching time: ");
        int switchT = Integer.parseInt(scanner.nextLine());

        System.out.println("Enter each process's info (Name ArrivalTime BurstTime PriorityNumber):");
        ArrayList<Process> processes = new ArrayList<>();
        for ( int i = 0; i < processNum; ++i ) {
            String input;
            input = scanner.nextLine();
            String[] arrayList = input.split(" ");

            String curName = arrayList[0];
            int AT = Integer.parseInt(arrayList[1]);
            int BT = Integer.parseInt(arrayList[2]);
            int PN = Integer.parseInt(arrayList[3]);

            processes.add(new Process(curName, AT, BT, PN));
        }
        while ( true ) {
            System.out.println("Choose Your Scheduler:");
            System.out.println("[1] Shortest- Job First (SJF)");
            System.out.println("[2] Shortest- Remaining Time First (SRTF)");
            System.out.println("[3] Priority Scheduling");
            System.out.println("[4] AG Scheduling");
            System.out.println("[5] Exit");
            System.out.print("=> ");
            int k = Integer.parseInt(scanner.nextLine());
            switch(k) {
                case 1:
                    SJFScheduler sjfScheduler = new SJFScheduler(processes, switchT);
                    sjfScheduler.run();
                    break;
                case 2:
                    SRTFScheduling srtfScheduling = new SRTFScheduling(processes);
                    srtfScheduling.run();
                    break;
                case 3:
                    PriorityScheduling priorityScheduling = new PriorityScheduling(processes);
                    priorityScheduling.run();
                    break;
                case 4:
                    AGScheduler agScheduler = new AGScheduler(processes, quantumT);
                    agScheduler.run();
                    break;
                case 5:
                    System.exit(0);
            }
        }
//        AGScheduler agScheduler = new AGScheduler(4, 4);
//        agScheduler.run();
//        SJFScheduler sjfScheduler = new SJFScheduler(4, 0);
//        sjfScheduler.run();
//        agScheduler.printExecutionHistory();
//        agScheduler.printWaitingTime();
//        agScheduler.printTurnAroundTime();
//        agScheduler.printQuantumHistory();
//        agScheduler.run(5, 5, 5);
    }
}

class Process {
    private String name;
    private int AT, BT, PN;

    Process(String name, int AT, int BT, int PN) {
        this.name = name;
        this.AT = AT;
        this.BT = BT;
        this.PN = PN;
    }
    public String getName() {
        return name;
    }
    public int getAT() {
        return AT;
    }
    public int getBT() {
        return BT;
    }

    public int getPN() {
        return PN;
    }

    public void setBT(int BT) {
        this.BT = BT;
    }
}
//////////////////////////////////////////////////////////////////////////////////////

class AGProcess extends Process {
    private int AGF, QT;
    AGProcess(String name, int AT, int BT, int PN, int AGF, int QT) {
        super(name, AT, BT, PN);
        this.AGF = AGF;
        this.QT = QT;
    }
    public int getAGF() {
        return AGF;
    }
    public int getQT() {
        return QT;
    }
    public void setQT(int QT) {
        this.QT = QT;
    }
}
class AGProcessComparator implements Comparator<AGProcess> {
    @Override
    public int compare(AGProcess p1, AGProcess p2) {
        return Integer.compare(p1.getAGF(), p2.getAGF());
    }
}
class HistoryItem {
    String name;
    int t1, t2;
    HistoryItem(String name, int t1, int t2) {
        this.name = name;
        this.t1 = t1;
        this.t2 = t2;
    }
    @Override
    public String toString() {
        return (name + " running from " + t1 + " to " + t2);
    }
}

class AGScheduler {
    private Set<AGProcess> min_agfactor;
    private Deque<AGProcess> ready_queue;
    private Queue<AGProcess> all_processes;
    private ArrayList<AGProcess> die_list;
    private ArrayList<HistoryItem> executionHistory;
    private Map<String, Integer> waiting_time, turn_around_time, curQuantum;
    private void checkProcessAtT(int t) {
        while ( !all_processes.isEmpty() && t == all_processes.peek().getAT() ) {
            AGProcess top_element = all_processes.poll();
            min_agfactor.add(top_element);
            ready_queue.addLast(top_element);
        }
    }

    AGScheduler(ArrayList<Process> processes, int quantumT) {
        min_agfactor = new TreeSet<>(new AGProcessComparator());
        ready_queue = new LinkedList<>();
        all_processes = new LinkedList<>();
        die_list = new ArrayList<>();
        executionHistory = new ArrayList<>();
        waiting_time = new HashMap<>();
        turn_around_time = new HashMap<>();
        curQuantum = new HashMap<>();

        System.out.println("Enter each process's info (Name ArrivalTime BurstTime PriorityNumber):");

        ArrayList<AGProcess> AGList = new ArrayList<>();
        for ( int i = 0; i < processes.size(); ++i ) {
            String curName = processes.get(i).getName();
            int AT = processes.get(i).getAT();
            int BT = processes.get(i).getBT();
            int PN = processes.get(i).getPN();
            int AG = -1;

            Random random = new Random();
            int randomNumber = random.nextInt(21);
            if ( randomNumber < 10 )
                AG = randomNumber + AT + BT;
            else if ( randomNumber > 10 )
                AG = 10 + AT + BT;
            else
                AG = PN + AT + BT;

            AGList.add(new AGProcess(curName, AT, BT, PN, AG, quantumT));
        }
//        AGList.add(new AGProcess("P1", 0, 17, 4, 20, 4));
//        AGList.add(new AGProcess("P2", 3, 6, 9, 17, 4));
//        AGList.add(new AGProcess("P3", 4, 10, 2, 16, 4));
//        AGList.add(new AGProcess("P4", 29, 4, 8, 43, 4));
        Collections.sort(AGList, Comparator.comparingInt(AGProcess::getAT));
        for ( int i = 0; i < processes.size(); ++i ) {
            all_processes.add(AGList.get(i));
            curQuantum.put(AGList.get(i).getName(), quantumT);
        }
    }
    public void run() {

        int smQ = all_processes.peek().getQT() * all_processes.size(), t = 0, completed_processes = 0;
        int all_processes_count = all_processes.size();
        printQuantumHistory();

        while ( completed_processes < all_processes_count ) {


            int t1 = t;
            checkProcessAtT(t);

            AGProcess top_element = ready_queue.removeFirst(); min_agfactor.remove(top_element);
            String name = top_element.getName();
            int AT = top_element.getAT();
            int QT = top_element.getQT();
            int BT = top_element.getBT();
            int AGF = top_element.getAGF();

            if ( !waiting_time.containsKey(name) )
                waiting_time.put(name, t - AT);

            int runTime = Math.min((QT + 1) / 2, BT);
            for ( int i = 0; i < runTime; ++i ) {
                checkProcessAtT(++t);
            }
            QT = QT - runTime;
            BT -= runTime;

            while ( QT != 0 && BT != 0 &&
                    ( min_agfactor.isEmpty() || AGF <= min_agfactor.iterator().next().getAGF() ) ) {
                --QT; --BT;
                checkProcessAtT(++t);
            }

            if ( BT == 0 ) {
                smQ -= top_element.getQT(); QT = 0;
                top_element.setQT(QT); top_element.setBT(BT);
                curQuantum.put(name, QT);
                die_list.add(top_element);
                completed_processes++;
                turn_around_time.put(
                        name,
                        t - AT
                );
            }
            else {
                if ( QT != 0 ) {
                    smQ += QT;
                    QT += top_element.getQT();
                    top_element.setQT(QT);
                    top_element.setBT(BT);
                    curQuantum.put(name, QT);
                    ready_queue.remove(min_agfactor.iterator().next());
                    ready_queue.addFirst(min_agfactor.iterator().next());
                }
                else {
                    int val = (int) ( Math.ceil(0.1 * smQ / all_processes_count) );
                    QT = top_element.getQT() + val;   smQ += val;
                    top_element.setQT(QT);
                    top_element.setBT(BT);
                    curQuantum.put(name, QT);
                }
                ready_queue.addLast(top_element);
                min_agfactor.add(top_element);
            }
            int t2 = t;
            executionHistory.add(new HistoryItem(name, t1, t2));

            printQuantumHistory();
        }
        System.out.println("*******************************");
        printExecutionHistory();
        System.out.println("*******************************");
        printWaitingTime();
        System.out.println("*******************************");
        printTurnAroundTime();
        System.out.println("*******************************");
    }
    public void printExecutionHistory() {
        for ( int i = 0; i < executionHistory.size(); ++i )
            System.out.println(executionHistory.get(i).toString());
    }
    public void printWaitingTime() {
        double avg = 0;
        for ( Map.Entry<String, Integer> entry : waiting_time.entrySet() ) {
            System.out.println(entry.getKey() + " waiting time : " + entry.getValue());
            avg += entry.getValue();
        }
        avg /= waiting_time.size();
        System.out.println("Average waiting time: " + avg);
    }
    public void printTurnAroundTime() {
        double avg = 0;
        for ( Map.Entry<String, Integer> entry : turn_around_time.entrySet() ) {
            System.out.println(entry.getKey() + " turnaround time : " + entry.getValue());
            avg += entry.getValue();
        }
        avg /= turn_around_time.size();
        System.out.println("Average turnaround time: " + avg);
    }
    public void printQuantumHistory() {
        System.out.print("Quantum History: ");
        for ( Map.Entry<String, Integer> entry : curQuantum.entrySet() ) {
            System.out.print(entry.getKey() + ": " + entry.getValue() + "  ");
        }
        System.out.println();
    }
}

//////////////////////////////////////////////////////////////////////////////////////
class SJFScheduler {

    private List<Process> processes;
    private List<Process> executedProcesses;
    private int contextTime;
    private Map<String, Integer> waiting_time, turn_around_time;
    SJFScheduler(ArrayList<Process> all_processes, int contextTime) {
        processes = new ArrayList<>();
        executedProcesses = new ArrayList<>();
        waiting_time = new HashMap<>();
        turn_around_time = new HashMap<>();
        this.contextTime = contextTime;

        System.out.println("Enter each processes' info (Name ArrivalTime BurstTime):");

        Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < all_processes.size(); i++) {
            String curName = all_processes.get(i).getName();
            int AT = all_processes.get(i).getAT();
            int BT = all_processes.get(i).getBT();
            int PN = 0;
            processes.add(new Process(curName, AT, BT, PN));
        }
        Collections.sort(processes, Comparator.comparingInt(p -> p.getAT()));
    }
    public void run() {
        System.out.println("\nGantt Chart:");
        System.out.print("0");

        int t = 0;
        while (!processes.isEmpty()) {
            Process shortestJob = null;
            int shortestBurst = Integer.MAX_VALUE;

            for (Process p : processes) {
                if (p.getAT() <= t && p.getBT() < shortestBurst) {
                    shortestJob = p;
                    shortestBurst = p.getBT();
                }
            }

            if (shortestJob == null) {
                t = processes.get(0).getAT();
                System.out.print(" Idle (" + t + ")");
            } else {
                processes.remove(shortestJob);
                waiting_time.put(shortestJob.getName(), t - shortestJob.getAT());
                turn_around_time.put(
                        shortestJob.getName(),
                        waiting_time.get(shortestJob.getName()) + shortestJob.getBT()
                );
                t += shortestJob.getBT();

                executedProcesses.add(shortestJob);

                System.out.print(" -> " + shortestJob.getName() + " (" + t + ")");
                t += contextTime;
            }
        }
        System.out.println("\n*********************************************");
        printWaitingTime();
        System.out.println("*********************************************");
        printTurnAroundTime();
        System.out.println("*********************************************");
    }
    public void printWaitingTime() {
        double avg = 0;
        for ( Map.Entry<String, Integer> entry : waiting_time.entrySet() ) {
            System.out.println(entry.getKey() + " waiting time : " + entry.getValue());
            avg += entry.getValue();
        }
        avg /= waiting_time.size();
        System.out.println("Average waiting time: " + avg);
    }
    public void printTurnAroundTime() {
        double avg = 0;
        for ( Map.Entry<String, Integer> entry : turn_around_time.entrySet() ) {
            System.out.println(entry.getKey() + " turnaround time : " + entry.getValue());
            avg += entry.getValue();
        }
        avg /= turn_around_time.size();
        System.out.println("Average turnaround time: " + avg);
    }

}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class SRTFScheduling {
    private int currentTime, completedProcesses;
    private ArrayList<Process> processes;
    private ArrayList<String> ganttChartProcesses;
    private ArrayList<Integer> ganttChartTime;
    private Map<String, Integer> waitingTime, turnaroundTime;

    SRTFScheduling(ArrayList<Process>processes) {
        this.processes = processes;
        currentTime = completedProcesses = 0;
        ganttChartProcesses = new ArrayList<>();
        ganttChartTime = new ArrayList<>();
        waitingTime = new HashMap<>();
        turnaroundTime = new HashMap<>();
        for (int i = 0; i < processes.size(); ++i) {
            turnaroundTime.put(processes.get(i).getName(), processes.get(i).getBT());
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void scheduler() {
        Collections.sort(processes, Comparator.comparingInt(Process::getAT));
        currentTime = processes.get(0).getAT();
        while (completedProcesses < processes.size()) {
            ArrayList<Process> readyQueue = new ArrayList<>();
            int i;
            for (i = 0; i < processes.size() && processes.get(i).getAT() <= currentTime; i++) {
                if (processes.get(i).getBT() > 0)  readyQueue.add(processes.get(i));
            }
            if (readyQueue.isEmpty()) {
                ganttChartProcesses.add(" ");
                ganttChartTime.add(processes.get(i).getAT() - currentTime);
                currentTime = processes.get(i).getAT();
                continue;
            }
            Collections.sort(readyQueue, Comparator.comparingInt(Process::getBT));
            Process executingProcess = readyQueue.get(0);
            if (i < processes.size()) {
                int nextArrivalTime = processes.get(i).getAT();
                ganttChartProcesses.add(executingProcess.getName());
                ganttChartTime.add(Math.min(nextArrivalTime - currentTime, executingProcess.getBT()));
                int t = currentTime;
                currentTime += Math.min(nextArrivalTime - currentTime, executingProcess.getBT());
                executingProcess.setBT(executingProcess.getBT() - (Math.min(nextArrivalTime - t, executingProcess.getBT())));
                if (executingProcess.getBT() == 0) completedProcesses++;
            }
            else {
                for (Process p : readyQueue) {
                    ganttChartProcesses.add(p.getName());
                    ganttChartTime.add(p.getBT());
                    completedProcesses++;
                }
                ArrayList<Integer>temp = new ArrayList<>();
                temp.add(processes.get(0).getAT());
                for (int j = 0; j < ganttChartTime.size(); ++j) {
                    temp.add(ganttChartTime.get(j) + temp.get(j));
                }
                ganttChartTime = temp;
                for (int j = 1; j < ganttChartProcesses.size(); ++j) {
                    /*
                        to solve this case
                        Process		P1		P2		P2		P2		P4		P1		P3
                        Time		0		1		2		3		5		10		17		26
                     */
                    if (Objects.equals(ganttChartProcesses.get(j), ganttChartProcesses.get(j-1))) {
                        ganttChartProcesses.remove(j);
                        ganttChartTime.remove(j);
                        --j;
                    }
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void calculateWaitingTime() {
        for (Process p : processes) {
            boolean first = true;
            int endTime = 0;
            for (int i = 0; i < ganttChartProcesses.size(); ++i) {
                if (Objects.equals(ganttChartProcesses.get(i), p.getName())) {
                    if (first) {
                        waitingTime.put(ganttChartProcesses.get(i), ganttChartTime.get(i) - p.getAT());
                        first = false;
                    }
                    else {
                        int x = waitingTime.get(ganttChartProcesses.get(i));
                        waitingTime.put(ganttChartProcesses.get(i), ganttChartTime.get(i) - endTime + x);
                    }
                    if (i != ganttChartProcesses.size() - 1) {
                        endTime = ganttChartTime.get(i + 1);
                    }
                }
            }
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void calculateTurnaroundTime() {
        for (int i = 0; i < processes.size(); ++i) {
            int waiting = waitingTime.get(processes.get(i).getName());
            int burst = turnaroundTime.get(processes.get(i).getName());
            turnaroundTime.put(processes.get(i).getName(), waiting + burst);
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private double avgWaitingTime() {
        double avg = 0;
        for (Map.Entry<String, Integer> entry : waitingTime.entrySet()) {
            avg += entry.getValue();
        }
        avg /= waitingTime.size();
        return avg;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private double avgTurnaroundTime() {
        double avg = 0;
        for (Map.Entry<String, Integer> entry : turnaroundTime.entrySet()) {
            avg += entry.getValue();
        }
        avg /= turnaroundTime.size();
        return avg;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    void run() {
        scheduler();
        calculateWaitingTime();
        calculateTurnaroundTime();
        System.out.println("-------------------------- Gantt Chart --------------------------");
        System.out.print("Process\t\t");
        for (int i = 0; i < ganttChartProcesses.size(); ++i) {
            System.out.print(ganttChartProcesses.get(i) + "\t\t");
        }
        System.out.println();
        System.out.print("Time\t\t");
        for (int i = 0; i < ganttChartTime.size(); ++i) {
            System.out.print(ganttChartTime.get(i) + "\t\t");
        }
        System.out.print("\n\n");
        System.out.println("--------------------- Waiting time for each process -------------------");
        System.out.println("Process\t\twaiting time");
        for (Map.Entry<String, Integer> entry : waitingTime.entrySet()) {
            System.out.println(entry.getKey() + "\t\t\t" + entry.getValue());
        }
        System.out.print("\n");
        System.out.println("--------------------- Turnaround time for each process -------------------");
        System.out.println("Process\t\tturnaround time");
        for (Map.Entry<String, Integer> entry : turnaroundTime.entrySet()) {
            System.out.println(entry.getKey() + "\t\t\t" + entry.getValue());
        }
        System.out.println("\nAverage Waiting Time = " + avgWaitingTime());
        System.out.println("Average Turnaround Time = " + avgTurnaroundTime());
    }



}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class PriorityScheduling {
    private ArrayList<Process>processes;
     ArrayList<String>ganttChartProcesses;
     ArrayList<Integer>ganttChartTime;
     Map<String, Integer>waitingTime, turnaroundTime;

    PriorityScheduling(ArrayList<Process>processes) {
        this.processes = processes;
        ganttChartTime = new ArrayList<>();
        ganttChartProcesses = new ArrayList<>();
        waitingTime = new HashMap<>();
        turnaroundTime = new HashMap<>();
    }
    ////////////////////////////////////////////////////////////////////
    void scheduler() {
        Collections.sort(processes, Comparator.comparingInt(Process::getAT));
        ganttChartTime.add(processes.get(0).getAT());
        int currentTime = processes.get(0).getAT();
        while (processes.size() > 0) {
            ArrayList<Process>readyQueue = new ArrayList<>();
            int i;
            for (i = 0; i < processes.size() && processes.get(i).getAT() <= currentTime; ++i) {
                    readyQueue.add(processes.get(i));
            }
            Collections.sort(readyQueue, Comparator.comparingInt(Process::getPN));
            if (i < processes.size()) {
                int nextArrivalTime = processes.get(i).getAT();
                for (Process p : readyQueue) {
                    ganttChartProcesses.add(p.getName());
                    ganttChartTime.add(p.getBT() + ganttChartTime.get(ganttChartTime.size() - 1));

                    waitingTime.put(p.getName(), currentTime - p.getAT());
                    turnaroundTime.put(p.getName(), currentTime - p.getAT() + p.getBT());

                    currentTime += p.getBT();
                    processes.remove(p);
                    if (currentTime >= nextArrivalTime) break;
                }
                if (currentTime < nextArrivalTime) {
                    ganttChartProcesses.add(" ");
                    ganttChartTime.add(ganttChartTime.get(ganttChartTime.size() - 1) + nextArrivalTime - currentTime);
                    currentTime = nextArrivalTime;
                }
            }
            else {
                for (Process p : readyQueue) {
                    ganttChartProcesses.add(p.getName());
                    ganttChartTime.add(p.getBT() + ganttChartTime.get(ganttChartTime.size() - 1));
                    waitingTime.put(p.getName(), currentTime - p.getAT());
                    turnaroundTime.put(p.getName(), currentTime - p.getAT() + p.getBT());
                    currentTime += p.getBT();
                    processes.remove(p);
                }
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////
    double avgWaitingTime() {
        double avg = 0;
        for (Map.Entry<String, Integer>entry : waitingTime.entrySet()) {
            avg += entry.getValue();
        }
        avg /= waitingTime.size();
        return avg;
    }
    /////////////////////////////////////////////////////////////////////////
    double avgTurnaroundTime() {
        double avg = 0;
        for (Map.Entry<String, Integer>entry : turnaroundTime.entrySet()) {
            avg += entry.getValue();
        }
        avg /= turnaroundTime.size();
        return avg;
    }
    ////////////////////////////////////////////////////////////////////////////
    void run() {
        scheduler();
        System.out.println("-------------------------- Gantt Chart --------------------------");
        System.out.print("Process\t\t");
        for (int i = 0; i < ganttChartProcesses.size(); ++i) {
            System.out.print(ganttChartProcesses.get(i) + "\t\t");
        }
        System.out.println();
        System.out.print("Time\t\t");
        for (int i = 0; i < ganttChartTime.size(); ++i) {
            System.out.print(ganttChartTime.get(i) + "\t\t");
        }
        System.out.print("\n\n");
        System.out.println("--------------------- Waiting time for each process -------------------");
        System.out.println("Process\t\twaiting time");
        for (Map.Entry<String, Integer> entry : waitingTime.entrySet()) {
            System.out.println(entry.getKey() + "\t\t\t" + entry.getValue());
        }
        System.out.print("\n");
        System.out.println("--------------------- Turnaround time for each process -------------------");
        System.out.println("Process\t\tturnaround time");
        for (Map.Entry<String, Integer> entry : turnaroundTime.entrySet()) {
            System.out.println(entry.getKey() + "\t\t\t" + entry.getValue());
        }
        System.out.println("\nAverage Waiting Time = " + avgWaitingTime());
        System.out.println("Average Turnaround Time = " + avgTurnaroundTime());
    }
}

