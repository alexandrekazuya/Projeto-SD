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
    private GatewayServ gateway;

    @PostConstruct
    public void registerClient() {
        try {
            WebSocketClientRMI client = new WebSocketClientRMI(pushController);
            Registry registry = LocateRegistry.getRegistry(gatewayHost, 1099);
            GatewayService gw = (GatewayService) registry.lookup(   "Gateway");
            gw.registerClient(client);
            
        } catch (Exception e) {
        }
    }
}
