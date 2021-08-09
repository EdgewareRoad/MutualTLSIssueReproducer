package mutualtlsissue;

import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomePageHandler {
    Logger logger = LoggerFactory.getLogger(HomePageHandler.class);

    @RequestMapping(path="/")
    public String getSimpleResponse()
    {
        String response = "Hello and welcome. The current time here is " + LocalTime.now().toString();
        logger.info("Home page called. Response = {}", response);
        return response;
    }
}
