package com.xien.consistency.paxos.basic.message;

import com.xien.consistency.paxos.basic.model.BallotNumber;
import lombok.Data;


/**
 * 协议第 1 步，发起人广播的消息
 *
 * @author xien
 * @version 2022年05月18日 19:17 xien
 */
@Data
public class NextBallot implements Message {
    private static final long serialVersionUID = 308161034646336675L;

    private final BallotNumber b;

    @Override
    public String toString() {
        return "NextBallot{b: " + b + "}";
    }
}