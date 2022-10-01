package gosha.kalosha.balancer;

import java.net.MalformedURLException;
import java.net.URL;

public class Service {

    public final String healthUrl;
    private final int failureThreshold;
    private final int successThreshold;
    public String host;
    public int port;
    private int timesFailed;
    private int timesSucceeded;
    public volatile boolean isAlive = true;

    public Service(String healthUrl, int failureThreshold, int successThreshold) throws MalformedURLException {
        this.healthUrl = healthUrl;
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        final var parsedHealthUrl = new URL(healthUrl);
        this.host = parsedHealthUrl.getHost();
        this.port = parsedHealthUrl.getPort() < 0 ? 80 : parsedHealthUrl.getPort();
    }

    public synchronized void tryReset() {
        if (timesSucceeded <= successThreshold) {
            ++timesSucceeded;
        } else {
            timesFailed = 0;
            isAlive = true;
        }
    }

    public synchronized void countFail() {
        if (timesFailed <= failureThreshold) {
            ++timesFailed;
        } else {
            timesSucceeded = 0;
            isAlive = false;
        }
    }
}
