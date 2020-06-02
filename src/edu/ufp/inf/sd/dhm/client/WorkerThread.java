package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.states.TaskState;

public class WorkerThread implements Runnable{
    private TaskState taskState ;

    public WorkerThread(TaskState taskState){
        this.taskState = taskState;
    }

    @Override
    public void run() {
        System.out.println("thread -> working on taskstate ->" + this.taskState.toString());
    }

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }
}
