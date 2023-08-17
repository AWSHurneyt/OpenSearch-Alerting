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
import org.opensearch.action.search.SearchRequest
import org.opensearch.action.search.SearchResponse
import org.opensearch.action.support.ActionFilters
import org.opensearch.action.support.HandledTransportAction
import org.opensearch.alerting.action.GetRemoteIndexesLatencyAction
import org.opensearch.alerting.action.GetRemoteIndexesLatencyRequest
import org.opensearch.alerting.action.GetRemoteIndexesLatencyResponse
import org.opensearch.alerting.action.GetRemoteIndexesLatencyResponse.RemoteIndexesLatency
import org.opensearch.alerting.action.GetRemoteIndexesLatencyResponse.RemoteIndexesLatency.IndexLatencyInfo
import org.opensearch.alerting.opensearchapi.suspendUntil
import org.opensearch.alerting.util.AlertingException
import org.opensearch.client.Client
import org.opensearch.cluster.service.ClusterService
import org.opensearch.common.inject.Inject
import org.opensearch.common.settings.Settings
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.tasks.Task
import org.opensearch.transport.TransportService

private val log = LogManager.getLogger(TransportGetRemoteIndexesLatencyAction::class.java)
private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

class TransportGetRemoteIndexesLatencyAction @Inject constructor(
    val transportService: TransportService,
    val client: Client,
    actionFilters: ActionFilters,
    val xContentRegistry: NamedXContentRegistry,
    val clusterService: ClusterService,
    settings: Settings,
) : HandledTransportAction<GetRemoteIndexesLatencyRequest, GetRemoteIndexesLatencyResponse>(
    GetRemoteIndexesLatencyAction.NAME,
    transportService,
    actionFilters,
    ::GetRemoteIndexesLatencyRequest
) {
    override fun doExecute(
        task: Task,
        request: GetRemoteIndexesLatencyRequest,
        listener: ActionListener<GetRemoteIndexesLatencyResponse>
    ) {
        val remoteClusterIndexes = request.indexes
        client.threadPool().threadContext.stashContext().use {
            val clusterIndexes = mutableListOf<RemoteIndexesLatency>()
            remoteClusterIndexes.forEach {
                val remoteClient = client.getRemoteClusterClient(clusterService.clusterName.value())
                val indexLatencyInfos = mutableListOf<IndexLatencyInfo>()
                it.indexes.forEach { indexName ->
                    scope.launch {
                        val latency = getLatency(it.clusterAlias, indexName, remoteClient, listener)
                        indexLatencyInfos.add(IndexLatencyInfo(indexName, latency))
                    }
                }
                clusterIndexes.add(RemoteIndexesLatency(it.clusterAlias, indexLatencyInfos))
            }
            listener.onResponse(GetRemoteIndexesLatencyResponse(clusterIndexes))
        }
    }

    private suspend fun getLatency(
        clusterAlias: String,
        indexName: String,
        remoteClient: Client,
        listener: ActionListener<GetRemoteIndexesLatencyResponse>
    ): Long? {
        val remoteIndexName = "$clusterAlias:$indexName"
        var latency: Long? = null
        try {
            val searchRequest = SearchRequest().indices(remoteIndexName)
                .source(SearchSourceBuilder.searchSource().size(1).query(QueryBuilders.matchAllQuery()))
            val searchResponse: SearchResponse = remoteClient.suspendUntil { search(searchRequest, it) }
            latency = searchResponse.took.millis
        } catch (e: Exception) {
            log.error("Failed to retrieve latency for $remoteIndexName", e)
            listener.onFailure(AlertingException.wrap(e))
        }
        return latency
    }
}
