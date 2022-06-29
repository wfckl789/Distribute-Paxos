package com.xien.consistency.paxos.complete.repository;

import com.xien.consistency.paxos.complete.model.BallotNumber;
import com.xien.consistency.paxos.complete.model.PriestName;
import com.xien.consistency.paxos.complete.model.Vote;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
    public synchronized boolean tryUpdateNextBallotNumber(int greaterBallotNumber) {
        Path ballotNumberFile = baseDir.resolve("lastTried");

        if (!Files.exists(ballotNumberFile)) {
            try (RandomAccessFile file = new RandomAccessFile(ballotNumberFile.toFile(), "rwd")) {
                file.writeInt(greaterBallotNumber + 1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else {
            try (RandomAccessFile file = new RandomAccessFile(ballotNumberFile.toFile(), "rwd")) {
                int ballotNumber = file.readInt();
                if (ballotNumber > greaterBallotNumber) {
                    return false;
                }

                file.seek(0);
                file.writeInt(greaterBallotNumber + 1);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public synchronized int nextBallotNumber() {
        Path ballotNumberFile = baseDir.resolve("lastTried");

        if (!Files.exists(ballotNumberFile)) {
            try (RandomAccessFile file = new RandomAccessFile(ballotNumberFile.toFile(), "rwd")) {
                file.writeInt(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        } else {
            try (RandomAccessFile file = new RandomAccessFile(ballotNumberFile.toFile(), "rwd")) {
                int ballotNumber = file.readInt();
                file.seek(0);
                file.writeInt(ballotNumber + 1);
                return ballotNumber;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public synchronized int maxBallotNumber() {
        Path ballotNumberFile = baseDir.resolve("lastTried");

        if (!Files.exists(ballotNumberFile)) {
            return 0;
        }

        try (RandomAccessFile file = new RandomAccessFile(ballotNumberFile.toFile(), "rwd")) {
            int ballotNumber = file.readInt();
            file.seek(0);
            return ballotNumber;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized BallotNumber promised(BallotNumber ballotNumber) {
        Path promisesFile = baseDir.resolve("nextBal");
        if (!Files.exists(promisesFile)) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
            Files.newInputStream(promisesFile, StandardOpenOption.READ))) {
            BallotNumber maxBallotNumber = (BallotNumber)ois.readObject();
            if (maxBallotNumber.equals(ballotNumber)) {
                return null;
            }
            return maxBallotNumber;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized BallotNumber makePromise(BallotNumber nextBallotNumber) {
        Path promisesFile = baseDir.resolve("nextBal");

        if (Files.exists(promisesFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(promisesFile, StandardOpenOption.READ, StandardOpenOption.CREATE_NEW))) {
                BallotNumber maxBallotNumber = (BallotNumber)ois.readObject();
                if (maxBallotNumber.compareTo(nextBallotNumber) > 0) {
                    return maxBallotNumber;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(
            Files.newOutputStream(promisesFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        )) {
            oos.writeObject(nextBallotNumber);
            oos.flush();
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void recordVote(Vote vote) {
        Path votesFile = baseDir.resolve("prevVote");
        try (ObjectOutputStream oos = new ObjectOutputStream(
            Files.newOutputStream(votesFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        )) {
            oos.writeObject(vote);
            oos.flush();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public synchronized Vote lastVote() {
        Path votesFile = baseDir.resolve("prevVote");
        if (!Files.exists(votesFile)) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
            Files.newInputStream(votesFile, StandardOpenOption.READ)
        )) {
            return (Vote)ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}