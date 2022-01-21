/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.ruby.codegen.GenerationContext;
import software.amazon.smithy.ruby.codegen.RubyCodeWriter;
import software.amazon.smithy.ruby.codegen.RubySettings;
import software.amazon.smithy.ruby.codegen.RubySymbolProvider;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Base class for Parsers for HTTP based protocols.
 *
 * Protocols should extend this class to get common functionality -
 * generates the framework and non-protocol specific parts of
 * parsers.rb.
 */
@SmithyUnstableApi
public abstract class HttpParserGeneratorBase {

    private static final Logger LOGGER =
            Logger.getLogger(HttpParserGeneratorBase.class.getName());

    protected final GenerationContext context;
    protected final RubySettings settings;
    protected final Model model;
    protected final Set<ShapeId> generatedParsers;
    protected final SymbolProvider symbolProvider;

    protected final RubyCodeWriter writer;

    public HttpParserGeneratorBase(GenerationContext context) {
        this.context = context;
        this.settings = context.getRubySettings();
        this.model = context.getModel();
        this.generatedParsers = new HashSet<>();
        this.writer = new RubyCodeWriter();
        this.symbolProvider = new RubySymbolProvider(model, settings, "Params", true);
    }

    /**
     * Called to render an operation's body parser when it has a Payload member.
     * The generated code should deserialize the payloadMember from the
     * response body. The skeleton creates a data variable of the Output type.
     * The payload should be deserialzed and its values set onto data.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class HttpPayloadTraits
     *   def self.parse(http_resp)
     *     data = Types::HttpPayloadTraitsOutput.new
     *     #### START code generated by this method
     *     payload = http_resp.body.read
     *     data.blob = payload unless payload.empty?
     *     #### END code generated by this method
     *     data
     *   end
     * end
     * }</pre>
     *
     * @param outputShape the operation's outputShape
     * @param payloadMember the payload member
     * @param target the target shape of the paylaod member
     */
    protected abstract void renderPayloadBodyParser(Shape outputShape, MemberShape payloadMember, Shape target);

    /**
     * Called to render an operation's body parser when it does not have a payload member.
     * The skeleton creates a data variable of the output type.
     * The generated code should deserialize all of the appropriate outputShape's members
     * to the data object.  The parsed response body is available in map as a ruby hash.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class JsonEnums
     *   def self.parse(http_resp)
     *     data = Types::JsonEnumsOutput.new
     *     map = Seahorse::JSON.load(http_resp.body)
     *     #### START code generated by this method
     *     data.foo_enum1 = map['foo_enum1']
     *     #### END code generated by this method
     *     data
     *   end
     * end
     * }</pre>
     * @param outputShape the operation's outputShape
     */
    protected abstract void renderNoPayloadBodyParser(Shape outputShape);

    /**
     *  Called to render the union's member parser.  The class and parse method skeleton
     *  are rendered outside of this method and implementations only need to deserialize the member
     *  to the value variable.  The rendered code should return the correct member Type class.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class MyUnion
     *   def self.parse(map)
     *     key, value = map.flatten
     *     case key
     *     when 'string_value'
     *       #### START code generated by this method
     *       Types::MyUnion::StringValue.new(value) if value
     *       ### END code generated by this method
     *     else
     *       Types::MyUnion::Unknown.new({name: key, value: value})
     *     end
     *   end
     * end
     * }</pre>
     * @param s the union shape
     * @param member the union's member shape
     */
    protected abstract void renderUnionMemberParser(UnionShape s, MemberShape member);

    /**
     * Called to get the protocol specific name of the union member in the response.
     *
     * @param s the union shape
     * @param member the union's member shape
     * @return name of the union member in the response
     */
    protected abstract String unionMemberDataName(UnionShape s, MemberShape member);

    /**
     *  Called to render a map's member parser.  The class and parse method skeleton
     *  are rendered outside of this method and implementations only need to deserialize the member
     *  to the value variable.  The rendered code should set the deserialzed member onto the data hash.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class StringMap
     *   def self.parse(map)
     *     data = {}
     *     map.map do |key, value|
     *       #### START code generated by this method
     *       data[key] = value
     *       #### END code generated by this method
     *     end
     *     data
     *   end
     * end
     * }</pre>
     * @param s shape to generate for
     */
    protected abstract void renderMapMemberParser(MapShape s);

