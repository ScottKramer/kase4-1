package demo;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
@EnableRedisHttpSession
@EnableDiscoveryClient
@EnableCircuitBreaker
@SessionAttributes("vendors")
public class VendorApplication {

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter requestsAddVendorMetric = metrics.meter("requestsAddVendorMetric");
    private final Timer timerAddVendor = metrics.timer("timerAddVendor");

    //Metrics Reporting
    public VendorApplication() {

        // DropWizard Metrics Console Reporting
        //ConsoleReporter metricsConsoleReporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        //metricsConsoleReporter.start(10, TimeUnit.SECONDS);

        // Graphite Reporting to Graphite Server using Dropwizard Metrics
        //final Graphite graphite = new Graphite(new InetSocketAddress("192.168.99.100", 2003));
        //final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(metrics).prefixedWith("demo.kase4.com").convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(graphite);
        //graphiteReporter.start(1, TimeUnit.MINUTES);

    }


    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private IDGeneratorService idGeneratorService;

    @RequestMapping("/user")
    public Map<String, String> user(Principal user) {
        return Collections.singletonMap("name", user.getName());
    }

    public static void main(String[] args) {
        SpringApplication.run(VendorApplication.class, args);
    }

    @Configuration
    @Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
    protected static class SecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http
                    .httpBasic().and()
                    .authorizeRequests()
                    .antMatchers("/index.html", "/", "/hystrix.stream", "/turbine.stream").permitAll()
                    .anyRequest().hasRole("USER");
            // @formatter:on
        }
    }

    @RequestMapping(value = "/vendorInfo", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Vendor> vendorInfo(@ModelAttribute("vendors") List<Vendor> vendors) {
        return vendors;
    }

    @RequestMapping(value = "/addVendor", method = RequestMethod.POST)
    public
    @ResponseBody
    void addVendor(@ModelAttribute("vendors") List<Vendor> vendors, @RequestBody Vendor vendor) {
        requestsAddVendorMetric.mark();
        timerAddVendor.time();
        String identifier = idGeneratorService.generateIdentifier(serviceUrl() + "/vendor/idGenerator");
        vendor.setIdentifier(identifier);
        vendors.add(vendor);
        timerAddVendor.time().stop();
    }

    public String serviceUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances("vendor-service");
        return toURLString(instances.stream().findFirst().get());
    }

    String toURLString(ServiceInstance server) {
        try {
            return server.getUri().toURL().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @ModelAttribute("vendors")
    public List<Vendor> getFormData() {
        return new ArrayList<Vendor>();
    }


    @Autowired
    private RestTemplate restTemplate;

    public String generateIdentifier(String serviceUrl) {
        String identifier = restTemplate.getForObject(serviceUrl, String.class);
        return identifier;
    }
}
