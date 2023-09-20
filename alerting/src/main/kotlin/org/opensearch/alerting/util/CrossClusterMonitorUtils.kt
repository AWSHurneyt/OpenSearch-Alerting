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
                if (index.contains(":")) {
                    val parsedNames = index.split(":")
                    val clusterName = parsedNames[0]
                    val indexName = parsedNames[1]
                    output.getOrPut(clusterName) { mutableListOf() }.add(indexName)
                } else {
                    output.getOrPut(clusterService.clusterName.value()) { mutableListOf() }.add(index)
                }
            }
            return output
        }

        fun getClientForCluster(clusterName: String, client: Client, clusterService: ClusterService): Client {
            return if (clusterName == clusterService.clusterName.value()) client
            else client.getRemoteClusterClient(clusterName)
        }

        @JvmStatic
        fun getClientForIndex(index: String, client: Client, clusterService: ClusterService): Client {
            return if (index.contains(":")) {
                val clusterAlias = parseClusterAlias(index)
                if (clusterAlias == clusterService.clusterName.value()) {
                    log.info("hurneyt getClient LOCAL 1")
                    client
                } else {
                    log.info("hurneyt getClient REMOTE")
                    client.getRemoteClusterClient(clusterAlias)
                }
            } else {
                log.info("hurneyt getClient LOCAL 2")
                client
            }
        }

        @JvmStatic
        fun parseClusterAlias(index: String): String {
            return if (index.contains(":")) index.split(":").getOrElse(0) { "" }
            else ""
        }

        @JvmStatic
        fun parseIndexName(index: String): String {
            return if (index.contains(":")) index.split(":").getOrElse(1) { index }
            else index
        }

        @JvmStatic
        fun formatClusterAndIndexNames(clusterName: String, indexName: String): String {
            return if (clusterName.isNotEmpty()) "$clusterName:$indexName"
            else indexName
        }
    }
}