    /**
     *  Called to render a sets's member parser.  The class and parse method skeleton
     *  are rendered outside of this method and implementations only need to deserialize the member.
     *  The rendered code should return the deserialized value.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class StringSet
     *   def self.parse(list)
     *     data = list.map do |value|
     *       #### START code generated by this method
     *       value
     *       #### END code generated by this method
     *     end
     *     Set.new(data)
     *   end
     * end
     * }</pre>
     * @param s shape to generate for
     */
    protected abstract void renderSetMemberParser(SetShape s);

    /**
     *  Called to render a list's member parser.  The class and parse method skeleton
     *  are rendered outside of this method and implementations only need to deserialize the member.
     *  The rendered code should return the deserialized value.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class StringList
     *   def self.parse(list)
     *     list.map do |value|
     *       #### START code generated by this method
     *       value
     *       #### END code generated by this method
     *     end
     *   end
     * end
     * }</pre>
     * @param s shape to generate for
     */
    protected abstract void renderListMemberParser(ListShape s);

    /**
     *  Called to render a structure's member parsers.  The class and parse method skeleton
     *  are rendered outside of this method and implementations only need to deserialize the members.
     *  The rendered code should set the deserialized values onto the data variable.
     *
     * <p>The following example shows the generated skeleton and an example of what
     * this method is expected to render.</p>
     * <pre>{@code
     * class SimpleStruct
     *   def self.parse(map)
     *     data = Types::SimpleStruct.new
     *     ### START code generated by this method
     *     data.value = map['value']
     *     data.timestamp = Time.parse(map['timestamp']) if map['timestamp']
     *     #### END code generated by this method
     *     return data
     *   end
     * end
     * }</pre>
     * @param s shape to generate for
     */
    protected abstract void renderStructureMemberParsers(StructureShape s);

