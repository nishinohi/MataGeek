package com.example.matageek.fruity.types

class PrimitiveTypes {
    companion object {
        const val NODE_ID_BROADCAST: Short = 0
    }
}

enum class DeviceType(val type: Byte) {
    INVALID(0),
    STATIC(1), // A normal node that remains static at one position
    ROAMING(2), // A node that is moving constantly or often (not implemented)
    SINK(3), // A static node that wants to acquire data, e.g. a MeshGateway
    ASSET(4), // A roaming node that is sporadically or never connected but broadcasts data
    LEAF(5),  // A node that will never act as a slave but will only connect as a master (useful for roaming nodes, but no relaying possible)
}