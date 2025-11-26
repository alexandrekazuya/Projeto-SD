package googol.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SimpleClientCallback extends Remote {
    void updateStatsString(String stats) throws RemoteException;
}