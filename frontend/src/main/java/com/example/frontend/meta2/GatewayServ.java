package com.example.frontend.meta2;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import googol.common.GatewayService;

@Service
public class GatewayServ {
    private final GatewayService gateway;

    @Value("${gateway.host}")
    private String gatewayHost;

    public GatewayServ() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(gatewayHost, 1099);
        this.gateway = (GatewayService) registry.lookup("Gateway");
    }

    public GatewayService getGateway() {
        return this.gateway;
    }
}
