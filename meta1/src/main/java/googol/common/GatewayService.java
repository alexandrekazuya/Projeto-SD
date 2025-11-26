package googol.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

import googol.common.dto.SearchResult;

public interface GatewayService extends Remote {
    SearchResult[] searchWord(String[] terms, int page) throws RemoteException;
    String[] getIncomingLinks(String url) throws RemoteException;
    void putNewURL(String url) throws RemoteException;
    String takeNext() throws RemoteException;
    
    // Client callback registration for real-time stats updates
    void registerClient(SimpleClientCallback client) throws RemoteException;
    void unregisterClient(SimpleClientCallback client) throws RemoteException;

    // Stats methods for frontend
    String[][] getTop10Searches() throws RemoteException;
    java.util.Map<String, Integer> getBarrelStatus() throws RemoteException;
    java.util.Map<String, Double> getAverageResponseTimes() throws RemoteException;
    // Return the pre-formatted plain-text stats string (leaderboard + separator + barrels)
    String getPlainStatsString() throws RemoteException;
}
