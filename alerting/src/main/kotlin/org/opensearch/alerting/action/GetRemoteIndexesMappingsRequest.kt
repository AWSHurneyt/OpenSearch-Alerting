/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.alerting.util.IndexUtils.Companion.VALID_INDEX_NAME_REGEX
import org.opensearch.common.io.stream.StreamInput
import java.io.IOException

private val log = LogManager.getLogger(GetRemoteIndexesMappingsRequest::class.java)

class GetRemoteIndexesMappingsRequest : ActionRequest {
    val indexes: List<String>

    constructor(indexes: List<String>) {
        this.indexes = indexes
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        indexes = sin.readStringList()
    )

    override fun validate(): ActionRequestValidationException? {
        var validationException: ActionRequestValidationException? = null

        // TODO hurneyt
//        if (indexes.isEmpty()) {
//            validationException = ValidateActions.addValidationError(
//                "Request contains no indexes.", validationException
//            )
//        }
//
//        indexes.any {
//            var error = validateClusterAlias(it.clusterAlias)
//            if (!error.isNullOrEmpty()) {
//                validationException = ValidateActions.addValidationError(
//                    error, validationException
//                )
//            } else {
//                error = validateIndexNames(it.indexes)
//                if (!error.isNullOrEmpty()) {
//                    validationException = ValidateActions.addValidationError(
//                        error, validationException
//                    )
//                }
//            }
//            return@any error.isNullOrEmpty()
//        }
        return validationException
    }

    private fun validateClusterAlias(clusterAlias: String): String? {
        // TODO hurneyt: need to confirm character limitations for cluster names. The following opensearch resources don't list them
        //  https://opensearch.org/docs/latest/tuning-your-cluster/index/#step-1-name-a-cluster
        //  https://docs.aws.amazon.com/opensearch-service/latest/developerguide/createupdatedomains.html#:~:text=For-,Domain%20name,-%2C%20enter%20a%20domain
        val isValid = true
        return if (isValid) null else "Request contains an invalid cluster alias."
    }

    private fun validateIndexNames(indexes: List<String>): String? {
        if (indexes.isEmpty()) return "Request contains an empty list of indexes."
        val isValid = indexes.any {
            log.info("hurneyt validateIndexNames indexName = $it")
            VALID_INDEX_NAME_REGEX.containsMatchIn(it)
        }
        log.info("hurneyt validateIndexNames isValid = $isValid")
        return if (isValid) null else "Request contains an invalid index name."
    }

    companion object {
        const val INDEXES_FIELD = "indexes"
    }
}
