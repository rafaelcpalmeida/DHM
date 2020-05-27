package edu.ufp.inf.sd.dhm.server.states;

import edu.ufp.inf.sd.dhm.server.StringGroup;
import edu.ufp.inf.sd.dhm.server.TaskGroup;

import java.io.Serializable;

/**
 * Task ---> Worker
 */
public class TaskState implements Serializable {
    private final StringGroup stringGroup;

    public TaskState(StringGroup stringGroup){
        this.stringGroup = stringGroup;
    }

    public StringGroup getStringGroup() {
        return stringGroup;
    }

    @Override
    public String toString() {
        return "TaskState -> ceiling: " + this.stringGroup.getCeiling() + " delta: " + this.stringGroup.getDelta();
    }
}
