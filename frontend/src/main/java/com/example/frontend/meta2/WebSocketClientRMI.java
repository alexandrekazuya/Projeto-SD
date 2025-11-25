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
        try {
            int len = (stats == null) ? 0 : stats.length();
            System.out.println("[WebSocketClientRMI] Received updateStatsString (len=" + len + ")");
            if (len > 0) {
                String preview = (stats != null && stats.length() > 300) ? stats.substring(0,300) + "..." : stats;
                System.out.println("[WebSocketClientRMI] Preview:\n" + preview);
            } else {
                System.out.println("[WebSocketClientRMI] stats is empty or null");
            }
        } catch (Exception e) {
            System.err.println("[WebSocketClientRMI] Debug logging failed: " + e.getMessage());
        }

        messagingController.sendToClients(stats);
    }
}
