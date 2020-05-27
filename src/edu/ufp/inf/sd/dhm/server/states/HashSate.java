package edu.ufp.inf.sd.dhm.server.states;

import edu.ufp.inf.sd.dhm.server.StringGroup;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Worker ---> Task
 */
public class HashSate implements Serializable {
    int workerId;
    WorkerStatus status;
    StringGroup original;           // StringGroup originally attached to this worker
    StringGroup pending;            // StringGroup pending to mine ( not mined yet )
    ArrayList<String> matches;      // ArrayList with all the matches , if exists

    public HashSate(WorkerStatus workerStatus, StringGroup original, StringGroup pending, ArrayList<String> matches,int workerId) {
        this.status = workerStatus;
        this.original = original;
        this.pending = pending;
        this.matches = matches;
        this.workerId = workerId;
    }

    public HashSate(WorkerStatus workerStatus, int workerId) {
        this.status = workerStatus;
        this.workerId = workerId;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public StringGroup getOriginal() {
        return original;
    }

    public StringGroup getPending() {
        return pending;
    }

    public ArrayList<String> getMatches() {
        return matches;
    }

    public Integer getWorkerId() {
        return this.workerId;
    }
}
