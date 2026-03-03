package datastore;

import model.Session;
import model.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Transaction> transactions;

    public TransactionArchive() {
        this.transactions = new ArrayList<>();
    }

    /** Creates a completed transaction record from the given session and calculated fee. */
    public void archiveTransaction(Session session, double fee) {
        transactions.add(new Transaction(session, fee));
    }

    /** Returns all transactions for the specified date. */
    public List<Transaction> getTransactions(LocalDate date) {
        return transactions.stream()
                .filter(t -> t.getEntryTime().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    /** Returns total revenue collected on the specified date. */
    public double getRevenueForDate(LocalDate date) {
        return getTransactions(date).stream()
                .mapToDouble(Transaction::getFee)
                .sum();
    }

    /** Returns average session duration in minutes for the specified date. */
    public double getAverageDuration(LocalDate date) {
        List<Transaction> daily = getTransactions(date);
        if (daily.isEmpty()) return 0;
        return daily.stream()
                .mapToLong(t -> t.getDuration().toMinutes())
                .average()
                .orElse(0);
    }

    /** Writes all transaction records to a CSV file at the specified path. */
    public void exportToCsv(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("SessionId,SpaceId,Floor,EntryTime,ExitTime,Fee");
            for (Transaction t : transactions) {
                writer.printf("%s,%s,%d,%s,%s,%.2f%n",
                        t.getSessionId(), t.getSpaceId(), t.getFloor(),
                        t.getEntryTime(), t.getExitTime(), t.getFee());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearAllTransactions() {
        transactions.clear();
    }

    public List<Transaction> getAllTransactions() { return transactions; }
}
