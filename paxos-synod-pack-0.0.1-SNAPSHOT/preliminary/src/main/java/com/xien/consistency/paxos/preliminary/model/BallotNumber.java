package com.xien.consistency.paxos.preliminary.model;

import com.google.common.collect.Ordering;
import lombok.Data;

import java.io.Serializable;

/**
 * 表决编号
 *
 * @author xien
 * @version 2022年05月18日 19:18 xien
 */
@Data
public class BallotNumber implements Serializable, Comparable<BallotNumber> {
    private static final long serialVersionUID = 8790271158440136749L;

    private final int number;
    private final PriestName priestName;

    @Override
    public int compareTo(BallotNumber o) {
        return Ordering.natural().onResultOf(BallotNumber::getNumber)
            .compound(
                Ordering.natural().nullsFirst().onResultOf(BallotNumber::getPriestName)
            ).compare(this, o);
    }

    @Override
    public String toString() {
        return "(" + number + "," + priestName.getName() + ")";
    }
}