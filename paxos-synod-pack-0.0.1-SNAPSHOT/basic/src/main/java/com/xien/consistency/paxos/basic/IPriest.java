package com.xien.consistency.paxos.basic;

import com.xien.consistency.paxos.basic.message.Message;
import com.xien.consistency.paxos.basic.model.PriestName;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author xien
 * @version 2022年05月18日 21:02 xien
 */
public interface IPriest extends Remote {

    void onMessage(PriestName q, Message message) throws RemoteException;

}