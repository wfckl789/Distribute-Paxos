package com.xien.consistency.paxos.preliminary;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xien.consistency.paxos.preliminary.message.*;
import com.xien.consistency.paxos.preliminary.model.BallotNumber;
import com.xien.consistency.paxos.preliminary.model.Decree;
import com.xien.consistency.paxos.preliminary.model.PriestName;
import com.xien.consistency.paxos.preliminary.model.Vote;
import com.xien.consistency.paxos.preliminary.repository.ILedgerBack;
import com.xien.consistency.paxos.preliminary.repository.LedgerBack;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 牧师
 *
 * @author xien
 * @version 2022年05月18日 19:14 xien
 */
@Slf4j(topic = "牧师")
public class Priest extends UnicastRemoteObject implements IPriest {

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE
        = Executors.newSingleThreadScheduledExecutor(
        (r) -> {
            return new Thread(r, "priest");
        });

    private static final long serialVersionUID = 3494050188130196350L;

    private final Random random = new Random();

    private volatile boolean inChamber = false;

    private volatile boolean success = false;

    private final PriestName priestName;

    private final List<Decree> decrees;
    private final int totalPriestCount;

    private final IMessenger messenger;

    private final ILedgerBack ledgerBack;

    private final String rmiName;

    private final Map<BallotNumber, Quorum> onGoingBallots = new HashMap<>();

    @Data
    private static class Quorum {
        private final Set<PriestName> priests;
        private final List<LastVote> lastVotes;

        private final Set<PriestName> voteds;
        private final AtomicReference<Decree> decreeRef;
    }

    public Priest(PriestName priestName, int port, List<Decree> decrees, int totalPriestCount, IMessenger messenger)
        throws IOException {
        super();
        this.priestName = priestName;
        this.decrees = ImmutableList.copyOf(decrees);
        this.totalPriestCount = totalPriestCount;
        this.messenger = messenger;

        this.rmiName = String.format("rmi://localhost:%d/priest", port);
        LocateRegistry.createRegistry(port);

        this.ledgerBack = new LedgerBack(priestName);
    }

    public synchronized void enter() throws Exception {
        Naming.rebind(rmiName, this);
        inChamber = true;
        log.info("我『{}』进入议会大厅了", priestName.getName());

        SCHEDULED_EXECUTOR_SERVICE.schedule(this::tryNextBallot, random.nextInt(5) + 5, TimeUnit.SECONDS);
    }

    public synchronized void leave() throws Exception {
        Naming.unbind(rmiName);
        inChamber = false;
        success = false;
        log.info("我『{}』离开议会大厅了", priestName.getName());
    }

