package com.xien.consistency.paxos.preliminary;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.xien.consistency.paxos.preliminary.model.Decree;
import com.xien.consistency.paxos.preliminary.model.PriestName;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Paxos 初步协议
 *
 * @author xien
 * @version 2022年05月18日 19:12 xien
 */
public class PreliminaryProtocol {

    private static Properties loadProperties(String priestNumber) throws IOException {
        Properties properties = new Properties();
        try (InputStream is = PreliminaryProtocol.class.getResourceAsStream(
            "/priest-" + priestNumber + ".properties")) {
            properties.load(is);
        }
        return properties;
    }

    public static void main(String[] args) throws Exception {
        Properties properties = loadProperties(args[0]);

        PriestName myName = new PriestName(properties.getProperty("my.name"));
        Integer myPort = Integer.parseInt(properties.getProperty("my.port"));

        Map<PriestName, Integer> priestPorts = Maps.newHashMap();
        int index = 0;
        while (true) {
            String name = properties.getProperty("priest." + index + ".name");
            if (name == null) {
                break;
            }
            PriestName priestName = new PriestName(name);
            int priestPort = Integer.parseInt(properties.getProperty("priest." + index + ".port"));
            priestPorts.put(priestName, priestPort);

            index++;
        }

        List<Decree> decrees = new ArrayList<>();
        index = 0;
        while (true) {
            String decreeContent = properties.getProperty("decree." + index + ".content");
            if (decreeContent == null) {
                break;
            }
            decrees.add(new Decree(decreeContent));
            index++;
        }

        Priest priest = new Priest(
            myName,
            myPort,
            decrees,
            priestPorts.size(),
            new Messenger(ImmutableMap.copyOf(priestPorts))
        );

        AtomicBoolean exit = new AtomicBoolean(false);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                exit.set(true);
            }
        });

        Random random = new Random();
        while (!exit.get()) {
            priest.enter();
            try {
                do {
                    TimeUnit.SECONDS.sleep(random.nextInt(30) + 60);
                } while (random.nextBoolean() && !exit.get());
            } finally {
                priest.leave();
            }
            do {
                TimeUnit.SECONDS.sleep(random.nextInt(5) + 5);
            } while (random.nextBoolean() && !exit.get());
        }
    }
}