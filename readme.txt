Pre-requisites

a) Docker Machine Running; Docker Required:
b) Virtual Environment Users: Virtual Box and Docker
c) Docker Redis must be running on 192.168.99.100

IDE: IntelliJ or STS
----------------------------------
//TODO: add restassured; add swagger; add archaius; add ribbon; http://localhost:8686/hystrix

 1. Start Redis from VM

    docker-compose up
    --or--
    docker run –d  –p “6379:6379”  redis

 2. Build all projects

    mvn clean package

 3. Start gateway application

 4. Start discovery application

 5. Start vendor-service

 6. Start vendor-ui

 7. Go to localhost:8080

 8. Login user/password

 9. Go to Vendor app

10. Go to localhost:8686/hystrix  ( Reminder --> 20 failures in 5 seconds is the default in Hystrix )
    http://localhost:9000/hystrix.stream

11. To test the Hystrix shut down the VendorServiceApplication
