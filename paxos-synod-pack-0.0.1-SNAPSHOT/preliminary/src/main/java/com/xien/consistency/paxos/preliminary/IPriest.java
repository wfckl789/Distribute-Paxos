package com.xien.consistency.paxos.preliminary;

import com.xien.consistency.paxos.preliminary.message.*;
import com.xien.consistency.paxos.preliminary.model.PriestName;
import com.xien.consistency.paxos.preliminary.model.Vote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author xien
 * @version 2022年05月18日 21:02 xien
 */
public interface IPriest extends Remote {

    void onMessage(PriestName q, Message message) throws RemoteException;

}