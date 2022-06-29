package com.xien.consistency.paxos.complete.message;

import com.xien.consistency.paxos.complete.model.BallotNumber;
import com.xien.consistency.paxos.complete.model.Vote;
import lombok.Data;


/**
 * 协议第 2 步，参与人回复的消息
 *
 * @author xien
 * @version 2022年05月18日 19:18 xien
 */
@Data
public class LastVote implements Message {
    private static final long serialVersionUID = -960142349104281847L;

    private final BallotNumber b;
    private final Vote v;

    @Override
    public String toString() {
        return "LastVote{b: " + b + ", v:" + v + "}";
    }
}