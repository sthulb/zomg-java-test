/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.testutils.tracing;

import java.util.ArrayList;
import java.util.List;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;

public class Trace {
    private final List<SubSegment> subsegments = new ArrayList<>();

    public Trace() {
    }

    public List<SubSegment> getSubsegments() {
        return subsegments;
    }

    public void addSubSegment(SubSegment subSegment) {
        subsegments.add(subSegment);
    }
}
