package demo;


import com.codahale.metrics.ConsoleReporter;
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
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
@EnableRedisHttpSession
//@EnableEurekaClient  //or use EnableDiscoveryClient
@EnableDiscoveryClient
//@EnableFeignClients
@EnableCircuitBreaker
@SessionAttributes("vendors")
public class VendorApplication {

    //private final Meter requests = metrics.meter("requests");
    //final Histogram resultCounts = registry.histogram(name(ProductDAO.class, "result-counts");
    //resultCounts.update(results.size());

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter requestsAddVendorMetric = metrics.meter("requestsAddVendorMetric");
    private final Timer timerAddVendor = metrics.timer("timerAddVendor");
    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
   //reporter.start(1, TimeUnit.SECONDS);


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
