/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.alerting.core.resthandler

import org.opensearch.alerting.core.action.node.ScheduledJobsStatsAction
import org.opensearch.alerting.core.action.node.ScheduledJobsStatsRequest
import org.opensearch.client.node.NodeClient
import org.opensearch.common.Strings
import org.opensearch.rest.BaseRestHandler
import org.opensearch.rest.RestHandler
import org.opensearch.rest.RestHandler.Route
import org.opensearch.rest.RestRequest

import org.opensearch.rest.RestRequest.Method.GET
import org.opensearch.rest.action.RestActions
import java.util.Locale
import java.util.TreeSet

/**
 * RestScheduledJobStatsHandler is handler for getting ScheduledJob Stats.
 */
class RestScheduledJobStatsHandler(private val path: String) : BaseRestHandler() {

    companion object {
        const val JOB_SCHEDULING_METRICS: String = "job_scheduling_metrics"
        const val JOBS_INFO: String = "jobs_info"
        private val METRICS = mapOf<String, (ScheduledJobsStatsRequest) -> Unit>(
                JOB_SCHEDULING_METRICS to { it -> it.jobSchedulingMetrics = true },
                JOBS_INFO to { it -> it.jobsInfo = true }
        )
    }

    override fun getName(): String {
        return "${path}_jobs_stats"
    }

    override fun routes(): List<Route> {
        return listOf()
    }

    override fun replacedRoutes(): MutableList<RestHandler.ReplacedRoute> {
        return mutableListOf(
            RestHandler.ReplacedRoute(
                GET,
                "/_plugins/$path/{nodeId}/stats/",
                GET,
                "/_opendistro/$path/{nodeId}/stats/"
            ),
            RestHandler.ReplacedRoute(
                GET,
                "/_plugins/$path/{nodeId}/stats/{metric}",
                GET,
                "/_opendistro/$path/{nodeId}/stats/{metric}"
            ),
            RestHandler.ReplacedRoute(
                GET,
                "/_plugins/$path/stats/",
                GET,
                "/_opendistro/$path/stats/"
            ),
            RestHandler.ReplacedRoute(
                GET,
                "/_plugins/$path/stats/{metric}",
                GET,
                "/_opendistro/$path/stats/{metric}"
            )
        )
    }

    override fun prepareRequest(request: RestRequest, client: NodeClient): RestChannelConsumer {
        val scheduledJobNodesStatsRequest = getRequest(request)
        return RestChannelConsumer { channel ->
            client.execute(
                    ScheduledJobsStatsAction.INSTANCE,
                    scheduledJobNodesStatsRequest,
                    RestActions.NodesResponseRestListener(channel)
            )
        }
    }

    private fun getRequest(request: RestRequest): ScheduledJobsStatsRequest {
        val nodesIds = Strings.splitStringByCommaToArray(request.param("nodeId"))
        val metrics = Strings.tokenizeByCommaToSet(request.param("metric"))
        val scheduledJobsStatsRequest = ScheduledJobsStatsRequest(nodesIds)
        scheduledJobsStatsRequest.timeout(request.param("timeout"))

        if (metrics.isEmpty()) {
            return scheduledJobsStatsRequest
        } else if (metrics.size == 1 && metrics.contains("_all")) {
            scheduledJobsStatsRequest.all()
        } else if (metrics.contains("_all")) {
            throw IllegalArgumentException(
                    String.format(Locale.ROOT,
                            "request [%s] contains _all and individual metrics [%s]",
                            request.path(),
                            request.param("metric")))
        } else {
            // use a sorted set so the unrecognized parameters appear in a reliable sorted order
            scheduledJobsStatsRequest.clear()
            val invalidMetrics = TreeSet<String>()
            for (metric in metrics) {
                val handler = METRICS[metric]
                if (handler != null) {
                    handler.invoke(scheduledJobsStatsRequest)
                } else {
                    invalidMetrics.add(metric)
                }
            }

            if (!invalidMetrics.isEmpty()) {
                throw IllegalArgumentException(unrecognized(request, invalidMetrics, METRICS.keys, "metric"))
            }
        }
        return scheduledJobsStatsRequest
    }
}
