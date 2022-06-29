package com.xien.consistency.paxos.preliminary.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 投票
 *
 * @author xien
 * @version 2022年05月18日 19:16 xien
 */
@Data
public class Vote implements Serializable {
    private static final long serialVersionUID = -1724228543641206628L;

    private final BallotNumber b;
    private final Decree d;

    @Override
    public String toString() {
        return "Vote{b: " + b + ", d: " + d + "}";
    }
}