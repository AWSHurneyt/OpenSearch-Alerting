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
import org.opensearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.opensearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.opensearch.action.admin.indices.resolve.ResolveIndexAction
import org.opensearch.action.support.ActionFilters
import org.opensearch.action.support.HandledTransportAction
import org.opensearch.action.support.IndicesOptions
import org.opensearch.alerting.action.GetRemoteIndexesAction
import org.opensearch.alerting.action.GetRemoteIndexesRequest
import org.opensearch.alerting.action.GetRemoteIndexesResponse
import org.opensearch.alerting.action.GetRemoteIndexesResponse.ClusterIndexes
import org.opensearch.alerting.action.GetRemoteIndexesResponse.ClusterIndexes.ClusterIndex
import org.opensearch.alerting.opensearchapi.suspendUntil
import org.opensearch.alerting.util.AlertingException
import org.opensearch.alerting.util.CrossClusterMonitorUtils
import org.opensearch.alerting.util.clusterMetricsMonitorHelpers.toMap
import org.opensearch.client.Client
import org.opensearch.cluster.service.ClusterService
import org.opensearch.common.inject.Inject
import org.opensearch.common.settings.Settings
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.tasks.Task
import org.opensearch.transport.TransportService
import java.time.Duration
import java.time.Instant

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
        log.info("hurneyt TransportGetRemoteIndexesAction START")
        client.threadPool().threadContext.stashContext().use {
            scope.launch {
                val clusterIndexesList = mutableListOf<ClusterIndexes>()

                var resolveIndexResponse: ResolveIndexAction.Response? = null
                try {
                    resolveIndexResponse = getRemoteClusters(CrossClusterMonitorUtils.parseIndexesForRemoteSearch(request.indexes, clusterService))
                    log.info("hurneyt TransportGetRemoteIndexesAction::resolveIndexResponse = {}", resolveIndexResponse.toMap())
                } catch (e: Exception) {
                    log.error("Failed to retrieve indexes for request $request", e)
                    listener.onFailure(AlertingException.wrap(e))
                }

                val resolvedIndexes = resolveIndexResponse?.indices?.map { it.name } ?: emptyList()
                log.info("hurneyt TransportGetRemoteIndexesAction::resolvedIndexes = {}", resolvedIndexes)

                val clusterIndexesMap = CrossClusterMonitorUtils.separateClusterIndexes(resolvedIndexes, clusterService)
                log.info("hurneyt TransportGetRemoteIndexesAction::clusterIndexesMap = {}", clusterIndexesMap)

                clusterIndexesMap.forEach { (clusterName, indexes) ->
                    log.info("hurneyt TransportGetRemoteIndexesAction::clusterIndexesMap clusterName = {}", clusterName)
                    log.info("hurneyt TransportGetRemoteIndexesAction::clusterIndexesMap indexes = {}", indexes)
                    val targetClient = CrossClusterMonitorUtils.getClientForCluster(clusterName, client, clusterService)

                    val startTime = Instant.now()
                    var clusterHealthResponse: ClusterHealthResponse? = null
                    try {
                        clusterHealthResponse = getHealthStatuses(targetClient, indexes)
                    } catch (e: Exception) {
                        log.error("Failed to retrieve health statuses for request $request", e)
                        listener.onFailure(AlertingException.wrap(e))
                    }
                    val endTime = Instant.now()

                    log.info("hurneyt TransportGetRemoteIndexesAction::clusterHealthResponse keys = {}", clusterHealthResponse?.indices?.keys)

                    val latency = Duration.between(startTime, endTime).toMillis()

                    var mappingsResponse: GetMappingsResponse? = null
                    if (request.includeMappings) {
                        try {
                            mappingsResponse = getIndexMappings(targetClient, indexes)
                        } catch (e: Exception) {
                            log.error("Failed to retrieve mappings for request $request", e)
                            listener.onFailure(AlertingException.wrap(e))
                        }
                    }

                    val clusterIndexList = mutableListOf<ClusterIndex>()
                    indexes.forEach {
                        clusterIndexList.add(
                            ClusterIndex(
                                indexName = it,
                                indexHealth = clusterHealthResponse!!.indices[it]!!.status,
                                mappings = mappingsResponse?.mappings?.get(it)
                            )
                        )
                    }

                    clusterIndexesList.add(
                        ClusterIndexes(
                            clusterName = clusterName,
                            clusterHealth = clusterHealthResponse!!.status,
                            hubCluster = clusterName == clusterService.clusterName.value(),
                            indexes = clusterIndexList,
                            latency = latency
                        )
                    )
                }
                log.info("hurneyt TransportGetRemoteIndexesAction::clusterIndexesList = {}", clusterIndexesList)
                log.info("hurneyt TransportGetRemoteIndexesAction END")
                listener.onResponse(GetRemoteIndexesResponse(clusterIndexes = clusterIndexesList))
            }
        }
    }

    private suspend fun getRemoteClusters(parsedIndexes: List<String>): ResolveIndexAction.Response {
        val resolveRequest = ResolveIndexAction.Request(
            parsedIndexes.toTypedArray(),
            ResolveIndexAction.Request.DEFAULT_INDICES_OPTIONS
        )
        return client.suspendUntil {
            // TODO hurneyt: return aliases as well
            admin().indices().resolveIndex(resolveRequest, it)
        }
    }
    private suspend fun getHealthStatuses(targetClient: Client, parsedIndexesNames: List<String>): ClusterHealthResponse {
        val clusterHealthRequest = ClusterHealthRequest()
            .indices(*parsedIndexesNames.toTypedArray())
            .indicesOptions(IndicesOptions.lenientExpandHidden())
        return targetClient.suspendUntil {
            admin().cluster().health(clusterHealthRequest, it)
        }
    }

    private suspend fun getIndexMappings(targetClient: Client, parsedIndexNames: List<String>): GetMappingsResponse {
        val getMappingsRequest = GetMappingsRequest().indices(*parsedIndexNames.toTypedArray())
        return targetClient.suspendUntil {
            admin().indices().getMappings(getMappingsRequest, it)
        }
    }
}