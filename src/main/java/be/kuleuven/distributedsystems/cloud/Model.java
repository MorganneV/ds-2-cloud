package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.auth.WebSecurityConfig;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequestMapping("/api")
@RestController
public class Model {

    @Resource(name = "webClientBuilder")
    WebClient.Builder webClientBuilder;

    public String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    public String reliableAirline = "reliable-airline.com";
    public String unreliableAirline = "unreliable-airline.com";
    public List<String> airlines = Arrays.asList(reliableAirline);

    @Retryable
    @GetMapping("/getFlights")
    public List<Flight> getFlights(){
        List<Flight> flights = new ArrayList<>();
        for (String airline: airlines) {
            flights.addAll(webClientBuilder
                    .baseUrl("https://" + airline)
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

    @Retryable
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

    @Retryable
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

    @Retryable
    @GetMapping("/getAvailableSeats")
    public Map<String,List<Seat>> getAvailableSeats(@RequestParam("airline") String airline, @RequestParam("flightId") UUID flightId, @RequestParam("time") String time){
        List<Seat> seats = new ArrayList<>();
        String id = flightId.toString();
        seats.addAll(webClientBuilder
                .baseUrl("https://" + airline )
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(id)
                        .pathSegment("seats")
                        .queryParam("time", time)
                        .queryParam("available", "true")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block()
                .getContent());
        Map<String, List<Seat>> availableSeats = new HashMap<String, List<Seat>>();
        for (Seat seat: seats) {
            if (!availableSeats.containsKey(seat.getType())){
                availableSeats.put(seat.getType(), new ArrayList<Seat>());
            }
            availableSeats.get(seat.getType()).add(seat);
        }
        return availableSeats;
    }


    @Retryable
    @GetMapping("/getSeat")
    public Seat getSeat(@RequestParam("airline") String airline, @RequestParam("flightId") UUID flightId, @RequestParam("seatId") UUID seatId){
        Seat seat = webClientBuilder
                .baseUrl("https://" + airline)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .pathSegment(flightId.toString())
                        .pathSegment("seats")
                        .pathSegment(seatId.toString())
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {
                })
                .block();
        return seat;
    }

    private WebSecurityConfig webSecurityConfig = new WebSecurityConfig(new SecurityFilter());

    @Retryable(maxAttempts = 4)
    @PostMapping({"/confirmQuotes"})
    public void confirmQuotes(@RequestBody List<Quote> quotes) {
        String customer = webSecurityConfig.getUser().getEmail();
        String bookingReference = UUID.randomUUID().toString();
        try {
            for (Quote quote :
                    quotes) {
                if (!handledQuotes.contains(quote)) {
                    handledQuotes.add(quote);
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
                                    .queryParam("bookingReference", bookingReference)
                                    .queryParam("customer", customer)
                                    .queryParam("key", API_KEY)
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

            reservedTickets = new ArrayList<>();
            handledQuotes = new ArrayList<>();

            String hostPort = "localhost:8083";
            ManagedChannel channel = ManagedChannelBuilder.forTarget(hostPort).usePlaintext().build();
            try {
                TransportChannelProvider channelProvider =
                        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
                CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

                TopicName topicName = TopicName.of(Application.projectId, Application.topicId);
                Publisher publisher = Publisher.newBuilder(topicName).setCredentialsProvider(credentialsProvider).setChannelProvider(channelProvider).build();
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(Base64.getEncoder().encodeToString(
                        new ObjectMapper().writeValueAsString(quotes).getBytes()))).putAttributes("customer", customer).build();

                ApiFuture<String> future = publisher.publish(pubsubMessage);
                System.out.println("Message ID: " + future.get());
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
                channel.shutdown();
                channel.awaitTermination(1, TimeUnit.MINUTES);
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }
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
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    List<Ticket> reservedTickets = new ArrayList<>();
    List<Quote> handledQuotes = new ArrayList<>();
    List<Booking> reservedBookings = new ArrayList<>();

    public List<Booking> getReservedBookings() {
        return reservedBookings;
    }


    @Retryable
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


    @GetMapping("/getBookings")
    public List<Booking> getBookings(){
        String user = webSecurityConfig.getUser().getEmail();
        List<Booking> bookings = new ArrayList<>();
        for (Booking booking: getReservedBookings()) {
            if (booking.getCustomer().equals(user))
                bookings.add(booking);
        }
        return bookings;
    }


    @GetMapping("/getAllBookings")
    public List<Booking> getAllBookings(){
        return getReservedBookings();
    }


    @GetMapping("/getBestCustomers")
    public Set<String> getBestCustomers(){
        Map<String, Integer> customers = new HashMap<>();
        for (Booking booking: getReservedBookings()){
            String customer = booking.getCustomer();
            if(!customers.containsKey(customer)){
                customers.put(customer, 0);
            }
            Integer ticketsAmount = booking.getTickets().size();
            Integer newAmount = customers.get(customer) + ticketsAmount;
            customers.put(customer, newAmount);
        }

        Set<String> bestCustomer = new HashSet<>();
        Integer best = 0;
        for (String c: customers.keySet()) {
            Integer nbTickets = customers.get(c);
            if (nbTickets > best){
                bestCustomer.clear();
                bestCustomer.add(c);
                best = nbTickets;
            } else if (nbTickets.equals(best)){
                bestCustomer.add(c);
            }
        }

        return bestCustomer;
    }

}