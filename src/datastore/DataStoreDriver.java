package datastore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unified persistence interface. Manages in-memory data collections and serialized
 * file I/O. On startup, deserializes .dat files to restore the previous state;
 * on shutdown (close()), serializes all collections back to disk.
 */
public class DataStoreDriver {

    private final Path storagePath;

    private SessionLogger    sessionLogger;
    private CapacityMonitor  capacityMonitor;
    private TransactionArchive transactionArchive;

    private static final String SESSION_FILE     = "sessions.dat";
    private static final String CAPACITY_FILE    = "capacity.dat";
    private static final String TRANSACTION_FILE = "transactions.dat";

    public DataStoreDriver(Path storagePath) {
        this.storagePath = storagePath;
    }

    /** Initializes the storage directory and deserializes existing .dat files. */
    public void initialize() {
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sessionLogger      = loadOrCreate(SESSION_FILE,     new SessionLogger());
        capacityMonitor    = loadOrCreate(CAPACITY_FILE,    new CapacityMonitor());
        transactionArchive = loadOrCreate(TRANSACTION_FILE, new TransactionArchive());
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> T loadOrCreate(String filename, T defaultValue) {
        Path file = storagePath.resolve(filename);
        if (Files.exists(file)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
                return (T) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    /** Serializes all in-memory collections to .dat files. Call on application shutdown. */
    public void close() {
        persist(SESSION_FILE,     sessionLogger);
        persist(CAPACITY_FILE,    capacityMonitor);
        persist(TRANSACTION_FILE, transactionArchive);
    }

    private void persist(String filename, Serializable obj) {
        Path file = storagePath.resolve(filename);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearSessions() {
        sessionLogger.clearAllSessions();
        persist(SESSION_FILE, sessionLogger);
    }

    public void clearTransactions() {
        transactionArchive.clearAllTransactions();
        persist(TRANSACTION_FILE, transactionArchive);
    }

    public SessionLogger     getSessionLogger()      { return sessionLogger; }
    public CapacityMonitor   getCapacityMonitor()    { return capacityMonitor; }
    public TransactionArchive getTransactionArchive(){ return transactionArchive; }
}
