package com.xien.consistency.paxos.basic.repository;

import com.xien.consistency.paxos.basic.model.BallotNumber;
import com.xien.consistency.paxos.basic.model.Vote;

/**
 * @author xien
 * @version 2022年05月19日 10:13 xien
 */
public interface ILedgerBack {

    int nextBallotNumber();

    boolean promised(BallotNumber ballotNumber);

    Vote lastVote();

    boolean makePromise(BallotNumber ballotNumber);

    void recordVote(Vote vote);
}