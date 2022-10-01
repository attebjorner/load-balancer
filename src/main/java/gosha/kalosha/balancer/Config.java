package gosha.kalosha.balancer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Set;

public class Config {

    public int port = 8080;
    public List<ServiceConfig> services = List.of();
    public long delay = 3000;
    public int threadPoolSize = 6;
    public Set<Integer> extraCodesToFailOn = Set.of();
    public long timeout = 1000;

    public static Config load() throws FileNotFoundException {
        final var configPath = System.getenv("BALANCER_CONFIG");
        final var yaml = new Yaml(new Constructor(Config.class));
        final var configReader = new FileReader(configPath);
        return yaml.loadAs(configReader, Config.class);
    }
}
