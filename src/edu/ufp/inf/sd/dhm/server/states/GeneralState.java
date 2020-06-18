package edu.ufp.inf.sd.dhm.server.states;

import edu.ufp.inf.sd.dhm.server.AvailableDigestAlgorithms;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Task --> Fanout --> Workers
 */
public class GeneralState implements Serializable {
    private ArrayList<String> hashes = new ArrayList<>();
    private AvailableDigestAlgorithms hashType;
    private String wordsUrl;
    private boolean pause;
    private boolean resume;

    public GeneralState(ArrayList<String> hashes, boolean pause, AvailableDigestAlgorithms hashType, String wordsUrl, boolean resume) {
        this.hashes = hashes;
        this.pause = pause;
        this.hashType = hashType;
        this.wordsUrl = wordsUrl;
        this.resume = resume;
    }

    public ArrayList<String> getHashes() {
        return hashes;
    }

    public boolean isPause() {
        return pause;
    }

    public boolean isResume() {
        return resume;
    }

    public AvailableDigestAlgorithms getHashType() {
        return hashType;
    }

    public String getWordsUrl() {
        return wordsUrl;
    }

    @Override
    public String toString() {
        return "GeneralState{" +
                "hashes=" + hashes +
                ", hashType=" + hashType +
                ", wordsUrl='" + wordsUrl + '\'' +
                ", pause=" + pause +
                '}';
    }
}
