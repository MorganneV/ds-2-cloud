package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Seat {
    private String airline;
    private UUID flightId;
    private UUID seatId;
    private LocalDateTime time;
    private String type;
    private String name;
    private double price;

    public Seat() {
    }

    public Seat(String airline, UUID flightId, UUID seatId, LocalDateTime time, String type, String name, double price) throws ExecutionException, InterruptedException {
        this.airline = airline;
        this.flightId = flightId;
        this.seatId = seatId;
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;

        DocumentReference docRef = Application.db.collection("seats").document(seatId.toString());
        Map<String, Object> data = new HashMap<>();
        data.put("Airline", airline);
        data.put("flightId", flightId.toString());
        data.put("seatId", seatId.toString());
        data.put("time", time.toString());
        data.put("type", type);
        data.put("name", name);
        data.put("price", price);
        //asynchronously write data
        ApiFuture<WriteResult> result = Application.db.collection("seats").document(seatId.toString()).set(data);
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

    public LocalDateTime getTime() {
        return this.time;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public double getPrice() {
        return this.price;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Seat)) {
            return false;
        }
        var other = (Seat) o;
        return this.airline.equals(other.airline)
                && this.flightId.equals(other.flightId)
                && this.seatId.equals(other.seatId);
    }

    @Override
    public int hashCode() {
        return this.airline.hashCode() * this.flightId.hashCode() * this.seatId.hashCode();
    }
}
