package com.acme.fabric.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.acme.fabric.handler.FabricHandler;
import com.acme.fabric.handler.SmsDispatchHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class FabricRouter {
    @Bean
    RouterFunction<ServerResponse> routes(FabricHandler fabricHandler, SmsDispatchHandler smsDispatchHandler) {
        return route(GET("/api/v1/fabric/topology"), fabricHandler::topology)
                .andRoute(POST("/api/v1/fabric/events"), fabricHandler::publishEvent)
                .andRoute(GET("/api/v1/fabric/stream"), fabricHandler::stream)
                .andRoute(POST("/sms/dispatches"), smsDispatchHandler::create);
    }
}
