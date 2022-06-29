package com.xien.consistency.paxos.complete.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xien
 * @version 2022年05月24日 16:52 xien
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Election implements Message {
    private static final long serialVersionUID = 9111945765329269295L;

    private Integer lastTriedBallotNumber;
}