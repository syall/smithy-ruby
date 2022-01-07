# frozen_string_literal: true

# WARNING ABOUT GENERATED CODE
#
# This file was code generated using smithy-ruby.
# https://github.com/awslabs/smithy-ruby
#
# WARNING ABOUT GENERATED CODE

module Weather
  module Params

    module GetCityAnnouncementsInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::GetCityAnnouncementsInput, context: context)
        type = Types::GetCityAnnouncementsInput.new
        type.city_id = params[:city_id]
        type
      end
    end

    module GetCityImageInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::GetCityImageInput, context: context)
        type = Types::GetCityImageInput.new
        type.city_id = params[:city_id]
        type.image_type = ImageType.build(params[:image_type], context: "#{context}[:image_type]") unless params[:image_type].nil?
        type
      end
    end

    module GetCityInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::GetCityInput, context: context)
        type = Types::GetCityInput.new
        type.city_id = params[:city_id]
        type
      end
    end

    module GetCurrentTimeInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::GetCurrentTimeInput, context: context)
        type = Types::GetCurrentTimeInput.new
        type
      end
    end

    module GetForecastInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::GetForecastInput, context: context)
        type = Types::GetForecastInput.new
        type.city_id = params[:city_id]
        type
      end
    end

    module ImageType
      def self.build(params, context: '')
        return params if params.is_a?(Types::ImageType)
        Seahorse::Validator.validate!(params, ::Hash, Types::ImageType, context: context)
        unless params.size == 1
          raise ArgumentError,
                "Expected #{context} to have exactly one member, got: #{params}"
        end
        key, value = params.flatten
        case key
        when :raw
          Types::ImageType::Raw.new(
            params[:raw]
          )
        when :png
          Types::ImageType::Png.new(
            PNGImage.build(params[:png], context: "#{context}[:png]")
          )
        else
          raise ArgumentError,
                "Expected #{context} to have one of :raw, :png set"
        end
      end
    end

    module ListCitiesInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::ListCitiesInput, context: context)
        type = Types::ListCitiesInput.new
        type.next_token = params[:next_token]
        type.a_string = params[:a_string]
        type.default_bool = params[:default_bool]
        type.boxed_bool = params[:boxed_bool]
        type.default_number = params[:default_number]
        type.boxed_number = params[:boxed_number]
        type.some_enum = params[:some_enum]
        type.page_size = params[:page_size]
        type
      end
    end

    module PNGImage
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::PNGImage, context: context)
        type = Types::PNGImage.new
        type.height = params[:height]
        type.width = params[:width]
        type
      end
    end

    module Struct__456efg
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::Struct__456efg, context: context)
        type = Types::Struct__456efg.new
        type.member___123foo = params[:member___123foo]
        type
      end
    end

    module Struct__789BadNameInput
      def self.build(params, context: '')
        Seahorse::Validator.validate!(params, ::Hash, Types::Struct__789BadNameInput, context: context)
        type = Types::Struct__789BadNameInput.new
        type.member___123abc = params[:member___123abc]
        type.member = Struct__456efg.build(params[:member], context: "#{context}[:member]") unless params[:member].nil?
        type
      end
    end

  end
end
