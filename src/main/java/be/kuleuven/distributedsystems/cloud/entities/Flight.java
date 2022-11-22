package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Flight {
    private String airline;
    private UUID flightId;
    private String name;
    private String location;
    private String image;

    public Flight() {
    }

    public Flight(String airline, UUID flightId, String name, String location, String image) throws ExecutionException, InterruptedException {
        this.airline = airline;
        this.flightId = flightId;
        this.name = name;
        this.location = location;
        this.image = image;

        DocumentReference docRef = Application.db.collection("flights").document(flightId.toString());
        Map<String, Object> data = new HashMap<>();
        data.put("Airline", airline.toString());
        data.put("flightId", flightId.toString());
        data.put("name", name);
        data.put("location", location);
        data.put("image", image.toString());
        //asynchronously write data
        ApiFuture<WriteResult> result = Application.db.collection("flights").document(flightId.toString()).set(data);
        result.get();
    }

    public String getAirline() {
        return airline;
    }

    public UUID getFlightId() {
        return flightId;
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return this.location;
    }

    public String getImage() {
        return this.image;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Flight)) {
            return false;
        }
        var other = (Flight) o;
        return this.airline.equals(other.airline)
                && this.flightId.equals(other.flightId);
    }

    @Override
    public int hashCode() {
        return this.airline.hashCode() * this.flightId.hashCode();
    }
}
