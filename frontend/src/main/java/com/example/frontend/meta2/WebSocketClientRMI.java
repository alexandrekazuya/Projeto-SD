package com.example.frontend.meta2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import googol.common.SimpleClientCallback;

public class WebSocketClientRMI extends UnicastRemoteObject implements SimpleClientCallback {

    private final WebSocketPushController messagingController;

    public WebSocketClientRMI(WebSocketPushController messagingController) throws RemoteException {
        super(); // Required by UnicastRemoteObject
        this.messagingController = messagingController;
    }

    @Override
    public void updateStatsString(String stats) throws RemoteException {
        messagingController.sendToClients(stats);
    }
}
