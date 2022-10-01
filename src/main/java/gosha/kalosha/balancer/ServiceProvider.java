package gosha.kalosha.balancer;

import java.util.List;

public class ServiceProvider {
    private final List<Service> services;
    private int current = 0;

    public ServiceProvider(List<Service> services) {
        this.services = services;
    }

    public synchronized Service pickNext() {
        Service service;
        var retries = 0;
        do {
            service = services.get(current);
            current = (current + 1) % services.size();
            ++retries;
        } while (!service.isAlive && retries < services.size() + 1); // +1 so that we will still send requests to
        return service;                                              // different services even if none are alive
    }
}
