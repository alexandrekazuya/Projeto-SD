package googol.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

import googol.common.dto.SearchResult;

public interface GatewayService extends Remote {
    SearchResult[] searchWord(String[] terms, int page) throws RemoteException;
    String[] getIncomingLinks(String url) throws RemoteException;
    void putNewURL(String url) throws RemoteException;
    String takeNext() throws RemoteException;
    //StatsDTO getStats() throws RemoteException;
}
