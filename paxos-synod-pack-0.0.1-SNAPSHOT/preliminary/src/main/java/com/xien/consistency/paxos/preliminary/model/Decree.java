package com.xien.consistency.paxos.preliminary.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 法令
 *
 * @author xien
 * @version 2022年05月18日 19:15 xien
 */
@Data
public class Decree implements Serializable {
    private static final long serialVersionUID = -8100300467263971702L;

    private final String content;

    @Override
    public String toString() {
        return "(" + content + ")";
    }
}