package com.example.frontend.meta2;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import googol.common.GatewayService;

@Component
public class RegisterClient {

    @Autowired
    private WebSocketPushController pushController;

    @Value("${gateway.host}")
    private String gatewayHost;

    @Autowired
    @PostConstruct
    public void registerClient() {
        System.out.println("[RegisterClient] Attempting RMI registration to gateway host: " + gatewayHost + ":1099");
        try {
            WebSocketClientRMI client = new WebSocketClientRMI(pushController);
            Registry registry = LocateRegistry.getRegistry(gatewayHost, 1099);
            GatewayService gw = (GatewayService) registry.lookup("Gateway");
            gw.registerClient(client);
            System.out.println(
                    "[RegisterClient] Successfully registered RMI client with gateway at " + gatewayHost + ":1099");
        } catch (Exception e) {
            System.err.println("[RegisterClient] Failed to register with gateway: " + e.getClass().getName() + " - "
                    + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
