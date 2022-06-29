package com.xien.consistency.paxos.preliminary.message;

import com.xien.consistency.paxos.preliminary.model.BallotNumber;
import lombok.Data;

/**
 * 协议第 4 步, 参与人回应的消息
 *
 * @author xien
 * @version 2022年05月18日 19:20 xien
 */
@Data
public class Voted implements Message {
    private static final long serialVersionUID = -4825308457420771249L;

    private final BallotNumber b;

    @Override
    public String toString() {
        return "Voted{b: " + b + "}";
    }
}