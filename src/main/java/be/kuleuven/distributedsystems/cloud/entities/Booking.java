package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Booking {
    private UUID id;
    private LocalDateTime time;
    private List<Ticket> tickets;
    private String customer;

    public Booking(UUID id, LocalDateTime time, List<Ticket> tickets, String customer) throws ExecutionException, InterruptedException {
        this.id = id;
        this.time = time;
        this.tickets = tickets;
        this.customer = customer;

        DocumentReference docRef = Application.db.collection("bookings").document(id.toString());
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("time", time.toString());
        data.put("tickets", 1815);
        data.put("customer", customer.toString());
        //asynchronously write data
        ApiFuture<WriteResult> result = Application.db.collection("bookings").document(id.toString()).set(data);
        result.get();
    }



    public UUID getId() {
        return this.id;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public String getCustomer() {
        return this.customer;
    }
}
