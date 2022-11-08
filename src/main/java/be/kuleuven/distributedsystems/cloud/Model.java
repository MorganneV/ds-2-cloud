package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Flight;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.retry.annotation.Retryable;
import javax.annotation.Resource;
import java.awt.print.Book;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public List<Flight> getFlights(){
        List<Flight> flights = new ArrayList<>();
        for (String airline: airlines) {
            flights.addAll(webClientBuilder
                            .baseUrl(airline)
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

    @GetMapping(("/getFlight/{airline}/{flightId}"))
    public Flight getFlight(@PathVariable String airline, @PathVariable UUID flightId){
        Flight flight = webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights/" + flightId.toString())
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Flight>() {})
                .block();
        return flight;
    }

    //TODO
    @GetMapping(("/getFlightTimes/{airline}/{flightId}"))
    public Collection<LocalDateTime> getFlightTimes(@PathVariable String airline, @PathVariable UUID flightId){
        Flight flight = getFlight(airline, flightId);
        Collection<LocalDateTime> result = new ArrayList<>();
        String id = flight.getFlightId().toString();
        var times = webClientBuilder
                .baseUrl("https://reliable-airline.com/")
                        .build()
                        .get()
                        .uri(uriBuilder -> uriBuilder.pathSegment("flights/" + id + "/times/").queryParam("key", API_KEY).build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<String>>() {})
                        .block();
        if (times != null) {
            for (String t : times) {
                result.add(LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            }
            return result;
        }
        return null;
    }

    //TODO
    @GetMapping("/getAvailableSeats/{airline}/{flightId}/{time}")
    public List<Seat> getAvailableSeats(@PathVariable String airline, @PathVariable UUID flightId, @PathVariable LocalDateTime time){
        List<Seat> seats = new ArrayList<>();
        String id = flightId.toString();
        String path = "/flights/" + id + "/seats";
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("time", time.toString());
        params.add("available","true");
        params.add("key", API_KEY);
        seats.addAll(webClientBuilder
                        .baseUrl("https://" + airline)
                        .build()
                        .get()
                        .uri(uriBuilder -> uriBuilder.pathSegment(path).queryParams(params).build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                        .block()
                        .getContent());
        return seats;
    }

    //todo
    @GetMapping("/getSeat/{airline}/{flightId}/{seatId}")
    public Seat getSeat(@PathVariable String airline, @PathVariable UUID flightId, @PathVariable UUID seatId){
        String path = "flights/" + flightId.toString() + "/seat/" + seatId.toString();
        Seat seat = webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment(path)
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {
                })
                .block();
        return seat;
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
