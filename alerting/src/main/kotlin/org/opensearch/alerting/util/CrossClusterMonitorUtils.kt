/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.util

import org.apache.logging.log4j.LogManager
import org.opensearch.client.Client
import org.opensearch.cluster.service.ClusterService

private val log = LogManager.getLogger(CrossClusterMonitorUtils::class.java)

class CrossClusterMonitorUtils {
    companion object {

        @JvmStatic
        fun separateClusterIndexes(indexes: List<String>, clusterService: ClusterService): HashMap<String, MutableList<String>> {
            val output = hashMapOf<String, MutableList<String>>()
            indexes.forEach { index ->
                var clusterName = parseClusterName(index)
                val indexName = parseIndexName(index)
                if (clusterName.isEmpty()) clusterName = clusterService.clusterName.value()
                output.getOrPut(clusterName) { mutableListOf() }.add(indexName)
            }
            return output
        }

        @JvmStatic
        fun parseIndexesForRemoteSearch(indexes: List<String>, clusterService: ClusterService): List<String> {
            return indexes.map {
                var index = it
                val clusterName = parseClusterName(it)
                if (clusterName.isNotEmpty() && clusterName == clusterService.clusterName.value()) {
                    index = parseIndexName(it)
                }
                index
            }
        }

        @JvmStatic
        fun getClientForCluster(clusterName: String, client: Client, clusterService: ClusterService): Client {
            log.info("hurneyt getClientForCluster::clusterName = $clusterName")
            log.info("hurneyt getClientForCluster::clusterService.clusterName = ${clusterService.clusterName.value()}")
            return if (clusterName == clusterService.clusterName.value()) {
                log.info("hurneyt getClientForCluster LOCAL")
                client
            } else {
                log.info("hurneyt getClientForCluster REMOTE")
                client.getRemoteClusterClient(clusterName)
            }
        }

        @JvmStatic
        fun getClientForIndex(index: String, client: Client, clusterService: ClusterService): Client {
            val clusterName = parseClusterName(index)
            log.info("hurneyt getClientForIndex::clusterName = $clusterName")
            return if (clusterName.isNotEmpty() && clusterName != clusterService.clusterName.value()) {
                log.info("hurneyt getClientForIndex REMOTE")
                client.getRemoteClusterClient(clusterName)
            } else {
                log.info("hurneyt getClientForIndex LOCAL")
                client
            }
        }

        @JvmStatic
        fun parseClusterName(index: String): String {
            return if (index.contains(":")) index.split(":").getOrElse(0) { "" }
            else ""
        }

        @JvmStatic
        fun parseIndexName(index: String): String {
            return if (index.contains(":")) index.split(":").getOrElse(1) { index }
            else index
        }

        @JvmStatic
        fun formatClusterAndIndexName(clusterName: String, indexName: String): String {
            return if (clusterName.isNotEmpty()) "$clusterName:$indexName"
            else indexName
        }
    }
}