    public void render(FileManifest fileManifest) {
        writer
                .writePreamble()
                .write("require 'base64'\n")
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
                topDownIndex.getContainedOperations(context.getService()));
        containedOperations.stream()
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
                .openBlock("def self.parse(http_resp)")
                .write("data = Types::$L.new", symbolProvider.toSymbol(outputShape).getName())
                .call(() -> renderHeaderParsers(outputShape))
                .call(() -> renderPrefixHeaderParsers(outputShape))
                .call(() -> renderResponseCodeParser(outputShape))
                .call(() -> renderOperationBodyParser(outputShape))
                .write("data")
                .closeBlock("end")
                .closeBlock("end");
        LOGGER.finer("Generated parser class for " + operation.getId().getName());
    }

    protected void renderErrorParser(Shape s) {
        writer
                .write("")
                .write("# Error Parser for $L", s.getId().getName())
                .openBlock("class $L", symbolProvider.toSymbol(s).getName())
                .openBlock("def self.parse(http_resp)")
                .write("data = Types::$L.new", symbolProvider.toSymbol(s).getName())
                .call(() -> renderHeaderParsers(s))
                .call(() -> renderPrefixHeaderParsers(s))
                .call(() -> renderOperationBodyParser(s))
                .write("data")
                .closeBlock("end")
                .closeBlock("end");
        LOGGER.finer("Generated Error parser for " + s.getId().getName());
    }

    protected void renderHeaderParsers(Shape outputShape) {
        List<MemberShape> headerMembers = outputShape.members()
                .stream()
                .filter((m) -> m.hasTrait(HttpHeaderTrait.class))
                .collect(Collectors.toList());

        for (MemberShape m : headerMembers) {
            HttpHeaderTrait headerTrait = m.expectTrait(HttpHeaderTrait.class);
            String symbolName = symbolProvider.toMemberName(m);
            String dataSetter = "data." + symbolName + " = ";
            String valueGetter = "http_resp.headers['" + headerTrait.getValue() + "']";
            model.expectShape(m.getTarget()).accept(new HeaderDeserializer(m, dataSetter, valueGetter));
            LOGGER.finest("Generated header parser for " + m.getMemberName());
        }
    }

    protected void renderPrefixHeaderParsers(Shape outputShape) {
        List<MemberShape> headerMembers = outputShape.members()
                .stream()
                .filter((m) -> m.hasTrait(HttpPrefixHeadersTrait.class))
                .collect(Collectors.toList());

        for (MemberShape m : headerMembers) {
            HttpPrefixHeadersTrait headerTrait = m.expectTrait(HttpPrefixHeadersTrait.class);
            String prefix = headerTrait.getValue();
            // httpPrefixHeaders may only target map shapes
            MapShape targetShape = model.expectShape(m.getTarget(), MapShape.class);
            Shape valueShape = model.expectShape(targetShape.getValue().getTarget());
            String symbolName = symbolProvider.toMemberName(m);

            String dataSetter = "data." + symbolName + "[key.delete_prefix('" + prefix + "')] = ";
            writer
                    .write("data.$L = {}", symbolName)
                    .openBlock("http_resp.headers.each do |key, value|")
                    .openBlock("if key.start_with?('$L')", prefix)
                    .call(() -> valueShape.accept(new HeaderDeserializer(m, dataSetter, "value")))
                    .closeBlock("end")
                    .closeBlock("end");
            LOGGER.finest("Generated prefix header parser for " + m.getMemberName());

        }
    }

    protected void renderResponseCodeParser(Shape outputShape) {
        List<MemberShape> responseCodeMembers = outputShape.members()
                .stream()
                .filter((m) -> m.hasTrait(HttpResponseCodeTrait.class))
                .collect(Collectors.toList());

        if (responseCodeMembers.size() == 1) {
            MemberShape responseCodeMember = responseCodeMembers.get(0);
            writer.write("data.$L = http_resp.status", symbolProvider.toMemberName(responseCodeMember));
            LOGGER.finest("Generated response code parser for " + responseCodeMember.getMemberName());
        }
    }

    // The Output shape is combined with the Operation Parser
    // This generates the parsing of the body as if it was the Parser for the Out[put
    protected void renderOperationBodyParser(Shape outputShape) {
        //determine if there is an httpPayload member
        List<MemberShape> httpPayloadMembers = outputShape.members()
                .stream()
                .filter((m) -> m.hasTrait(HttpPayloadTrait.class))
                .collect(Collectors.toList());

        if (httpPayloadMembers.size() == 0) {
            renderNoPayloadBodyParser(outputShape);
        } else if (httpPayloadMembers.size() == 1) {
            MemberShape payloadMember = httpPayloadMembers.get(0);
            Shape target = model.expectShape(payloadMember.getTarget());
            renderPayloadBodyParser(outputShape, payloadMember, target);
        }
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
                    .openBlock("class $L", symbolProvider.toSymbol(s).getName())
                    .openBlock("def self.parse(map)")
                    .write("data = Types::$L.new", symbolProvider.toSymbol(s).getName())
                    .call(() -> renderStructureMemberParsers(s))
                    .write("return data")
                    .closeBlock("end")
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void listShape(ListShape s) {
            writer
                    .write("")
                    .openBlock("class $L", symbolProvider.toSymbol(s).getName())
                    .openBlock("def self.parse(list)")
                    .openBlock("list.map do |value|")
                    .call(() -> renderListMemberParser(s))
                    .closeBlock("end")
                    .closeBlock("end")
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void setShape(SetShape s) {
            writer
                    .write("")
                    .openBlock("class $L", symbolProvider.toSymbol(s).getName())
                    .openBlock("def self.parse(list)")
                    .openBlock("data = list.map do |value|")
                    .call(() -> renderSetMemberParser(s))
                    .closeBlock("end")
                    .write("Set.new(data)")
                    .closeBlock("end")
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void mapShape(MapShape s) {
            writer
                    .write("")
                    .openBlock("class $L", symbolProvider.toSymbol(s).getName())
                    .openBlock("def self.parse(map)")
                    .write("data = {}")
                    .openBlock("map.map do |key, value|")
                    .call(() -> renderMapMemberParser(s))
                    .closeBlock("end")
                    .write("data")
                    .closeBlock("end")
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void unionShape(UnionShape s) {
            writer
                    .write("")
                    .openBlock("class $L", symbolProvider.toSymbol(s).getName())
                    .openBlock("def self.parse(map)")
                    .write("key, value = map.flatten")
                    .write("case key")
                    .call(() -> {
                        s.members().forEach((member) -> {
                            writer
                                    .write("when '$L'", unionMemberDataName(s, member))
                                    .indent()
                                    .call(() -> {
                                        renderUnionMemberParser(s, member);
                                    })
                                    .write("Types::$L::$L.new(value) if value", symbolProvider.toSymbol(s).getName(),
                                            symbolProvider.toMemberName(member))
                                    .dedent();
                        });
                    })
                    .openBlock("else")
                    .write("Types::$L::Unknown.new({name: key, value: value})", s.getId().getName())
                    .closeBlock("end") // end of case
                    .closeBlock("end")
                    .closeBlock("end");

            return null;
        }
    }

    private class HeaderDeserializer extends ShapeVisitor.Default<Void> {

        private final String valueGetter;
        private final String dataSetter;
        private final MemberShape memberShape;

        HeaderDeserializer(MemberShape memberShape,
                           String dataSetter, String valueGetter) {
            this.valueGetter = valueGetter;
            this.dataSetter = dataSetter;
            this.memberShape = memberShape;
        }

        /**
         * For simple shapes, just copy to the data.
         */
        @Override
        protected Void getDefault(Shape shape) {
            writer.write("$L$L", dataSetter, valueGetter);
            return null;
        }

        private void rubyFloat() {
            writer.write("$1LSeahorse::NumberHelper.deserialize($2L) unless $2L.nil?", dataSetter, valueGetter);
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            rubyFloat();
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            rubyFloat();
            return null;
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            writer.write("$1L$2L == 'true' unless $2L.nil?", dataSetter, valueGetter);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            writer.write("$1L$2L&.to_i", dataSetter, valueGetter);
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            writer.write("$1L$2L&.to_i", dataSetter, valueGetter);
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            writer.write("$1L$2L&.to_i", dataSetter, valueGetter);
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            writer.write("$1L$2L&.to_i", dataSetter, valueGetter);
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            // string values with a mediaType trait are always base64 encoded.
            if (shape.hasTrait(MediaTypeTrait.class)) {
                writer.write("$1LBase64::decode64($2L).strip unless $2L.nil?", dataSetter, valueGetter);
            } else {
                writer.write("$1L$2L", dataSetter, valueGetter);
            }
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            // the default protocol format is date_time, which is parsed by Time.parse
            Optional<TimestampFormatTrait> format = memberShape.getTrait(TimestampFormatTrait.class);
            if (!format.isPresent()) {
                format = shape.getTrait(TimestampFormatTrait.class);
            }
            if (format.isPresent()) {
                switch (format.get().getFormat()) {
                    case EPOCH_SECONDS:
                        writer.write("$1LTime.at($2L.to_i) if $2L", dataSetter, valueGetter);
                        break;
                    case HTTP_DATE:
                    case DATE_TIME:
                    default:
                        writer.write("$1LTime.parse($2L) if $2L", dataSetter, valueGetter);
                        break;
                }
            } else {
                writer.write("$1LTime.parse($2L) if $2L", dataSetter, valueGetter);
            }
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.openBlock("unless $1L.nil? || $1L.empty?", valueGetter)
                    .write("$1L$2L", dataSetter, valueGetter)
                    .indent()
                    .write(".split(', ')")
                    .call(() -> model.expectShape(shape.getMember().getTarget())
                            .accept(new HeaderListMemberDeserializer(shape.getMember())))
                    .dedent()
                    .closeBlock("end");

            return null;
        }

        @Override
        public Void setShape(SetShape shape) {
            writer.openBlock("unless $1L.nil? || $1L.empty?", valueGetter)
                    .write("$1LSet.new($2L", dataSetter, valueGetter)
                    .indent()
                    .write(".split(', ')")
                    .call(() -> model.expectShape(shape.getMember().getTarget())
                            .accept(new HeaderListMemberDeserializer(shape.getMember())))
                    .dedent()
                    .write(")")
                    .closeBlock("end");

            return null;
        }

    }

    private class HeaderListMemberDeserializer extends ShapeVisitor.Default<Void> {

        private final MemberShape memberShape;

        HeaderListMemberDeserializer(MemberShape memberShape) {
            this.memberShape = memberShape;
        }

        @Override
        protected Void getDefault(Shape shape) {
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            // TODO: there is likely some stripping of extra quotes. Pending SEP definition
            writer.write(".map { |s| s.to_s }");
            return null;
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            writer.write(".map { |s| s == 'true' }");
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            writer.write(".map { |s| s.to_i }");
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            // header values are serialized using the http-date format by default
            Optional<TimestampFormatTrait> format = memberShape.getTrait(TimestampFormatTrait.class);
            if (format.isPresent()) {
                switch (format.get().getFormat()) {
                    case EPOCH_SECONDS:
                        writer.write(".map { |s| Time.at(s.to_i) }");
                        break;
                    case DATE_TIME:
                    case HTTP_DATE:
                    default:
                        writer.write(".map { |s| Time.parse(s) }");
                        break;
                }
            } else {
                writer.write(".map { |s| Time.parse(s) }");
            }
            return null;
        }
    }

}
