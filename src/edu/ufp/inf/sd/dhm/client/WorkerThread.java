package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.AvailableDigestAlgorithms;
import edu.ufp.inf.sd.dhm.server.StringGroup;
import edu.ufp.inf.sd.dhm.server.states.TaskState;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// Possible algorithms: "MD4", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "RIPEMD128", "RIPEMD160"
// , "RIPEMD256", "RIPEMD320", "Tiger", "DHA256", e "FORK256".
public class WorkerThread implements Runnable {
    private TaskState taskState;
    private Worker worker;
    private List<String> words;
    private ArrayList<String> hashes;
    private int currentLine;
    private long deliveryTag;

    public WorkerThread(TaskState taskState, Worker worker) {
        this.taskState = taskState;
        this.worker = worker;
        this.hashes = worker.getHashes();
        this.currentLine = taskState.getStringGroup().getCeiling();
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    @Override
    public void run() {
        try {
            System.out.println("Thread from worker " + this.worker.getId() + " is getting started ...");
            this.populateStringGroupWords();                    // populate this.words with the words from @StringGroup
            this.work();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void work() throws NoSuchAlgorithmException, UnsupportedEncodingException, RemoteException, InterruptedException {
        String hashType = String.valueOf(this.worker.getHashType()).replace("_", "-");
        MessageDigest algorithm = MessageDigest.getInstance(hashType);
        System.out.println("printing digests");
        this.hashes.forEach(digest -> {
            System.out.println(digest);
        });
        for (String word : this.words) {
            //System.out.println("I'm currently in line " + this.currentLine);
            byte[] hashByte = algorithm.digest(word.getBytes(StandardCharsets.UTF_8));
            String digest = this.byteToString(hashByte);
            //System.out.println("Word -> " + word + " Digest ->  " + digest);
            if (this.hashes.contains(digest)) {
                // Match done!
                System.out.println("Worker#" + this.worker.getId() + " had a match w/ word: " + word + " w/ hash: "
                        + digest);
                this.worker.setCurrentLine(this.currentLine);
                this.worker.match(word,digest,this.deliveryTag);
                System.out.println("removig digest " + digest);
                this.hashes.remove(digest);  // removes hash from arraylist of hashes
                if(this.hashes.isEmpty()){
                    System.out.println("no more hashses , im kill ma self");
                    Thread.currentThread().interrupt();
                }
            }
            this.currentLine++;

            //Thread.sleep(1000);
        }
        System.out.println("I'm currently in line " + this.currentLine);
        System.out.println("Done with string group , going to tell the task!");
        this.worker.doneWithStringGroup(false,this.deliveryTag);
    }

    /**
     * Populates the ArrayList of words with only the words from the designated
     * StringGroup sent by the TaskState
     */
    private void populateStringGroupWords() {
        System.out.println("Populating thread string group words...");
        StringGroup stringGroup = this.taskState.getStringGroup();
        this.words = this.worker.getWords().subList(stringGroup.getCeiling(),
                stringGroup.getCeiling() + stringGroup.getDelta());
    }

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    /**
     * @param bytes w/ the digest
     * @return bytes in string
     */
    private String byteToString(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", 0xFF & b));
        }
        return hexString.toString();
    }

}
