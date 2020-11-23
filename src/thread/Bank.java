package thread;

import construct.Business;
import construct.Content;
import construct.Customer;
import construct.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ghy
 * @date 2020/11/23 上午11:07
 */
public class Bank {
    static ArrayList<Customer> globalCustomQueue = new ArrayList<Customer>();
    static ArrayList<Customer> preCustomer = new ArrayList<>();
    static ArrayList<Window> windowsList = new ArrayList<>();
    static double baseTime = 1000;
    static int flag = -1;
    static ArrayList<Content> contents = new ArrayList<>();

    public Bank(double baseTime) {
        Bank.baseTime = baseTime;
    }

    public Bank() {

    }

    public void makeCustomer() {
        for (int i = 0; i < 10; i++) {
            preCustomer.add(new Customer(i, "顾客" + i, 1));
        }
        for (int i = 10; i < 12; i++) {
            preCustomer.add(new Customer(i, "顾客" + i, 0));
        }
        Collections.shuffle(preCustomer);
    }

    public void makeWindow() {
        windowsList.add(new Window(0, "V", new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)), 0));
        windowsList.add(new Window(1, "A", new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)), 1));
        windowsList.add(new Window(2, "B", new ArrayList<Integer>(Arrays.asList(1, 2, 4, 5, 7)), 1));
        windowsList.add(new Window(3, "B", new ArrayList<Integer>(Arrays.asList(1, 2, 4, 5, 7)), 1));
    }

    public void bankOpen() {
        System.out.println("银行开门");
        this.makeCustomer();
        this.makeWindow();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(new CustomerComing(10, preCustomer, globalCustomQueue));
        for (int i = 0; i < 4; i++) {
            executorService.execute(new WindowServing(windowsList.get(i), baseTime, globalCustomQueue));
        }
        executorService.shutdown();
//        while (true){
//            if(executorService.isTerminated()){
//                System.out.println("银行关门啦");
//                System.out.println("一天的顾客日志如下");
//                System.out.printf("%s%20s%20s%20s\n","客户名称","客户到达时间","客户办理业务类型","客户所需时间");
//                for (Content c : contents) {
//                    System.out.printf("%s%20s%20s%20s\n",c.getCustomerName(),c.getArriveTime(),c.getBusinessType(),c.getUseTime());
//                }
//                break;
//            }
//        }
    }

    static class CustomerComing implements Runnable {
        private int customerSize;
        private ArrayList<Customer> customerList;
        private final ArrayList<Customer> globalCustomQueue;

        public CustomerComing(int customerSize, ArrayList<Customer> customerList, ArrayList<Customer> globalCustomQueue) {
            this.customerSize = customerSize;
            this.customerList = customerList;
            this.globalCustomQueue = globalCustomQueue;
        }


        @Override
        public void run() {
            for (int i = 0; i < this.customerSize; i++) {
                try {
                    System.out.println(customerList.get(i).getName() + "到达,取号等待,需要办理的业务类型为："
                            + customerList.get(i).getBusiness().getId());
                    synchronized (globalCustomQueue) {
                        globalCustomQueue.add(customerList.get(i));
                    }
                    Thread.sleep(new Random().nextInt((int) Bank.baseTime));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Bank.flag = 1;
        }
    }

    static class WindowServing implements Runnable {
        private final Window serveWindow;
        private final double baseTime;
        private final ArrayList<Customer> globalCustomQueue;

        WindowServing(Window serveWindow, double baseTime, ArrayList<Customer> globalCustomQueue) {
            this.serveWindow = serveWindow;
            this.baseTime = baseTime;
            this.globalCustomQueue = globalCustomQueue;
        }

        /**
         * 如果银行的窗口名称为A类窗口，就直接从顾客等待队列里拿第一个数据并移除
         * 如果银行的窗口名称为B类窗口，遍历每一个等待队列中的顾客，如果某个顾客办理的业务B窗口无法执行就跳过这个顾客
         * 如果银行的窗口名称为V类窗口，遍历每一个等待队列中的顾客，如果顾客是VIP(priority = 0)，就优先返回这个顾客并移除队列，如果没有直接取第一个
         *
         * @return Customer
         */
        public Customer getCustomerFromQueue() {
            synchronized (globalCustomQueue) {
                if (!readQueue()) {
                    if ("A".equals(this.serveWindow.getName())) {
                        Customer res = globalCustomQueue.get(0);
                        globalCustomQueue.remove(0);
                        return res;
                    } else if ("B".equals(this.serveWindow.getName())) {
                        for (int i = 0; i < globalCustomQueue.size(); i++) {
                            Business customer = globalCustomQueue.get(i).getBusiness();
                            if (customer == Business.PAY_FINE || customer == Business.PURCHASE_FUND
                                    || customer == Business.INDIVIDUAL_LOAN_REPAYMENT) {
                                continue;
                            } else {
                                Customer res = globalCustomQueue.get(i);
                                globalCustomQueue.remove(i);
                                return res;
                            }
                        }
                        return null;
                    } else if ("V".equals(this.serveWindow.getName())) {
                        for (int i = 0; i < globalCustomQueue.size(); i++) {
                            if (globalCustomQueue.get(i).getPriority() == 0) {
                                Customer res = globalCustomQueue.get(i);
                                globalCustomQueue.remove(i);
                                return res;
                            }
                        }
                        Customer res = globalCustomQueue.get(0);
                        globalCustomQueue.remove(0);
                        return res;
                    }
                } else {
                    return null;
                }
            }
            return null;
        }


        /**
         * 获取队列当前状态，是否为空
         *
         * @return Boolean
         */
        public Boolean readQueue() {
            synchronized (globalCustomQueue) {
                return globalCustomQueue.isEmpty();
            }
        }

        @Override
        public void run() {
            System.out.println("窗口" + this.serveWindow.getName() + "窗口id" + this.serveWindow.getId() + "开启");
            while (true) {
                if (readQueue()) {
                    if (Bank.flag == 1) {
                        break;
                    }
                } else {
                    Customer customer = getCustomerFromQueue();
                    if (customer == null) {
                        continue;
                    }
                    try {
                        System.out.println("窗口" + this.serveWindow.getName() + " 窗口id" + this.serveWindow.getId() + " 服务顾客" + customer.getId());
                        double low = customer.getBusiness().getLowTimeProportion();
                        double high = customer.getBusiness().getHighTimeProportion();
                        int serveTime = new Random().nextInt((int) (baseTime * (high - low))) + (int) (baseTime * low);
                        Thread.sleep(serveTime);
                        Bank.contents.add(new Content(customer.getName(), 0, customer.getBusiness(), serveTime));
                        System.out.println(customer.getName() + "在窗口" + this.serveWindow.getName() + "完成服务," + "耗时:" + serveTime * 1.0 / 1000 + "秒");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Thread.yield();
            }
            System.out.println("窗口" + this.serveWindow.getName() + "窗口id" + this.serveWindow.getId() + "关闭");
        }
    }
}
