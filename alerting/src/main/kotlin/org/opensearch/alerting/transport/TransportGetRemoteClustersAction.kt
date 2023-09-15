/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.transport

import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionListener
import org.opensearch.action.support.ActionFilters
import org.opensearch.action.support.HandledTransportAction
import org.opensearch.alerting.action.GetRemoteClustersAction
import org.opensearch.alerting.action.GetRemoteClustersRequest
import org.opensearch.alerting.action.GetRemoteClustersResponse
import org.opensearch.alerting.action.GetRemoteClustersResponse.ClusterInfo
import org.opensearch.client.Client
import org.opensearch.cluster.service.ClusterService
import org.opensearch.common.inject.Inject
import org.opensearch.common.settings.Settings
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.tasks.Task
import org.opensearch.transport.TransportService

private val log = LogManager.getLogger(TransportGetRemoteClustersAction::class.java)

class TransportGetRemoteClustersAction @Inject constructor(
    val transportService: TransportService,
    val client: Client,
    actionFilters: ActionFilters,
    val xContentRegistry: NamedXContentRegistry,
    val clusterService: ClusterService,
    settings: Settings,
) : HandledTransportAction<GetRemoteClustersRequest, GetRemoteClustersResponse>(
    GetRemoteClustersAction.NAME,
    transportService,
    actionFilters,
    ::GetRemoteClustersRequest
) {

    override fun doExecute(
        task: Task,
        request: GetRemoteClustersRequest,
        listener: ActionListener<GetRemoteClustersResponse>
    ) {
        if (!transportService.remoteClusterService.isCrossClusterSearchEnabled) {
            log.debug("Cross-cluster search is disabled.")
            return
        }

        client.threadPool().threadContext.stashContext().use {
            val clusterInfoList = mutableListOf<ClusterInfo>()
            clusterInfoList.add(
                ClusterInfo(
                    clusterAlias = clusterService.clusterName.value(),
                    connected = true,
                    hubCluster = true
                )
            )
            transportService.remoteClusterService.remoteConnectionInfos.forEach {
                clusterInfoList.add(
                    ClusterInfo(
                        clusterAlias = it.clusterAlias,
                        connected = it.isConnected,
                        hubCluster = false
                    )
                )
            }
            listener.onResponse(GetRemoteClustersResponse(clusterInfoList))
        }
    }
}
