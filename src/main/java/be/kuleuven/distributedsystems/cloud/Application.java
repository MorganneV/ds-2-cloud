package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {


    static String projectId = "demo-distributed-systems-kul";
    static String subscriptionId = "subscription";
    static String topicId = "topic";
    static String pushEndpoint = "http://localhost:8080/api/confirmQuotes";

    public static Firestore db;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));

        // Start Spring Boot application
        ApplicationContext context = SpringApplication.run(Application.class, args);
        createPushSubscription(projectId,subscriptionId,topicId,pushEndpoint);

        //set up firestore
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(projectId)
                        .setCredentials(new FirestoreOptions.EmulatorCredentials())
                        .setEmulatorHost("localhost:8084")
                        .build();
        db = firestoreOptions.getService();
        System.out.println("db is:" + db.toString());
    }

    @Bean
    public boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    @Bean
    public String projectId() {
        return "demo-distributed-systems-kul";
    }

    /*
     * You can use this builder to create a Spring WebClient instance which can be used to make REST-calls.
     */
    @Bean
    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
        return configurer.registerHypermediaTypes(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
    }

    @Bean
    HttpFirewall httpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    public static void createPushSubscription(String projectId, String subscriptionId, String topicId, String pushEndpoint) throws IOException {
        String hostPort = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostPort).usePlaintext().build();
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(NoCredentialsProvider.create())
                .setTransportChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)))
                .build())) {
            TopicName topicName = TopicName.of(projectId, topicId);
            try {
                TopicAdminClient topicClient = TopicAdminClient.create(TopicAdminSettings.newBuilder()
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .setTransportChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)))
                        .build());
                topicClient.createTopic(topicName);
            } catch (Exception e) {
            }
            ;
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();
            System.out.println("Endpoint: " + pushEndpoint);

            // Create a push subscription with default acknowledgement deadline of 10 seconds.
            // Messages not successfully acknowledged within 10 seconds will get resent by the server.
            try {
                Subscription subscription =
                        subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
                System.out.println("Created push subscription: " + subscription.getName());
            } catch (Exception e) {
                System.out.println("Subscription already exists: " + subscriptionAdminClient.getSubscription(subscriptionName).getName());
            }
            ;
            channel.shutdown();
            channel.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}