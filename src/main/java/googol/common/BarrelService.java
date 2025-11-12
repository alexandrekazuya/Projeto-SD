package googol.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

import googol.common.dto.PageDTO;
import googol.common.dto.SearchResult;
//import googol.common.dto.StatsDTO;

public interface BarrelService extends Remote {
    void sendPage(PageDTO page) throws RemoteException;//downloader -> barrel
    SearchResult[] searchWord(String[] terms, int page) throws RemoteException;
    String[] getIncomingLinks(String url) throws RemoteException;
    int getIndexSize() throws RemoteException;
}
