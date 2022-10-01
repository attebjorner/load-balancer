package gosha.kalosha.balancer;

public class ServiceConfig {

    public String healthUrl;
    public int failureThreshold = 3;
    public int successThreshold = 3;
}
