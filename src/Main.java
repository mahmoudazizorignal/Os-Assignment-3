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
        AGScheduler agScheduler = new AGScheduler(4, 4);
        agScheduler.run();
        agScheduler.printExecutionHistory();
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
    private Map<String, Integer> waiting_time, turn_around_time;
    private ArrayList<ArrayList<Integer>> quantumHistory;
    private int QT;
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
        quantumHistory = new ArrayList<>();
        QT = quantumT;

//        System.out.println("Enter each process's info (Name ArrivalTime BurstTime PriorityNumber):");
//        Scanner scanner = new Scanner(System.in);
//
        ArrayList<AGProcess> AGList = new ArrayList<>();
//        for ( int i = 0; i < processNum; ++i ) {
//            String input;
//            input = scanner.nextLine();
//            String[] arrayList = input.split(" ");
//
//            String curName = arrayList[0];
//            int AT = Integer.parseInt(arrayList[1]);
//            int BT = Integer.parseInt(arrayList[2]);
//            int PN = Integer.parseInt(arrayList[3]);
//            int AG = -1;
//
//            Random random = new Random();
//            int randomNumber = random.nextInt(21);
//            if ( randomNumber < 10 )
//                AG = randomNumber + AT + BT;
//            else if ( randomNumber > 10 )
//                AG = 10 + AT + BT;
//            else
//                AG = PN + AT + BT;
//
//            AGList.add(new AGProcess(curName, AT, BT, PN, AG, quantumT));
//        }
        AGList.add(new AGProcess("P1", 0, 17, 4, 20, 4));
        AGList.add(new AGProcess("P2", 3, 6, 9, 17, 4));
        AGList.add(new AGProcess("P3", 4, 10, 2, 16, 4));
        AGList.add(new AGProcess("P4", 29, 4, 8, 43, 4));
        Collections.sort(AGList, Comparator.comparingInt(AGProcess::getAT));
        for ( int i = 0; i < processNum; ++i ) {
            all_processes.add(AGList.get(i));
        }
    }
    public void run() {
        int smQ = QT * all_processes.size(), t = 0, completed_processes = 0;
        int all_processes_count = all_processes.size();
        while ( completed_processes < all_processes_count ) {
            int t1 = t;
            checkProcessAtT(t);

            AGProcess top_element = ready_queue.removeFirst(); min_agfactor.remove(top_element);
            String name = top_element.getName();
            if ( !waiting_time.containsKey(name) )
                waiting_time.put(name, t - top_element.getAT());
            turn_around_time.put(name, 0);

            int runTime = Math.min((top_element.getQT() + 1) / 2, top_element.getBT());
            for ( int i = 0; i < runTime; ++i ) {
                checkProcessAtT(++t);
                turn_around_time.put(name, turn_around_time.get(name) + 1);
            }
            int curQT = top_element.getQT() - runTime;
            top_element.setBT(top_element.getBT() - runTime);

            while ( curQT!=0 && top_element.getBT()!=0 &&
                    ( min_agfactor.isEmpty() || top_element.getAGF() <= min_agfactor.iterator().next().getAGF() ) ) {
                checkProcessAtT(++t);
                turn_around_time.put(name, turn_around_time.get(name) + 1);
                --curQT;
                top_element.setBT(top_element.getBT() - 1);
            }

            if ( top_element.getBT() == 0 ) {
                smQ -= top_element.getQT();
                top_element.setQT(0);
                die_list.add(top_element);
                completed_processes++;
            }
            else {
                if ( curQT != 0 ) {
                    top_element.setQT(top_element.getQT() + curQT);
                    smQ += curQT;
                    turn_around_time.put(top_element.getName(),
                        turn_around_time.get(top_element.getName()) + waiting_time.get(top_element.getName()));
                    ready_queue.remove(min_agfactor.iterator().next());
                    ready_queue.addFirst(min_agfactor.iterator().next());
                }
                else {
                    int val = (int) ( Math.ceil(0.1 * smQ / all_processes_count) );
                    top_element.setQT(top_element.getQT() + val);
                    smQ += val;
                }
                ready_queue.addLast(top_element);
                min_agfactor.add(top_element);
            }
            int t2 = t;
            executionHistory.add(new HistoryItem(name, t1, t2));
        }
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
        System.out.println("Quantum History:");
        for ( int i = 0; i < quantumHistory.size(); ++i ) {
            for ( int j = 0; j < quantumHistory.get(i).size(); ++i )
                System.out.print(quantumHistory.get(i).get(j) + " ");
            System.out.println();
        }
    }
}