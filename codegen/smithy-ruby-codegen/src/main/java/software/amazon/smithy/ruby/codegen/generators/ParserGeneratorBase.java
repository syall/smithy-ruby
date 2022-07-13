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

package software.amazon.smithy.ruby.codegen.generators;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.ruby.codegen.GenerationContext;
import software.amazon.smithy.ruby.codegen.RubyCodeWriter;
import software.amazon.smithy.ruby.codegen.RubySettings;
import software.amazon.smithy.ruby.codegen.RubySymbolProvider;
import software.amazon.smithy.ruby.codegen.util.Streaming;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Base class for Parser Generators which iterates shapes and builds skeleton classes.
 *
 * <p>
 * Protocols should extend this class to get common functionality -
 * generates the framework and non-protocol specific parts of
 * parsers.rb.
 */
@SmithyUnstableApi
public abstract class ParserGeneratorBase {

    private static final Logger LOGGER =
            Logger.getLogger(RestParserGeneratorBase.class.getName());

    protected final GenerationContext context;
    protected final RubySettings settings;
    protected final Model model;
    protected final Set<ShapeId> generatedParsers;
    protected final SymbolProvider symbolProvider;

    protected final RubyCodeWriter writer;

    public ParserGeneratorBase(GenerationContext context) {
        this.context = context;
        this.settings = context.settings();
        this.model = context.model();
        this.generatedParsers = new HashSet<>();
        this.writer = new RubyCodeWriter(context.settings().getModule() + "::Parsers");
        this.symbolProvider = new RubySymbolProvider(model, settings, "Parsers", true);
    }

    /**
     * Called to render a union's parser.
     * The rendered code should return the correct member Type class.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class MyUnion
     *   #### START code generated by this method
     *   def self.parse(map)
     *     key, value = map.flatten
     *     case key
     *     when 'string_value'
     *       Types::MyUnion::StringValue.new(value) if value
     *     else
     *       Types::MyUnion::Unknown.new({name: key, value: value})
     *     end
     *   end
     *   ### END code generated by this method
     * end
     * }</pre>
     *
     * @param s the union shape
     */
    protected abstract void renderUnionParseMethod(UnionShape s);


    /**
     * Called to render a map's parse method.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class StringMap
     *   #### START code generated by this method
     *   def self.parse(map)
     *     data = {}
     *     map.map do |key, value|
     *       data[key] = value
     *     end
     *     data
     *   end
     *   #### END code generated by this method
     * end
     * }</pre>
     *
     * @param s shape to generate for
     */
    protected abstract void renderMapParseMethod(MapShape s);

    /**
     * Called to render a list's parse method.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class StringList
     *   #### START code generated by this method
     *   def self.parse(list)
     *     list.map do |value|
     *       value
     *     end
     *   end
     *   #### END code generated by this method
     * end
     * }</pre>
     *
     * @param s shape to generate for
     */
    protected abstract void renderListParseMethod(ListShape s);

    /**
     * Called to render a structure's parse method.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class SimpleStruct
     *   ### START code generated by this method
     *   def self.parse(map)
     *     data = Types::SimpleStruct.new
     *     data.value = map['value']
     *     data.timestamp = Time.parse(map['timestamp']) if map['timestamp']
     *     return data
     *   end
     *   #### END code generated by this method
     * end
     * }</pre>
     *
     * @param s shape to generate for
     */
    protected abstract void renderStructureParseMethod(StructureShape s);

    /**
     * Called to render an operation's parse method.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class Operation
     *   ### START code generated by this method
     *   def self.parse(http_resp)
     *     data = Types::OperationOutput.new
     *     map = Hearth::JSON.load(http_resp.body)
     *     data.contents = (Parsers::Contents.parse(map['contents']) unless map['contents'].nil?)
     *     data
     *   end
     *   #### END code generated by this method
     * end
     * }</pre>
     *
     * @param operation   the operation to generate a parse method for
     * @param outputShape the operation's outputShape
     */
    protected abstract void renderOperationParseMethod(OperationShape operation, Shape outputShape);

    /**
     * Called to render an error's parse method.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class ComplexError
     *   ### START code generated by this method
     *   def self.parse(http_resp)
     *     data = Types::ComplexError.new
     *     data.header = http_resp.headers['X-Header']
     *     map = Hearth::JSON.load(http_resp.body)
     *     data.top_level = map['TopLevel']
     *     data.nested = (Parsers::ComplexNestedErrorData.parse(map['Nested']) unless map['Nested'].nil?)
     *     data
     *   end
     *   ### END code generated by this method
     * end
     * }</pre>
     *
     * @param s the error shape to generate a parse method for
     */
    protected abstract void renderErrorParseMethod(Shape s);

