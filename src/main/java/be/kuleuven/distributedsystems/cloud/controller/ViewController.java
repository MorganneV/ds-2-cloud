package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.Flight;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.CachingResourceTransformer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class ViewController {
    private final Model model;

    @Autowired
    public ViewController(Model model){
        this.model = model;
    }

//    @GetMapping({"/flights/*/*", "/flights/*/*/*", "/cart", "/account", "/manager", "/login"})
//    public String spa() {
//        return "forward:/";
//    }

    @GetMapping("/_ah/warmup")
    public void warmup() {
    }

    @GetMapping({"/","/flights"})
    public ModelAndView getFlights() {
        ModelAndView modelAndView = new ModelAndView("flights");
        modelAndView.addObject("flights", this.model.getFlights());
        return modelAndView;
    }

//
//    @GetMapping("flights/{airline}/{flightId}")
//    public ModelAndView getFlightTimes(@PathVariable String airline, @PathVariable UUID flightId){
//        ModelAndView modelAndView = new ModelAndView("flight_times");
//        modelAndView.addObject("flight", this.model.getFlight(airline, flightId));
//        modelAndView.addObject("flightTimes", this.model.getFlightTimes(airline,flightId).stream().sorted().collect(Collectors.toList()));
//        return modelAndView;
//    }
//
//    @GetMapping("flights/{airline}/{flightId}/{time}")
//    public ModelAndView getFlightSeats(@PathVariable String airline, @PathVariable UUID flightId, @PathVariable LocalDateTime time){
//        ModelAndView modelAndView = new ModelAndView("flight_seats");
//        modelAndView.addObject("flight", this.model.getFlight(airline, flightId));
//        modelAndView.addObject("time", time.format(DateTimeFormatter.ofPattern("d MMM uuuu H:mm")));
//
//        return ModelAndView;
//    }


//
//    )
}
