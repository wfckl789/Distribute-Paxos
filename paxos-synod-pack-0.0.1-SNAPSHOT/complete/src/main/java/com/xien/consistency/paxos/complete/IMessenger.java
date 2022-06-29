package com.xien.consistency.paxos.complete;

import com.xien.consistency.paxos.complete.message.Message;
import com.xien.consistency.paxos.complete.model.PriestName;

/**
 * @author xien
 * @version 2022年05月18日 19:51 xien
 */
public interface IMessenger {

    void broadcast(PriestName p, Message message);

    void sendTo(PriestName p, PriestName q, Message message);
}