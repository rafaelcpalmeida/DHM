package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.Task;
import edu.ufp.inf.sd.dhm.server.TaskGroup;
import edu.ufp.inf.sd.dhm.server.User;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Client {

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("creating taskgroup ....");
        User user = new User("rolotes");
        TaskGroup taskGroup = new TaskGroup(20,user);
        System.out.println("creating task ....");
        Task task = new Task(null,2,null,null,20,taskGroup);
        System.out.println("creating workers ....");
        Worker worker = new Worker(1,user);
        Worker worker2 = new Worker(2,user);
        //task.publish("rolotes123","rolotes_task_send.worker1");
        task.publishToAllWorkers("GENERAL MESSAGE");
        task.publishToWorkersQueue("JUST 1 CAN GET THIS");
        System.out.println("finish");
    }
}