    public void render(FileManifest fileManifest) {
        writer
                .includePreamble()
                .includeRequires()
                .openBlock("module $L", settings.getModule())
                .openBlock("module Parsers")
                .call(() -> renderParsers())
                .closeBlock("end")
                .closeBlock("end");

        String fileName = settings.getGemName() + "/lib/" + settings.getGemName() + "/parsers.rb";
        fileManifest.writeFile(fileName, writer.toString());
        LOGGER.fine("Wrote parsers to " + fileName);
    }

    protected void renderParsers() {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Set<OperationShape> containedOperations = new TreeSet<>(
                topDownIndex.getContainedOperations(context.service()));
        containedOperations.stream()
                .filter((o) -> !Streaming.isEventStreaming(model, o))
                .sorted(Comparator.comparing((o) -> o.getId().getName()))
                .forEach(o -> {
                    Shape outputShape = model.expectShape(o.getOutputShape());
                    renderParsersForOperation(o, outputShape);
                    generatedParsers.add(o.toShapeId());
                    generatedParsers.add(outputShape.toShapeId());

                    Iterator<Shape> it = new Walker(model).iterateShapes(outputShape);
                    while (it.hasNext()) {
                        Shape s = it.next();
                        if (!generatedParsers.contains(s.getId())) {
                            generatedParsers.add(s.getId());
                            s.accept(new ParserClassGenerator());
                        }
                    }

                    for (ShapeId errorShapeId : o.getErrors()) {
                        Iterator<Shape> errIt = new Walker(model).iterateShapes(model.expectShape(errorShapeId));
                        while (errIt.hasNext()) {
                            Shape s = errIt.next();
                            if (!generatedParsers.contains(s.getId())) {
                                generatedParsers.add(s.getId());
                                if (s.hasTrait(ErrorTrait.class)) {
                                    renderErrorParser(s);
                                } else {
                                    s.accept(new ParserClassGenerator());
                                }
                            }
                        }
                    }
                });
    }

    protected void renderParsersForOperation(OperationShape operation, Shape outputShape) {

        writer
                .write("")
                .write("# Operation Parser for $L", operation.getId().getName())
                .openBlock("class $L", symbolProvider.toSymbol(operation).getName())
                .call(() -> renderOperationParseMethod(operation, outputShape))
                .closeBlock("end");
        LOGGER.finer("Generated parser class for " + operation.getId().getName());
    }

    protected void renderErrorParser(Shape s) {
        writer
                .write("")
                .write("# Error Parser for $L", s.getId().getName())
                .openBlock("class $T", symbolProvider.toSymbol(s))
                .call(() -> renderErrorParseMethod(s))
                .closeBlock("end");
        LOGGER.finer("Generated Error parser for " + s.getId().getName());
    }

    protected void renderStreamingBodyParser(Shape outputShape) {
        MemberShape streamingMember = outputShape.members().stream()
                .filter((m) -> m.getMemberTrait(model, StreamingTrait.class).isPresent())
                .findFirst().get();

        writer.write("data.$L = http_resp.body",
                symbolProvider.toMemberName(streamingMember)); // do NOT read the body when streaming
    }

    private class ParserClassGenerator extends ShapeVisitor.Default<Void> {
        @Override
        protected Void getDefault(Shape shape) {
            return null;
        }

        @Override
        public Void structureShape(StructureShape s) {
            writer
                    .write("")
                    .openBlock("class $T", symbolProvider.toSymbol(s))
                    .call(() -> renderStructureParseMethod(s))
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void listShape(ListShape s) {
            writer
                    .write("")
                    .openBlock("class $T", symbolProvider.toSymbol(s))
                    .call(() -> renderListParseMethod(s))
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void mapShape(MapShape s) {
            writer
                    .write("")
                    .openBlock("class $T", symbolProvider.toSymbol(s))
                    .call(() -> renderMapParseMethod(s))
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void unionShape(UnionShape s) {
            writer
                    .write("")
                    .openBlock("class $T", symbolProvider.toSymbol(s))
                    .call(() -> renderUnionParseMethod(s))
                    .closeBlock("end");

            return null;
        }
    }

}
