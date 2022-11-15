package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import org.eclipse.jetty.util.DateCache;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
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

    @GetMapping(("/getFlight"))
    public Flight getFlight(@RequestParam("airline") String airline, @RequestParam("flightId") UUID flightId){
        Flight flight = webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(flightId.toString())
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Flight>() {})
                .block();
        return flight;
    }

    @GetMapping(("/getFlightTimes"))
    public List<LocalDateTime> getFlightTimes(@RequestParam("airline") String airline, @RequestParam("flightId") UUID flightId){
        List<LocalDateTime> times = new ArrayList<>();
        String id = flightId.toString();
        times.addAll(webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(id)
                        .pathSegment("times")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {})
                .block()
                .getContent());
        return times;
    }

    //TODO
    @GetMapping("/getAvailableSeats")
    public Map<String,List<Seat>> getAvailableSeats(@RequestParam("airline") String airline, @RequestParam("flightId") UUID flightId, @RequestParam("time") LocalDateTime time){
        List<Seat> seats = new ArrayList<>();
        String id = flightId.toString();
        String path = "/flights/" + id + "/seats";
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("time", time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        params.add("available","true");
        params.add("key", API_KEY);
        seats.addAll(webClientBuilder
                .baseUrl("https://" + airline )
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(id)
                        .pathSegment("seats")
                        .queryParams(params).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block()
                .getContent());
        Map<String, List<Seat>> types = new HashMap<String, List<Seat>>();
        for (Seat seat: seats) {
            if (!types.containsKey(seat.getType())){
                types.put(seat.getType(), new ArrayList<Seat>());
            }
            types.get(seat.getType()).add(seat);
        }
        return types;
    }

    //todo
    @GetMapping("/getSeat")
    public Seat getSeat(@RequestParam("airline") String airline, @RequestParam("flightId") UUID flightId, @RequestParam("seatId") UUID seatId){
        String path = "flights/" + flightId.toString() + "/seat/" + seatId.toString();
        Seat seat = webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(flightId.toString())
                        .pathSegment("seat")
                        .pathSegment(seatId.toString())
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {
                })
                .block();
        return seat;
    }

    //todo
    @PostMapping({"/confirmQuotes","/cart"})
    public void confirmQuotes(List<Quote> quotes, String customer) {
        try {
            for (Quote quote :
                    quotes) {
                if (!handledQuotes.contains(quote)) {
                    handledQuotes.add(quote);
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("customer", customer);
                    params.add("key", API_KEY);
                    webClientBuilder
                            .baseUrl("https://" + quote.getAirline())
                            .build()
                            .put()
                            .uri(uriBuilder -> uriBuilder
                                    .pathSegment("flights")
                                    .pathSegment(quote.getFlightId().toString())
                                    .pathSegment("seats")
                                    .pathSegment(quote.getSeatId().toString())
                                    .pathSegment("ticket")
                                    .queryParams(params)
                                    .build())
                            .body(BodyInserters.fromValue("customer"))
                            .retrieve()
                            .onStatus(s -> s.value() == 409, response -> Mono.error(new IllegalStateException()))
                            .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                            })
                            .block();
                    Ticket ticket = getTicket(quote.getAirline(), quote.getFlightId(), quote.getSeatId());
                    reservedTickets.add(ticket);
                }
            }
            Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), reservedTickets, customer);
            reservedBookings.add(booking);

            reservedTickets.clear();
            handledQuotes.clear();
        } catch(IllegalStateException e) {
            e.printStackTrace();
            for (Ticket ticket: reservedTickets) {
                webClientBuilder
                        .baseUrl("https://" + ticket.getAirline())
                        .build()
                        .delete()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("flights")
                                .pathSegment(ticket.getFlightId().toString())
                                .pathSegment("seats")
                                .pathSegment(ticket.getSeatId().toString())
                                .pathSegment("ticket")
                                .pathSegment(ticket.getTicketId().toString())
                                .queryParam("key", API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                        })
                        .block();
            }
        }
    }

    List<Ticket> reservedTickets = new ArrayList<>();
    List<Quote> handledQuotes = new ArrayList<>();
    List<Booking> reservedBookings = new ArrayList<>();

    public List<Booking> getReservedBookings() {
        return reservedBookings;
    }



    private Ticket getTicket(String airline, UUID flightId, UUID seatId){
        Ticket ticket = webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(flightId.toString())
                        .pathSegment("seats")
                        .pathSegment(seatId.toString())
                        .pathSegment("ticket")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                })
                .block();
        return ticket;
    }

    //todo
    @GetMapping({"/getBookings", "/account"})
    public List<Booking> getBookings(String user){
        List<Booking> bookings = new ArrayList<>();
        for (Booking booking: getReservedBookings()) {
            if (booking.getCustomer().equals(user))
                bookings.add(booking);
        }
        return bookings;
    }


    //todo
    @GetMapping({"/getAllBookings", "/manager"})
    public List<Booking> getAllBookings(){
        return getReservedBookings();
    }

    //todo
    @GetMapping({"/getBestCustomers", "/manager"})
    public Set<String> getBestCustomers(){
        Map<String, Integer> topCustomers = new HashMap<>();
        for (Booking booking: getReservedBookings()) {
            if (!topCustomers.containsKey(booking.getCustomer())){
                topCustomers.put(booking.getCustomer(), 0);
            }
            topCustomers.put(booking.getCustomer(), topCustomers.get(booking.getCustomer()) + booking.getTickets().size());
        }

        Set<String> bestCustomer = new HashSet<>();
        Integer amount = 0;
        for (String customer : topCustomers.keySet()) {
            Integer nbTickets = topCustomers.get(customer);
            if (nbTickets > amount){
                amount = nbTickets;
                bestCustomer.clear();
                bestCustomer.add(customer);
            } else if (nbTickets.equals(amount)){
                bestCustomer.add(customer);
            }
        }
        return bestCustomer;
    }

//    @GetMapping("/cart")
//    public List<Quote> viewCart{
//
//    }



}