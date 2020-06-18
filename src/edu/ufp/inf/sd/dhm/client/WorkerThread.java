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
import java.util.logging.Logger;

// Possible algorithms: "MD4", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "RIPEMD128", "RIPEMD160"
// , "RIPEMD256", "RIPEMD320", "Tiger", "DHA256", e "FORK256".
public class WorkerThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(WorkerThread.class.getName());
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
            if(!this.worker.isStop()){
                //LOGGER.info("Thread from worker " + this.worker.getId() + " is getting started ...");
                this.populateStringGroupWords();                    // populate this.words with the words from @StringGroup
                this.work();
            }
            //LOGGER.info("stopping thread");
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
        //LOGGER.info("printing digests");
        //this.hashes.forEach(digest -> {
        //    LOGGER.info(digest);
        //});
        for (String word : this.words) {
            //LOGGER.info("I'm currently in line " + this.currentLine);
            if(this.worker.isStop()) return;
            if(this.worker.isPause()){
                this.pauseWork();   // pauses the work being done
            }
            byte[] hashByte = algorithm.digest(word.getBytes(StandardCharsets.UTF_8));
            String digest = this.byteToString(hashByte);
            //LOGGER.info("Word -> " + word + " Digest ->  " + digest);
            if (this.hashes.contains(digest)) {
                // Match done!
                LOGGER.info("Worker#" + this.worker.getId() + " had a match w/ word: " + word + " w/ hash: "
                        + digest);
                this.worker.setCurrentLine(this.currentLine);
                this.worker.match(word,digest,this.deliveryTag);
                //LOGGER.info("removig digest " + digest);
                //this.hashes.remove(digest);  // removes hash from arraylist of hashes
                if(this.hashes.isEmpty()){
                    //LOGGER.info("NO MORE HASHESSSSSSSSSSS");
                    //Thread.currentThread().interrupt();
                }
            }
            this.currentLine++;
            //Thread.sleep(1000);
        }
        //LOGGER.info("I'm currently in line " + this.currentLine);
        //LOGGER.info("Done with string group , going to tell the task!");
        //LOGGER.info("DELIVERY TAG-> " + this.deliveryTag);
        this.worker.doneWithStringGroup(false,this.deliveryTag);
    }

    /**
     * While the flag is paused , do nothing
     */
    private void pauseWork() {
        while(this.worker.isPause()){
            //LOGGER.info("pausing cycle ....");
            int doSomething = 1 + 1;
        }
    }

    /**
     * Populates the ArrayList of words with only the words from the designated
     * StringGroup sent by the TaskState
     */
    private void populateStringGroupWords() {
        //LOGGER.info("Populating thread string group words...");
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
            hexString.append(String.format("%02X", 0xFF & b));
        }
        return hexString.toString();
    }

    //TODO pause work() by user

}
