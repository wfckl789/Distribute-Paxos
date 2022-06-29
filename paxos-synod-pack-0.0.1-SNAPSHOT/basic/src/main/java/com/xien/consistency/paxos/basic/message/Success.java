package com.xien.consistency.paxos.basic.message;

import com.xien.consistency.paxos.basic.model.Decree;
import lombok.Data;

/**
 * 协议第 5 步, 发起人广播的消息
 *
 * @author xien
 * @version 2022年05月18日 19:20 xien
 */
@Data
public class Success implements Message {
    private static final long serialVersionUID = 7924215552488346993L;

    private final Decree d;

    @Override
    public String toString() {
        return "Success{d: " + d + "}";
    }
}