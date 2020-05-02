package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.Task;
import edu.ufp.inf.sd.dhm.server.TaskGroup;
import edu.ufp.inf.sd.dhm.server.User;

public class Client {

    public static void main(String[] args) {
        System.out.println("creating taskgroup ....");
        TaskGroup taskGroup = new TaskGroup(20,new User("rolotes"));
        System.out.println("creating task ....");
        Task task = new Task(null,2,null,null,20,taskGroup);
        System.out.println("creating workers ....");
        Worker worker = new Worker(1,null,task.getRecvQueue(),task.getSendQueue());
        Worker worker2 = new Worker(2,null,task.getRecvQueue(),task.getSendQueue());

        task.publish(task.getSendChannel(),"rolotes gay");

        System.out.println("finish");

    }
}
