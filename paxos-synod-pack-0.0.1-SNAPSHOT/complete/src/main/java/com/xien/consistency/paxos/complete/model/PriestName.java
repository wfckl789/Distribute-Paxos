package com.xien.consistency.paxos.complete.model;

import com.google.common.collect.Ordering;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 牧师
 *
 * @author xien
 * @version 2022年05月18日 19:14 xien
 */
@Data
public class PriestName implements Serializable, Comparable<PriestName> {
    private static final long serialVersionUID = -4447788922677685799L;

    private final String name;

    @Override
    public int compareTo(PriestName o) {
        return Ordering.natural()
            .nullsFirst()
            .compare(name, o.name)
            ;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof PriestName)) {
            return false;
        }

        PriestName q = (PriestName)obj;
        return Objects.equals(name, q.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}