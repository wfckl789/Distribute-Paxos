package com.xien.consistency.paxos.preliminary.repository;

import com.xien.consistency.paxos.preliminary.model.BallotNumber;
import com.xien.consistency.paxos.preliminary.model.Vote;

/**
 * @author xien
 * @version 2022年05月19日 10:13 xien
 */
public interface ILedgerBack {
    boolean isBallotNumberUnique(int number);

    void recordBallotNumber(int number);

    boolean hasPromised(BallotNumber ballotNumber);

    Vote lastVoteBefore(BallotNumber ballotNumber);

    void makePromise(BallotNumber from, BallotNumber to);

    void recordVote(Vote vote);
}