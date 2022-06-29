package com.xien.consistency.paxos.complete.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 表决
 *
 * @author xien
 * @version 2022年05月18日 19:15 xien
 */
@Data
public class Ballot implements Serializable {
    private static final long serialVersionUID = -806259599014669776L;

    private final BallotNumber b;
}