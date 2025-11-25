package googol.common;

import java.rmi.RemoteException;

/**
 * Optional simpler callback for clients that prefer a single plain-string stats update.
 * Implement this in clients that only need to receive and print the stats string.
 */
public interface SimpleClientCallback extends ClientCallback {
    void updateStatsString(String stats) throws RemoteException;
}
