package com.xien.consistency.paxos.preliminary.repository;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.xien.consistency.paxos.preliminary.model.BallotNumber;
import com.xien.consistency.paxos.preliminary.model.PriestName;
import com.xien.consistency.paxos.preliminary.model.Vote;
import lombok.Data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * @author xien
 * @version 2022年05月19日 10:15 xien
 */
public class LedgerBack implements ILedgerBack {
    private final Path baseDir;

    public LedgerBack(PriestName name) throws IOException {
        baseDir = Paths.get(".", ".ledger-" + name.getName());
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }
    }

    @Override
    public synchronized boolean isBallotNumberUnique(int number) {
        Path ballotNumberFile = baseDir.resolve("ballot-numbers.bin");
        if (!Files.exists(ballotNumberFile)) {
            return true;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
            Files.newInputStream(ballotNumberFile, StandardOpenOption.READ)
        )) {
            BitSet bitSet = (BitSet)ois.readObject();
            return !bitSet.get(number);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void recordBallotNumber(int number) {
        Path ballotNumberFile = baseDir.resolve("ballot-numbers.bin");
        BitSet bitSet;
        if (!Files.exists(ballotNumberFile)) {
            bitSet = new BitSet(Integer.MAX_VALUE);
        } else {
            try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(ballotNumberFile, StandardOpenOption.READ)
            )) {
                bitSet = (BitSet)ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        bitSet.set(number);

        try (ObjectOutputStream oos = new ObjectOutputStream(
            Files.newOutputStream(ballotNumberFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        )) {
            oos.writeObject(bitSet);
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class Promise implements Serializable {
        public static final BallotNumber NEGATIVE_INFINITY_BALLOT_NUMBER = new BallotNumber(Integer.MIN_VALUE, null);

        private static final long serialVersionUID = -8149246501790378309L;

        private final BallotNumber from;
        private final BallotNumber to;
    }

    @Override
    public synchronized boolean hasPromised(BallotNumber ballotNumber) {
        RangeSet<BallotNumber> rangeSet = TreeRangeSet.create();

        Path promisesFile = baseDir.resolve("promises.bin");
        if (Files.exists(promisesFile)) {
            List<Promise> promises;
            try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(promisesFile, StandardOpenOption.READ))) {
                promises = (List<Promise>)ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            promises.forEach((pair) -> rangeSet.add(Range.range(
                pair.getFrom(), BoundType.CLOSED,
                pair.getTo(), BoundType.OPEN
            )));
        }

        return rangeSet.contains(ballotNumber);
    }

    @Override
    public void makePromise(BallotNumber from, BallotNumber to) {
        Path promisesFile = baseDir.resolve("promises.bin");

        List<Promise> promises;
        if (Files.exists(promisesFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(promisesFile, StandardOpenOption.READ))) {
                promises = (List<Promise>)ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            promises = new ArrayList<>(1);
        }

        promises.add(new Promise(from != null ? from : Promise.NEGATIVE_INFINITY_BALLOT_NUMBER, to));

        try (ObjectOutputStream oos = new ObjectOutputStream(
            Files.newOutputStream(promisesFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        )) {
            oos.writeObject(promises);
            oos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void recordVote(Vote vote) {
        Path votesFile = baseDir.resolve("votes.bin");
        Vote[] votes;
        if (Files.exists(votesFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(votesFile, StandardOpenOption.READ)
            )) {
                votes = (Vote[])ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
        else {
            votes = new Vote[0];
        }

        Vote[] newVotes = Arrays.copyOf(votes, votes.length + 1);
        newVotes[votes.length] = vote;

        try (ObjectOutputStream oos = new ObjectOutputStream(
            Files.newOutputStream(votesFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        )) {
            oos.writeObject(newVotes);
            oos.flush();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public synchronized Vote lastVoteBefore(BallotNumber ballotNumber) {
        Path votesFile = baseDir.resolve("votes.bin");
        if (!Files.exists(votesFile)) {
            return null;
        }

        Vote[] votes;
        try (ObjectInputStream ois = new ObjectInputStream(
            Files.newInputStream(votesFile, StandardOpenOption.READ)
        )) {
            votes = (Vote[])ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException();
        }

        Arrays.sort(votes, Comparator.comparing(Vote::getB));
        int index = Arrays.binarySearch(votes, new Vote(ballotNumber, null), Comparator.comparing(Vote::getB));
        if (index < 0) {
            index = -index - 1;
        }

        if (index == 0) {
            return null;
        } else {
            return votes[index - 1];
        }
    }
}