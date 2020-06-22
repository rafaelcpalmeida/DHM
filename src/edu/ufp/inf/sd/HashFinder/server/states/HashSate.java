package edu.ufp.inf.sd.HashFinder.server.states;

import edu.ufp.inf.sd.HashFinder.server.StringGroup;

import java.io.Serializable;

/**
 * Worker ---> Task
 */
public class HashSate implements Serializable {
    int workerId;
    String ownerName;
    WorkerStatus status;
    StringGroup original;
    StringGroup pending;
    String hash;
    String word;

    public HashSate(WorkerStatus workerStatus, StringGroup original, StringGroup pending, String hash,int workerId, String word, String ownerName) {
        this.status = workerStatus;
        this.original = original;
        this.pending = pending;
        this.hash = hash;
        this.workerId = workerId;
        this.word = word;
        this.ownerName = ownerName;
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

    public String getHash() {
        return hash;
    }

    public String getWord(){
        return word;
    }

    public Integer getWorkerId() {
        return this.workerId;
    }

    @Override
    public String toString() {
        return "HashSate{" +
                "workerId=" + workerId +
                ", status=" + status +
                ", original=" + original +
                ", pending=" + pending +
                ", hash='" + hash + '\'' +
                ", word='" + word + '\'' +
                '}';
    }

    public String getOwnerName() {
        return ownerName;
    }
}
