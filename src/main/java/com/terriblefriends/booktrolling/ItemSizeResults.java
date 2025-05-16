package com.terriblefriends.booktrolling;

public record ItemSizeResults(boolean error, long diskSize, long diskSizeCompressed, int packetSize, int packetSizeCompressed) {

}