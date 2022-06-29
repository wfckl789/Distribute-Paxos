#!/bin/sh

mvn clean compile exec:java -Dexec.mainClass=com.xien.consistency.paxos.basic.BasicProtocol -Dexec.args="$1"