package com.xien.consistency.paxos.complete.repository;

import com.xien.consistency.paxos.complete.model.BallotNumber;
import com.xien.consistency.paxos.complete.model.Vote;

/**
 * @author xien
 * @version 2022年05月19日 10:13 xien
 */
public interface ILedgerBack {

    boolean tryUpdateNextBallotNumber(int greaterBallotNumber);

    int nextBallotNumber();

    int maxBallotNumber();

    BallotNumber promised(BallotNumber ballotNumber);

    Vote lastVote();

    BallotNumber makePromise(BallotNumber ballotNumber);

    void recordVote(Vote vote);
}