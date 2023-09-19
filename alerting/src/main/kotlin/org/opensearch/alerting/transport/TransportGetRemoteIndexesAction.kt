/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionListener
import org.opensearch.action.admin.cluster.health.ClusterHealthRequest
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse
import org.opensearch.action.support.ActionFilters
import org.opensearch.action.support.HandledTransportAction
import org.opensearch.action.support.IndicesOptions
import org.opensearch.alerting.action.GetRemoteIndexesAction
import org.opensearch.alerting.action.GetRemoteIndexesRequest
import org.opensearch.alerting.action.GetRemoteIndexesResponse
import org.opensearch.alerting.action.GetRemoteIndexesResponse.ClusterInfo
import org.opensearch.alerting.action.GetRemoteIndexesResponse.ClusterInfo.IndexInfo
import org.opensearch.alerting.opensearchapi.suspendUntil
import org.opensearch.alerting.util.AlertingException
import org.opensearch.client.Client
import org.opensearch.cluster.service.ClusterService
import org.opensearch.common.inject.Inject
import org.opensearch.common.settings.Settings
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.tasks.Task
import org.opensearch.transport.TransportService
import java.time.Duration
import java.time.Instant
import kotlin.streams.toList

private val log = LogManager.getLogger(TransportGetRemoteIndexesAction::class.java)
private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

class TransportGetRemoteIndexesAction @Inject constructor(
    val transportService: TransportService,
    val client: Client,
    actionFilters: ActionFilters,
    val xContentRegistry: NamedXContentRegistry,
    val clusterService: ClusterService,
    settings: Settings,
) : HandledTransportAction<GetRemoteIndexesRequest, GetRemoteIndexesResponse>(
    GetRemoteIndexesAction.NAME,
    transportService,
    actionFilters,
    ::GetRemoteIndexesRequest
) {
    override fun doExecute(
        task: Task,
        request: GetRemoteIndexesRequest,
        listener: ActionListener<GetRemoteIndexesResponse>
    ) {
        var clusterAliases = transportService.remoteClusterService.remoteConnectionInfos.toList()

        log.info("hurneyt filter clusterAliases BEFORE = $clusterAliases")

        // If clusters are specified, filter out unspecified clusters
        if (request.clusterAliases.isNotEmpty())
            clusterAliases = clusterAliases.filter { request.clusterAliases.contains(it.clusterAlias) }.toList()

        log.info("hurneyt clusterAliases AFTER = $clusterAliases")
        client.threadPool().threadContext.stashContext().use {
            scope.launch {
                val clusterInfos = mutableListOf<ClusterInfo>()
                try {
                    clusterAliases.forEach {
                        if (it.isConnected) {
                            clusterInfos.add(getRemoteIndexes(it.clusterAlias))
                        }
                    }
                } catch (e: Exception) {
                    log.error("Failed to retrieve index stats for request $request", e)
                    listener.onFailure(AlertingException.wrap(e))
                }
                listener.onResponse(GetRemoteIndexesResponse(clusters = clusterInfos))
            }
        }
    }

    private suspend fun getRemoteIndexes(clusterAlias: String): ClusterInfo {
        log.info("hurneyt getRemoteIndexes::clusterAlias = $clusterAlias")
        val targetClient =
            if (clusterService.clusterName.value() == clusterAlias) {
                log.info("hurneyt getRemoteIndexes LOCAL CLIENT")
                client
            }
            else {
                log.info("hurneyt getRemoteIndexes REMOTE CLIENT")
                client.getRemoteClusterClient(clusterAlias)
            }

        val clusterHealthRequest = ClusterHealthRequest().indicesOptions(IndicesOptions.lenientExpandHidden())

        val startTime = Instant.now()
        val clusterHealthResponse: ClusterHealthResponse =
            targetClient.suspendUntil { admin().cluster().health(clusterHealthRequest, it) }
        val endTime = Instant.now()

        // Manually calculating the latency of ClusterHealth call as the API does not return that metric
        val latency = Duration.between(startTime, endTime).toMillis()

        val indexInfos = mutableListOf<IndexInfo>()
        clusterHealthResponse.indices.forEach { (index, health) ->
            indexInfos.add(IndexInfo(index, health.status))
        }
        return ClusterInfo(clusterAlias, indexInfos, latency)
    }
}
