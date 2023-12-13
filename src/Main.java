import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
//        System.out.print("Enter # of processes: ");
//        int processNum = Integer.parseInt(scanner.nextLine());
//        System.out.print("Enter round robin time quantum: ");
//        int quantumT = Integer.parseInt(scanner.nextLine());
//        System.out.print("Enter context switching time: ");
//        int switchT = Integer.parseInt(scanner.nextLine());
//        AGScheduler agScheduler = new AGScheduler(4, 4);
//        agScheduler.run();
        SJFScheduler sjfScheduler = new SJFScheduler(4, 0);
        sjfScheduler.run();
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

    AGScheduler(int processNum, int quantumT) {
        min_agfactor = new TreeSet<>(new AGProcessComparator());
        ready_queue = new LinkedList<>();
        all_processes = new LinkedList<>();
        die_list = new ArrayList<>();
        executionHistory = new ArrayList<>();
        waiting_time = new HashMap<>();
        turn_around_time = new HashMap<>();
        curQuantum = new HashMap<>();

        System.out.println("Enter each process's info (Name ArrivalTime BurstTime PriorityNumber):");
        Scanner scanner = new Scanner(System.in);

        ArrayList<AGProcess> AGList = new ArrayList<>();
        for ( int i = 0; i < processNum; ++i ) {
            String input;
            input = scanner.nextLine();
            String[] arrayList = input.split(" ");

            String curName = arrayList[0];
            int AT = Integer.parseInt(arrayList[1]);
            int BT = Integer.parseInt(arrayList[2]);
            int PN = Integer.parseInt(arrayList[3]);
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
        for ( int i = 0; i < processNum; ++i ) {
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
    SJFScheduler(int processNum, int contextTime) {
        processes = new ArrayList<>();
        executedProcesses = new ArrayList<>();
        waiting_time = new HashMap<>();
        turn_around_time = new HashMap<>();
        this.contextTime = contextTime;

        System.out.println("Enter each processes' info (Name ArrivalTime BurstTime):");

        Scanner scanner = new Scanner(System.in);
        for (int i = 1; i <= processNum; i++) {
            String input;
            input = scanner.nextLine();
            String[] arrayList = input.split(" ");

            String curName = arrayList[0];
            int AT = Integer.parseInt(arrayList[1]);
            int BT = Integer.parseInt(arrayList[2]);
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