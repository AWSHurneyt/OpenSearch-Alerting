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
import org.opensearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.opensearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.opensearch.action.support.ActionFilters
import org.opensearch.action.support.HandledTransportAction
import org.opensearch.alerting.action.GetRemoteIndexesMappingsAction
import org.opensearch.alerting.action.GetRemoteIndexesMappingsRequest
import org.opensearch.alerting.action.GetRemoteIndexesMappingsResponse
import org.opensearch.alerting.opensearchapi.suspendUntil
import org.opensearch.alerting.util.AlertingException
import org.opensearch.alerting.util.CrossClusterMonitorUtils
import org.opensearch.client.Client
import org.opensearch.cluster.metadata.MappingMetadata
import org.opensearch.cluster.service.ClusterService
import org.opensearch.common.inject.Inject
import org.opensearch.common.settings.Settings
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.tasks.Task
import org.opensearch.transport.TransportService

private val log = LogManager.getLogger(TransportGetRemoteIndexesMappingsAction::class.java)
private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

class TransportGetRemoteIndexesMappingsAction @Inject constructor(
    val transportService: TransportService,
    val client: Client,
    actionFilters: ActionFilters,
    val xContentRegistry: NamedXContentRegistry,
    val clusterService: ClusterService,
    settings: Settings,
) : HandledTransportAction<GetRemoteIndexesMappingsRequest, GetRemoteIndexesMappingsResponse>(
    GetRemoteIndexesMappingsAction.NAME,
    transportService,
    actionFilters,
    ::GetRemoteIndexesMappingsRequest
) {
    override fun doExecute(
        task: Task,
        request: GetRemoteIndexesMappingsRequest,
        listener: ActionListener<GetRemoteIndexesMappingsResponse>
    ) {
        log.info("hurneyt TransportGetRemoteIndexesMappingsAction::request.indexes = ${request.indexes}")
        val remoteClusterIndexes = CrossClusterMonitorUtils.separateClusterIndexes(request.indexes, clusterService)

        log.info("hurneyt TransportGetRemoteIndexesMappingsAction::remoteClusterIndexes = $remoteClusterIndexes")

        client.threadPool().threadContext.stashContext().use {
            scope.launch {
                val clusterIndexes = hashMapOf<String, MappingMetadata>()
                try {
                    remoteClusterIndexes.forEach { (clusterName, indexes) ->
                        val targetClient = CrossClusterMonitorUtils.getClientForCluster(clusterName, client, clusterService)
                        val getMappingsRequest = GetMappingsRequest().indices(*indexes.toTypedArray())
                        val getMappingsResponse: GetMappingsResponse = targetClient.suspendUntil {
                            admin().indices().getMappings(getMappingsRequest, it)
                        }
                        getMappingsResponse.mappings.forEach {
                            log.info("hurneyt TransportGetRemoteIndexesMappingsAction::it.key = ${it.key}")
                            log.info("hurneyt TransportGetRemoteIndexesMappingsAction::it.value = ${it.value.sourceAsMap}")
                            val formattedIndexName = CrossClusterMonitorUtils.formatClusterAndIndexNames(clusterName, it.key)
                            log.info("hurneyt TransportGetRemoteIndexesMappingsAction::formattedIndexName = $formattedIndexName")
                            clusterIndexes[formattedIndexName] = it.value
                        }
                    }
                } catch (e: Exception) {
                    log.error("Failed to retrieve index mappings for request $request", e)
                    listener.onFailure(AlertingException.wrap(e))
                }
                listener.onResponse(GetRemoteIndexesMappingsResponse(clusterIndexes))
            }
        }
    }
}
