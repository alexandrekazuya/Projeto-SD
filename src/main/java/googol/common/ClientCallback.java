package googol.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ClientCallback extends Remote {
    void updateTop10Searches(String[][] top10) throws RemoteException;
    void updateBarrelStatus(Map<String, Integer> barrelStats) throws RemoteException;
    void updateResponseTimes(Map<String, Double> responseTimes) throws RemoteException;
}
