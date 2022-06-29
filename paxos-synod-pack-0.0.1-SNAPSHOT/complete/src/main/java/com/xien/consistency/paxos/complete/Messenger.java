package com.xien.consistency.paxos.complete;

import com.google.common.collect.ImmutableMap;
import com.xien.consistency.paxos.complete.message.Message;
import com.xien.consistency.paxos.complete.model.PriestName;
import lombok.extern.slf4j.Slf4j;

import java.rmi.Naming;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xien
 * @version 2022年05月18日 21:08 xien
 */
@Slf4j
public class Messenger implements IMessenger {

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
        10, 10, Long.MAX_VALUE, TimeUnit.MINUTES, new SynchronousQueue<>(),
        r -> {
            Thread t = new Thread(r, "messager");
            t.setDaemon(true);
            return t;
        }, new ThreadPoolExecutor.DiscardPolicy()
    );

    private final Map<PriestName, Integer> priestPorts;

    public Messenger(Map<PriestName, Integer> priestPorts) {
        this.priestPorts = ImmutableMap.copyOf(priestPorts);
    }

    @Override
    public void broadcast(PriestName from, Message message) {
        priestPorts.keySet()
            .forEach((to) -> {
                sendTo(from, to, message);
            });
    }

    @Override
    public void sendTo(PriestName from, PriestName to, Message message) {
        final Integer port = priestPorts.get(to);
        if (port == null) {
            return;
        }

        THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                IPriest priest = (IPriest)Naming.lookup("rmi://localhost:" + port + "/priest");
                priest.onMessage(from, message);
            } catch (Exception e) {
            }

        });
    }
}