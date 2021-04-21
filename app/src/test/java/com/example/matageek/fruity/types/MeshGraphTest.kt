package com.example.matageek.fruity.types

import junit.framework.TestCase

import org.junit.Assert.*
import org.junit.Test

class MeshGraphTest : TestCase() {
    /**
     * 0 -- 1 -- 3 -- 6
     *   |    └- 4
     *   └- 2 -- 5
     */
    @Test
    fun test_mesh_graph() {
        val root = MeshGraph(0)
        val nodeList: MutableList<Pair<Short, Short>> = mutableListOf()
        nodeList.add(Pair(0, 1))
        nodeList.add(Pair(0, 2))
        nodeList.add(Pair(1, 0))
        nodeList.add(Pair(1, 3))
        nodeList.add(Pair(1, 3))
        nodeList.add(Pair(1, 4))
        nodeList.add(Pair(2, 0))
        nodeList.add(Pair(2, 5))
        nodeList.add(Pair(2, 5))
        nodeList.add(Pair(3, 1))
        nodeList.add(Pair(3, 6))
        nodeList.add(Pair(5, 2))
        nodeList.add(Pair(5, 2))
        for (pair in nodeList) {
            root.addNode(pair.first, pair.second)
        }
        assert(root.meshSize == 7)
    }
}