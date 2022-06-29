package com.xien.consistency.paxos.complete.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xien
 * @version 2022年05月24日 17:19 xien
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaxBallot implements Message {
    private static final long serialVersionUID = 5992342013443701669L;

    private Integer ballotNumber;
}