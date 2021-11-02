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
 *   Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.alerting.settings

import org.opensearch.action.ActionRequest
import org.opensearch.action.admin.cluster.health.ClusterHealthRequest
import org.opensearch.action.admin.cluster.node.stats.NodesStatsRequest
import org.opensearch.action.admin.cluster.node.tasks.list.ListTasksRequest
import org.opensearch.action.admin.cluster.repositories.get.GetRepositoriesRequest
import org.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest
import org.opensearch.action.admin.cluster.state.ClusterStateRequest
import org.opensearch.action.admin.cluster.stats.ClusterStatsRequest
import org.opensearch.action.admin.cluster.tasks.PendingClusterTasksRequest
import org.opensearch.action.admin.indices.recovery.RecoveryRequest
import org.opensearch.alerting.core.model.LocalUriInput
import org.opensearch.alerting.core.model.LocalUriInput.ApiType
import org.opensearch.common.xcontent.XContentHelper
import org.opensearch.common.xcontent.json.JsonXContent

/**
 * A class that supports storing a unique set of API paths that can be accessed by general users.
 */
class SupportedApiSettings {
    companion object {
        const val RESOURCE_FILE = "supported_json_payloads.json"

        /**
         * The key in this map represents the path to call an API.
         *
         * NOTE: Paths should conform to the following pattern:
         * "/_cluster/stats"
         *
         * The value in these maps represents a path root mapped to a list of paths to field values.
         * If the value mapped to an API is an empty map, no fields will be redacted from the API response.
         *
         * NOTE: Keys in this map should consist of root components of the response body; e.g.,:
         * "indices"
         *
         * Values in these maps should consist of the remaining fields in the path
         * to the supported value separated by periods; e.g.,:
         * "shards.total",
         * "shards.index.shards.min"
         *
         * In this example for ClusterStats, the response will only include
         * the values at the end of these two paths:
         * "/_cluster/stats": {
         *      "indices": [
         *          "shards.total",
         *          "shards.index.shards.min"
         *      ]
         * }
         */
        private var supportedApiList = HashMap<String, Map<String, ArrayList<String>>>()

        init {
            val supportedJsonPayloads = SupportedApiSettings::class.java.getResource(RESOURCE_FILE)
            @Suppress("UNCHECKED_CAST")
            if (supportedJsonPayloads != null) supportedApiList =
                XContentHelper.convertToMap(JsonXContent.jsonXContent, supportedJsonPayloads.readText(), false) as HashMap<String, Map<String, ArrayList<String>>>
        }

        /**
         * Returns the map of all supported json payload associated with the provided path from supportedApiList.
         * @param path The path for the requested API.
         * @return The map of the supported json payload for the requested API.
         * @throws IllegalArgumentException When supportedApiList does not contain a value for the provided key.
         */
        fun getSupportedJsonPayload(path: String): Map<String, ArrayList<String>> {
            return supportedApiList[path] ?: throw IllegalArgumentException("API path not in supportedApiList.")
        }

        /**
         * Will return an [ActionRequest] for the API associated with that path.
         * Will otherwise throw an exception.
         * @param localUriInput The [LocalUriInput] to resolve.
         * @throws IllegalArgumentException when the requested API is not supported.
         * @return The [ActionRequest] for the API associated with the provided [LocalUriInput].
         */
        fun resolveToActionRequest(localUriInput: LocalUriInput): ActionRequest {
            val pathParams = localUriInput.getPathParams()
            return when (localUriInput.apiType) {
                // TODO: For CAT_ALIASES, implement toXContent parsing logic for response.
//                ApiType.CAT_ALIASES -> {
//                    val pathParamsArray = pathParams.split(",").toTypedArray()
//                    return GetAliasesRequest(*pathParamsArray)
//                }
                ApiType.CAT_PENDING_TASKS -> PendingClusterTasksRequest()
                ApiType.CAT_RECOVERY -> {
                    if (pathParams.isEmpty()) return RecoveryRequest()
                    val pathParamsArray = pathParams.split(",").toTypedArray()
                    return RecoveryRequest(*pathParamsArray)
                }
                ApiType.CAT_REPOSITORIES -> GetRepositoriesRequest()
                ApiType.CAT_SNAPSHOTS -> {
                    return GetSnapshotsRequest(pathParams, arrayOf(GetSnapshotsRequest.ALL_SNAPSHOTS))
                }
                ApiType.CAT_TASKS -> ListTasksRequest()
                ApiType.CLUSTER_HEALTH -> {
                    if (pathParams.isEmpty()) return ClusterHealthRequest()
                    val pathParamsArray = pathParams.split(",").toTypedArray()
                    return ClusterHealthRequest(*pathParamsArray)
                }
                ApiType.CLUSTER_SETTINGS -> ClusterStateRequest().routingTable(false).nodes(false)
                ApiType.CLUSTER_STATS -> {
                    if (pathParams.isEmpty()) return ClusterStatsRequest()
                    val pathParamsArray = pathParams.split(",").toTypedArray()
                    return ClusterStatsRequest(*pathParamsArray)
                }
                // TODO: For NODES_HOT_THREADS, determine what the response payload should look like.
//                ApiType.NODES_HOT_THREADS -> {
//                    val pathParamsArray = pathParams.split(",").toTypedArray()
//                    return NodesHotThreadsRequest(*pathParamsArray)
//                }
                ApiType.NODES_STATS -> NodesStatsRequest()
                else -> throw IllegalArgumentException("Unsupported API.")
            }
        }

        /**
         * Confirms whether the provided path is in [supportedApiList].
         * Throws an exception if the provided path is not on the list; otherwise performs no action.
         * @param localUriInput The [LocalUriInput] to validate.
         * @throws IllegalArgumentException when supportedApiList does not contain the provided path.
         */
        fun validateApiType(localUriInput: LocalUriInput) {
            if (!supportedApiList.keys.contains(localUriInput.apiType.defaultPath))
                throw IllegalArgumentException("API path not in supportedApiList.")
        }
    }
}
