package backend.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteMatchmaking extends Remote {
    
    String createGame(String username, String gameName, int maxPlayers) throws RemoteException;
    
    List<String> listGames() throws RemoteException;
    
    String joinGame(int port) throws RemoteException;
    
    String updateGame(int port, int currentPlayers) throws RemoteException;
    
    String deleteGame(int port) throws RemoteException;
    
    String ping() throws RemoteException;
}
