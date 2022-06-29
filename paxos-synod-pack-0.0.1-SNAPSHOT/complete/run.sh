#!/bin/sh

mvn clean compile exec:java -Dexec.mainClass=com.xien.consistency.paxos.complete.CompleteProtocol -Dexec.args="$1"