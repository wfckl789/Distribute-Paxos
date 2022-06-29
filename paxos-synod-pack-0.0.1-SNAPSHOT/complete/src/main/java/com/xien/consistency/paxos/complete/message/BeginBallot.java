package com.xien.consistency.paxos.complete.message;

import com.xien.consistency.paxos.complete.model.BallotNumber;
import com.xien.consistency.paxos.complete.model.Decree;
import lombok.Data;

/**
 * 协议第 3 步, 发起人广播的消息
 *
 * @author xien
 * @version 2022年05月18日 19:20 xien
 */
@Data
public class BeginBallot implements Message {
    private static final long serialVersionUID = -4259484026351061126L;

    private final BallotNumber b;
    private final Decree d;

    @Override
    public String toString() {
        return "BeginBallot{b: " + b + ", d: " + d + "}";
    }
}