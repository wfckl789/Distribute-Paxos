package com.xien.consistency.paxos.complete;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xien.consistency.paxos.complete.message.*;
import com.xien.consistency.paxos.complete.model.BallotNumber;
import com.xien.consistency.paxos.complete.model.Decree;
import com.xien.consistency.paxos.complete.model.PriestName;
import com.xien.consistency.paxos.complete.model.Vote;
import com.xien.consistency.paxos.complete.repository.ILedgerBack;
import com.xien.consistency.paxos.complete.repository.LedgerBack;
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
import java.util.concurrent.ScheduledFuture;
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

    private volatile ScheduledFuture<?> electionBoardCastTask;

    private volatile ScheduledFuture<?> beginBallotTask;

    private volatile boolean success = false;

    private final PriestName priestName;

    private final List<Decree> decrees;
    private final int totalPriestCount;

    private final IMessenger messenger;

    private final ILedgerBack ledgerBack;

    private final String rmiName;

    private Quorum currentBallot;

    private final Set<PriestName> electingPriestNames = Sets.newConcurrentHashSet();

    @Data
    private static class Quorum {

        private final BallotNumber ballotNumber;

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

        electionBoardCastTask = SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                messenger.broadcast(priestName, new Election(ledgerBack.maxBallotNumber()));
            }
        }, 0, 3, TimeUnit.SECONDS);

        beginBallotTask = SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (amIPresident()) {
                    log.info("我『{}』当选了, 准备发起表决", priestName.getName());
                    tryNextBallot();
                }
            }
        }, 10, 10, TimeUnit.SECONDS);

        log.info("我『{}』进入议会大厅了", priestName.getName());
    }

    public synchronized void leave() throws Exception {
        Naming.unbind(rmiName);
        inChamber = false;
        success = false;
        currentBallot = null;
        electionBoardCastTask.cancel(false);
        beginBallotTask.cancel(false);
        log.info("我『{}』离开议会大厅了", priestName.getName());
    }

    private boolean amIPresident() {
        if (electingPriestNames.isEmpty()) {
            return true;
        }
        PriestName max = Collections.max(electingPriestNames);
        electingPriestNames.clear();
        return Objects.equals(max, priestName);
    }

    @Override
    public synchronized void onMessage(PriestName q, Message message) throws RemoteException {
        if (!inChamber) {
            return;
        }

        SCHEDULED_EXECUTOR_SERVICE.submit(() -> {
            try {
                if (message instanceof Election) {
                    onElection(q, (Election)message);
                } else if (message instanceof MaxBallot) {
                    onMaxBallot(q, (MaxBallot)message);
                } else if (message instanceof NextBallot) {
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

    private void onElection(PriestName q, Election election) {
        electingPriestNames.add(q);
        if (ledgerBack.tryUpdateNextBallotNumber(election.getLastTriedBallotNumber())) {
            log.info("我『{}』更新我的表决号到 {}", priestName.getName(), election.getLastTriedBallotNumber());
            currentBallot = null;
        }
    }

    private void onMaxBallot(PriestName q, MaxBallot maxBallot) {
        if (ledgerBack.tryUpdateNextBallotNumber(maxBallot.getBallotNumber())) {
            log.info("我『{}』更新我的表决号到 {}", priestName.getName(), maxBallot.getBallotNumber());
            currentBallot = null;
        }
    }

    private void onNextBallot(PriestName q, NextBallot nextBallot) {
        BallotNumber greaterBallotNumber = ledgerBack.makePromise(nextBallot.getB());
        if (greaterBallotNumber == null) {
            log.info("我『{}』收到了来自『{}』的 NextBallot 消息 {}", priestName.getName(), q.getName(), nextBallot);
            Vote lastVote = ledgerBack.lastVote();

            LastVote lastVoteMessage = new LastVote(nextBallot.getB(), lastVote);
            log.info("我『{}』向『{}』回应 LastVote 消息 {}", priestName.getName(), q.getName(), lastVoteMessage);
            messenger.sendTo(priestName, q, lastVoteMessage);
        } else {
            messenger.sendTo(priestName, q, new MaxBallot(greaterBallotNumber.getNumber()));
        }
    }

    private void onLastVote(PriestName q, LastVote lastVote) {
        if (currentBallot == null || !currentBallot.getBallotNumber().equals(lastVote.getB())) {
            return;
        }

        if (currentBallot.ballotNumber.compareTo(lastVote.getB()) != 0) {
            return;
        }

        if (currentBallot.priests.size() <= Math.floor((double)totalPriestCount / 2)) {
            log.info("我『{}』收到了来自『{}』的 LastVote 消息 {}", priestName.getName(), q.getName(), lastVote);
            currentBallot.priests.add(q);
            currentBallot.lastVotes.add(lastVote);

            if (currentBallot.priests.size() > Math.floor((double)totalPriestCount / 2)) {
                log.info("我『{}』收到了超过半数的议员参与第 {} 轮表决, 议员LastVote列表: {}", priestName.getName(), lastVote.getB(),
                    currentBallot.lastVotes);

                BallotNumber highestBallotNumber = null;
                Vote highestLastVote = null;

                for (LastVote l : currentBallot.lastVotes) {
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
                currentBallot.decreeRef.set(decree);

                BeginBallot beginBallot = new BeginBallot(lastVote.getB(), decree);
                log.info("我『{}』开始发送的 BeginBallot 消息 {}", priestName.getName(), beginBallot);
                for (PriestName to : currentBallot.priests) {
                    messenger.sendTo(priestName, to, beginBallot);
                }
            }
        }
    }

    private void onBeginBallot(PriestName q, BeginBallot beginBallot) {
        BallotNumber greaterBallotNumber = ledgerBack.promised(beginBallot.getB());
        if (greaterBallotNumber == null) {
            log.info("我『{}』收到了来自『{}』的 BeginBallot 消息 {}", priestName.getName(), q.getName(), beginBallot);
            Voted vote = new Voted(beginBallot.getB());

            ledgerBack.recordVote(new Vote(beginBallot.getB(), beginBallot.getD()));
            log.info("我『{}』向『{}』回应了 Voted 消息 {}", priestName.getName(), q.getName(), vote);
            messenger.sendTo(priestName, q, vote);
        } else {
            messenger.sendTo(priestName, q, new MaxBallot(greaterBallotNumber.getNumber()));
        }
    }

    private void onVoted(PriestName q, Voted voted) {
        if (currentBallot == null || !currentBallot.getBallotNumber().equals(voted.getB())) {
            return;
        }

        currentBallot.voteds.add(q);
        if (currentBallot.voteds.equals(currentBallot.priests)) {
            log.info("我『{}』在第 {} 轮表决成功. 通过的法令是: {}", priestName.getName(), voted.getB(), currentBallot.decreeRef.get());
            messenger.broadcast(priestName, new Success(currentBallot.decreeRef.get()));
        }
    }

    private void onSuccess(PriestName q, Success success) {
        log.info("我『{}』收到了来自『{}』的通过的法令的消息. 通过的法令是是: {}", priestName.getName(), q.getName(), success.getD());
        this.success = true;
        if (beginBallotTask != null && !beginBallotTask.isCancelled()) {
            beginBallotTask.cancel(true);
        }
        if (electionBoardCastTask != null && !electionBoardCastTask.isCancelled()) {
            electionBoardCastTask.cancel(true);
        }
    }

    private void tryNextBallot() {
        if (!inChamber || success) {
            return;
        }

        try {
            int nextBallotNumber = ledgerBack.nextBallotNumber();
            log.info("我『{}』准备开始新一轮 {} 选举", priestName.getName(), nextBallotNumber);
            NextBallot nextBallot = new NextBallot(new BallotNumber(nextBallotNumber, priestName));
            currentBallot = new Quorum(nextBallot.getB(), Sets.newHashSet(), Lists.newArrayList(), Sets.newHashSet(),
                new AtomicReference<>());
            messenger.broadcast(priestName, nextBallot);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}