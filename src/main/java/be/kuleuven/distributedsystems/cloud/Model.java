package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Flight;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.retry.annotation.Retryable;
import javax.annotation.Resource;
import java.awt.print.Book;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;

@RequestMapping("/api")
@RestController
public class Model {

    @Resource(name = "webClientBuilder")
    WebClient.Builder webClientBuilder;

    public String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    public String reliableAirline = "https://reliable-airline.com";
    public String unreliableAirline = "https://unreliable-airline.com";
    public List<String> airlines = Arrays.asList(reliableAirline);

    @Retryable
    @GetMapping("/getFlights")
    public Collection<Flight> getFlights(){
        Collection<Flight> flights = new ArrayList<>();
        for (String airline: airlines) {
            flights.addAll(webClientBuilder
                            .baseUrl("https://reliable-airline.com")
                            .build()
                            .get()
                            .uri(uriBuilder -> uriBuilder.pathSegment("flights").queryParam("key", API_KEY).build())
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<CollectionModel<Flight>>() {})
                            .block()
                            .getContent());

        }
        return flights;
    }

    public Flight getFlight(String airline, UUID flightId){
        Collection<Flight> flights = getFlights();
        for (Flight flight: flights) {
            if(flight.getAirline().equals(airline) && flight.getFlightId().equals(flightId)){
                return flight;
            }
        }
        return null;
    }

    //TODO
    public List<LocalDateTime> getFlightTimes(String airline, UUID flightId){
        return null;
    }

    //TODO
    public List<Seat> getAvailableSeats(String airline, UUID flightId, LocalDateTime time){
        return null;
    }

    //todo
    public Seat getSeat(String airline, UUID flightId, UUID seatId){
        return null;
    }

    //todo
    public void confirmQuotes(){}

    //todo
    public Ticket getTicket(String airline, UUID flightId, UUID seatId){
        return null;
    }

    //todo
    public List<Booking> getBookings(String user){
        return null;
    }

    //todo
    public List<Booking> getAllBookings(){
        return null;
    }

    //todo
    public List<String> getBestCustomers(){
        return null;
    }



}
