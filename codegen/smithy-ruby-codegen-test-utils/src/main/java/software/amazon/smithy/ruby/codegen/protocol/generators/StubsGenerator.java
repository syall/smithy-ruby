/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.ruby.codegen.protocol.generators;

import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.ruby.codegen.GenerationContext;
import software.amazon.smithy.ruby.codegen.generators.RestStubsGeneratorBase;

public class StubsGenerator extends RestStubsGeneratorBase {

    public StubsGenerator(GenerationContext context) {
        super(context);
    }

    @Override
    protected void renderUnionStubMethod(UnionShape shape) {

    }

    @Override
    protected void renderListStubMethod(ListShape shape) {

    }

    @Override
    protected void renderMapStubMethod(MapShape shape) {

    }

    @Override
    protected void renderStructureStubMethod(StructureShape shape) {

    }

    @Override
    protected void renderPayloadBodyStub(Shape outputShape, MemberShape payloadMember, Shape target) {
        if (target.hasTrait(StreamingTrait.class)) {
            renderStreamingStub(outputShape);
        }
    }

    @Override
    protected void renderBodyStub(Shape outputShape) {

    }

    @Override
    protected void renderErrorStubMethod(Shape errorShape) {

    }
}