    @Override
    public synchronized void onMessage(PriestName q, Message message) throws RemoteException {
        if (!inChamber) {
            return;
        }

        SCHEDULED_EXECUTOR_SERVICE.submit(() -> {
            try {
                if (message instanceof NextBallot) {
                    onNextBallot(q, (NextBallot)message);
                } else if (message instanceof LastVote) {
                    onLastVote(q, (LastVote)message);
                } else if (message instanceof BeginBallot) {
                    onBeginBallot(q, (BeginBallot)message);
                } else if (message instanceof Voted) {
                    onVoted(q, (Voted)message);
                } else if (message instanceof Success) {
                    onSuccess(q, (Success)message);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    private void onNextBallot(PriestName q, NextBallot nextBallot) {
        if (!ledgerBack.hasPromised(nextBallot.getB())) {
            log.info("我『{}』收到了来自『{}』的 NextBallot 消息 {}", priestName.getName(), q.getName(), nextBallot);
            Vote lastVote = ledgerBack.lastVoteBefore(nextBallot.getB());
            ledgerBack.makePromise(
                Optional.ofNullable(lastVote).map(Vote::getB).orElse(null),
                nextBallot.getB());

            LastVote lastVoteMessage = new LastVote(nextBallot.getB(), lastVote);
            log.info("我『{}』向『{}』回应 LastVote 消息 {}", priestName.getName(), q.getName(), lastVoteMessage);
            messenger.sendTo(priestName, q, lastVoteMessage);
        }
    }

    private void onLastVote(PriestName q, LastVote lastVote) {
        Quorum quorum = onGoingBallots.get(lastVote.getB());
        if (quorum == null) {
            return;
        }

        if (quorum.priests.size() <= Math.floor((double)totalPriestCount / 2)) {
            log.info("我『{}』收到了来自『{}』的 LastVote 消息 {}", priestName.getName(), q.getName(), lastVote);
            quorum.priests.add(q);
            quorum.lastVotes.add(lastVote);

            if (quorum.priests.size() > Math.floor((double)totalPriestCount / 2)) {
                log.info("超过半数的议员参与了第 {} 轮表决, 议员LastVote列表: {}", lastVote.getB(), quorum.lastVotes);

                BallotNumber highestBallotNumber = null;
                Vote highestLastVote = null;

                for (LastVote l : quorum.lastVotes) {
                    if (highestBallotNumber == null || highestBallotNumber.compareTo(l.getB()) < 0) {
                        highestBallotNumber = l.getB();
                        highestLastVote = l.getV();
                    }
                }

                Decree decree;
                if (highestLastVote == null) {
                    decree = decrees.get(random.nextInt(decrees.size()));
                } else {
                    decree = highestLastVote.getD();
                }
                quorum.decreeRef.set(decree);

                BeginBallot beginBallot = new BeginBallot(lastVote.getB(), decree);
                log.info("我『{}』开始发送的 BeginBallot 消息 {}", priestName.getName(), beginBallot);
                for (PriestName to : quorum.priests) {
                    messenger.sendTo(priestName, to, beginBallot);
                }
            }
        }
    }

    private void onBeginBallot(PriestName q, BeginBallot beginBallot) {
        if (!ledgerBack.hasPromised(beginBallot.getB())) {
            log.info("我『{}』收到了来自『{}』的 BeginBallot 消息 {}", priestName.getName(), q.getName(), beginBallot);
            Voted vote = new Voted(beginBallot.getB());

            ledgerBack.recordVote(new Vote(beginBallot.getB(), beginBallot.getD()));
            log.info("我『{}』向『{}』回应了 Voted 消息 {}", priestName.getName(), q.getName(), vote);
            messenger.sendTo(priestName, q, vote);
        }
    }

    private void onVoted(PriestName q, Voted voted) {
        Quorum quorum = onGoingBallots.get(voted.getB());
        if (quorum == null) {
            return;
        }

        quorum.voteds.add(q);
        if (quorum.voteds.equals(quorum.priests)) {
            log.info("我『{}』在第 {} 轮表决成功. 通过的法令是: {}", priestName.getName(), voted.getB(), quorum.decreeRef.get());
            messenger.broadcast(priestName, new Success(quorum.decreeRef.get()));
        }
    }

    private void onSuccess(PriestName q, Success success) {
        log.info("我『{}』收到了来自『{}』的通过的法令的消息. 通过的法令是是: {}", priestName.getName(), q.getName(), success.getD());
        this.success = true;
    }

    private int i = 0;

    private void tryNextBallot() {
        if (!inChamber || success) {
            return;
        }

        try {
            while (inChamber) {
                int nextBallotNumber = i++;
                if (ledgerBack.isBallotNumberUnique(nextBallotNumber)) {
                    ledgerBack.recordBallotNumber(nextBallotNumber);

                    log.info("我『{}』准备开始新一轮 {} 选举", priestName.getName(), nextBallotNumber);
                    NextBallot nextBallot = new NextBallot(new BallotNumber(nextBallotNumber, priestName));
                    onGoingBallots.put(nextBallot.getB(),
                        new Quorum(Sets.newHashSet(), Lists.newArrayList(), Sets.newHashSet(),
                            new AtomicReference<>()));
                    messenger.broadcast(priestName, nextBallot);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (inChamber && !success) {
                SCHEDULED_EXECUTOR_SERVICE.schedule(this::tryNextBallot, random.nextInt(10) + 10, TimeUnit.SECONDS);
            }
        }
    }
}