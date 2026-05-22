package com.acme.fabric.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.acme.fabric.handler.FabricHandler;
import com.acme.fabric.handler.ControlPlaneHandler;
import com.acme.fabric.handler.PlatformFunctionHandler;
import com.acme.fabric.handler.SmsDispatchHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class FabricRouter {
    @Bean
    RouterFunction<ServerResponse> routes(FabricHandler fabricHandler, SmsDispatchHandler smsDispatchHandler,
                                          ControlPlaneHandler controlPlaneHandler,
                                          PlatformFunctionHandler platformFunctionHandler) {
        return route(GET("/api/v1/fabric/topology"), fabricHandler::topology)
                .andRoute(POST("/api/v1/fabric/events"), fabricHandler::publishEvent)
                .andRoute(GET("/api/v1/fabric/stream"), fabricHandler::stream)
                .andRoute(POST("/api/v1/sms/dispatches"), smsDispatchHandler::create)
                .andRoute(POST("/sms/dispatches"), smsDispatchHandler::create)
                .andRoute(GET("/api/v1/sms/gateways/status"), smsDispatchHandler::status)
                .andRoute(POST("/api/v1/systems/servers"), controlPlaneHandler::provisionServer)
                .andRoute(GET("/api/v1/security/firewall/rules"), controlPlaneHandler::firewallRules)
                .andRoute(POST("/api/v1/security/firewall/rules"), controlPlaneHandler::createFirewallRule)
                .andRoute(GET("/api/v1/security/honeypots/incidents"), controlPlaneHandler::honeypotIncidents)
                .andRoute(POST("/api/v1/networking/packet-trace"), controlPlaneHandler::tracePacket)
                .andRoute(POST("/api/v1/transfers/staged-payloads"), controlPlaneHandler::stageFile)
                .andRoute(POST("/api/v1/transfers/pipelines"), controlPlaneHandler::executePipeline)
                .andRoute(POST("/api/v1/networking/telnet/command"), controlPlaneHandler::terminalCommand)
                .andRoute(POST("/api/v1/quantum/circuits/executions"), controlPlaneHandler::executeQuantumCircuit)
                .andRoute(POST("/api/v1/networking/socket-listeners"), controlPlaneHandler::spawnSocket)
                .andRoute(POST("/api/v1/network/identity/scramble"), controlPlaneHandler::scrambleIdentity)
                .andRoute(POST("/api/v1/platform/functions/blueprints"), platformFunctionHandler::createBlueprint)
                .andRoute(POST("/api/v1/platform/polyglot/scripts/evaluate"), platformFunctionHandler::evaluateScript);
    }
}
