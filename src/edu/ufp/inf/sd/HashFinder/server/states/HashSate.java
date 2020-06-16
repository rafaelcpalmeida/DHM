package edu.ufp.inf.sd.HashFinder.server.states;

import edu.ufp.inf.sd.HashFinder.server.StringGroup;

import java.io.Serializable;

/**
 * Worker ---> Task
 */
public class HashSate implements Serializable {
    int workerId;
    WorkerStatus status;
    StringGroup original;           // StringGroup originally attached to this worker
    StringGroup pending;            // StringGroup pending to mine ( not mined yet )
    String hash;      // String w/ the match
    String word;      // String w/ the word found

    public HashSate(WorkerStatus workerStatus, StringGroup original, StringGroup pending, String hash, int workerId, String word) {
        this.status = workerStatus;
        this.original = original;
        this.pending = pending;
        this.hash = hash;
        this.workerId = workerId;
        this.word = word;
    }


    public WorkerStatus getStatus() {
        return status;
    }

    public String getHash() {
        return hash;
    }

    public String getWord() {
        return word;
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
}
