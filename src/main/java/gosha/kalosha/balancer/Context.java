package gosha.kalosha.balancer;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Context {

    public final ExecutorService executors;
    public final Config config;
    public final List<Service> services;

    public Context(final Config config) {
        this.config = config;
        this.executors = Executors.newFixedThreadPool(config.threadPoolSize);
        this.services = config.services.stream().map(this::configureService).toList();
    }

    private Service configureService(final ServiceConfig config) {
        try {
            return new Service(config.healthUrl, config.failureThreshold, config.successThreshold);
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Unable to create context because of bad service URL: ", ex);
        }
    }
}
