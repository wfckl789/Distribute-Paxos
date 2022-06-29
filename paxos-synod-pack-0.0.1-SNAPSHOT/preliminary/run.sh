#!/bin/sh

mvn clean compile exec:java -Dexec.mainClass=com.xien.consistency.paxos.preliminary.PreliminaryProtocol -Dexec.args="$1"