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

package org.opensearch.alerting.util

import org.opensearch.test.OpenSearchTestCase

class SupportedApiSettingsExtensionsTests : OpenSearchTestCase() {
    private var expectedResponse = hashMapOf<String, Any>()
    private var mappedResponse = hashMapOf<String, Any>()
    private var supportedJsonPayload = hashMapOf<String, ArrayList<String>>()

    fun `test redactFieldsFromResponse with non-empty supportedJsonPayload`() {
        // GIVEN
        mappedResponse = hashMapOf(
            ("pathRoot1" to hashMapOf(("pathRoot1_subPath1" to 11), ("pathRoot1_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath1" to 121), ("pathRoot1_subPath2_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath2_subPath1" to 1221))))))),
            ("pathRoot2" to hashMapOf(("pathRoot2_subPath1" to 21), ("pathRoot2_subPath2" to setOf(221, 222, "223string")))),
            ("pathRoot3" to hashMapOf(("pathRoot3_subPath1" to 31), ("pathRoot3_subPath2" to setOf(321, 322, "323string"))))
        )

        supportedJsonPayload = hashMapOf(
            ("pathRoot1" to arrayListOf("pathRoot1_subPath1", "pathRoot1_subPath2.pathRoot1_subPath2_subPath2.pathRoot1_subPath2_subPath2_subPath1")),
            ("pathRoot2" to arrayListOf("pathRoot2_subPath2")),
            ("pathRoot3" to arrayListOf())
        )

        expectedResponse = hashMapOf(
            ("pathRoot1" to hashMapOf(("pathRoot1_subPath1" to 11), ("pathRoot1_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath2_subPath1" to 1221))))))),
            ("pathRoot2" to hashMapOf(("pathRoot2_subPath2" to setOf(221, 222, "223string")))),
            ("pathRoot3" to hashMapOf(("pathRoot3_subPath1" to 31), ("pathRoot3_subPath2" to setOf(321, 322, "323string"))))
        )

        // WHEN
        val result = redactFieldsFromResponse(mappedResponse, supportedJsonPayload)

        // THEN
        assertEquals(expectedResponse, result)
    }

    fun `test redactFieldsFromResponse with empty supportedJsonPayload`() {
        // GIVEN
        mappedResponse = hashMapOf(
            ("pathRoot1" to hashMapOf(("pathRoot1_subPath1" to 11), ("pathRoot1_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath1" to 121), ("pathRoot1_subPath2_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath2_subPath1" to 1221))))))),
            ("pathRoot2" to hashMapOf(("pathRoot2_subPath1" to 21), ("pathRoot2_subPath2" to setOf(221, 222, "223string")))),
            ("pathRoot3" to 3)
        )

        expectedResponse = hashMapOf(
            ("pathRoot1" to hashMapOf(("pathRoot1_subPath1" to 11), ("pathRoot1_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath1" to 121), ("pathRoot1_subPath2_subPath2" to hashMapOf(("pathRoot1_subPath2_subPath2_subPath1" to 1221))))))),
            ("pathRoot2" to hashMapOf(("pathRoot2_subPath1" to 21), ("pathRoot2_subPath2" to setOf(221, 222, "223string")))),
            ("pathRoot3" to 3)
        )

        // WHEN
        val result = redactFieldsFromResponse(mappedResponse, supportedJsonPayload)

        // THEN
        assertEquals(expectedResponse, result)
    }
}
