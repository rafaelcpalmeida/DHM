package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.StringGroup;
import edu.ufp.inf.sd.HashFinder.server.states.TaskState;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WorkerThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(WorkerThread.class.getName());
    private final TaskState taskState;
    private final Worker worker;
    private List<String> words;
    private final ArrayList<String> hashes;
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

    /**
     * Inicia uma thread caso o Worker esteja num TaskGroup
     */
    @Override
    public void run() {
        try {
            if (!this.worker.isStop()) {
                LOGGER.info("A iniciar thread do worker " + this.worker.getId());
                this.populateStringGroupWords();                    // populate this.words with the words from @StringGroup
                this.work();
            }
            LOGGER.info("Killing Thread");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Faz a conversão para o tipo de Hash escolhido
     * Faz a comparação entre as Hashs e os Digests
     */
    private void work() throws NoSuchAlgorithmException {
        String hashType = String.valueOf(this.worker.getHashType()).replace("_", "-");
        MessageDigest algorithm = MessageDigest.getInstance(hashType);
        LOGGER.info("Printing Digests");
        this.hashes.forEach(LOGGER::info);
        for (String word : this.words) {
            if (this.worker.isStop()) return;
            byte[] hashByte = algorithm.digest(word.getBytes(StandardCharsets.UTF_8));
            String digest = this.byteToString(hashByte);
            if (this.hashes.contains(digest)) {
                // Match done!
                LOGGER.info("Worker#" + this.worker.getId() + " Encontrou correspondência para a palavra -> " + word);
                this.worker.setCurrentLine(this.currentLine);
                this.worker.match(word, digest);
            }
            this.currentLine++;
        }
        this.worker.doneWithStringGroup(false, this.deliveryTag);
    }

    /**
     * Recebe TaskState com StringGroup a trabalhar
     */
    private void populateStringGroupWords() {
        LOGGER.info("A atribuir palavras à Thread...");
        StringGroup stringGroup = this.taskState.getStringGroup();
        this.words = this.worker.getWords().subList(stringGroup.getCeiling(),
                stringGroup.getCeiling() + stringGroup.getDelta());
    }

    /**
     * Conversão do Hash para String
     * @param bytes w/ the digest
     * @return bytes in string
     */
    private String byteToString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", 0xFF & b));
        }
        return hexString.toString();
    }

}
