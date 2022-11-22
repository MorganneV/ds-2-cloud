package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Ticket {
    private String airline;
    private UUID flightId;
    private UUID seatId;
    private UUID ticketId;
    private String customer;
    private String bookingReference;

    public Ticket() {
    }

    public Ticket(String airline, UUID flightId, UUID seatId, UUID ticketId, String customer, String bookingReference) throws ExecutionException, InterruptedException {
        this.airline = airline;
        this.flightId = flightId;
        this.seatId = seatId;
        this.ticketId = ticketId;
        this.customer = customer;
        this.bookingReference = bookingReference;

        DocumentReference docRef = Application.db.collection("tickets").document(ticketId.toString());
        Map<String, Object> data = new HashMap<>();
        data.put("Airline", airline);
        data.put("flightId", flightId.toString());
        data.put("seatId", seatId.toString());
        data.put("ticketId", ticketId.toString());
        data.put("customer", customer);
        data.put("bookingReference", bookingReference);
        //asynchronously write data
        ApiFuture<WriteResult> result = Application.db.collection("tickets").document(ticketId.toString()).set(data);
        result.get();
    }

    public String getAirline() {
        return airline;
    }

    public UUID getFlightId() {
        return flightId;
    }

    public UUID getSeatId() {
        return this.seatId;
    }

    public UUID getTicketId() {
        return this.ticketId;
    }

    public String getCustomer() {
        return this.customer;
    }

    public String getBookingReference() {
        return this.bookingReference;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ticket)) {
            return false;
        }
        var other = (Ticket) o;
        return this.ticketId.equals(other.ticketId)
                && this.seatId.equals(other.seatId)
                && this.flightId.equals(other.flightId)
                && this.airline.equals(other.airline);
    }

    @Override
    public int hashCode() {
        return this.airline.hashCode() * this.flightId.hashCode() * this.seatId.hashCode() * this.ticketId.hashCode();
    }
}
