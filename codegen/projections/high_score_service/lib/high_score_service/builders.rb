# frozen_string_literal: true

# WARNING ABOUT GENERATED CODE
#
# This file was code generated using smithy-ruby.
# https://github.com/awslabs/smithy-ruby
#
# WARNING ABOUT GENERATED CODE

module HighScoreService
  # @api private
  module Builders

    class ApiKeyAuth
      def self.build(http_req, input:)
        http_req.http_method = 'GET'
        http_req.append_path('/api_key_auth')
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class BasicAuth
      def self.build(http_req, input:)
        http_req.http_method = 'GET'
        http_req.append_path('/basic_auth')
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class BearerAuth
      def self.build(http_req, input:)
        http_req.http_method = 'GET'
        http_req.append_path('/bearer_auth')
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class CreateHighScore
      def self.build(http_req, input:)
        http_req.http_method = 'POST'
        http_req.append_path('/high_scores')
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)

        http_req.headers['Content-Type'] = 'application/json'
        data = {}
        data[:high_score] = Builders::HighScoreParams.build(input[:high_score]) unless input[:high_score].nil?
        http_req.body = StringIO.new(Hearth::JSON.dump(data))
      end
    end

    class DeleteHighScore
      def self.build(http_req, input:)
        http_req.http_method = 'DELETE'
        if input[:id].to_s.empty?
          raise ArgumentError, "HTTP label :id cannot be empty."
        end
        http_req.append_path(format(
            '/high_scores/%<id>s',
            id: Hearth::HTTP.uri_escape(input[:id].to_s)
          )
        )
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class DigestAuth
      def self.build(http_req, input:)
        http_req.http_method = 'GET'
        http_req.append_path('/digest_auth')
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class GetHighScore
      def self.build(http_req, input:)
        http_req.http_method = 'GET'
        if input[:id].to_s.empty?
          raise ArgumentError, "HTTP label :id cannot be empty."
        end
        http_req.append_path(format(
            '/high_scores/%<id>s',
            id: Hearth::HTTP.uri_escape(input[:id].to_s)
          )
        )
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class HighScoreParams
      def self.build(input)
        data = {}
        data[:game] = input[:game] unless input[:game].nil?
        data[:score] = input[:score] unless input[:score].nil?
        data
      end
    end

    class ListHighScores
      def self.build(http_req, input:)
        http_req.http_method = 'GET'
        http_req.append_path('/high_scores')
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)
      end
    end

    class UpdateHighScore
      def self.build(http_req, input:)
        http_req.http_method = 'PUT'
        if input[:id].to_s.empty?
          raise ArgumentError, "HTTP label :id cannot be empty."
        end
        http_req.append_path(format(
            '/high_scores/%<id>s',
            id: Hearth::HTTP.uri_escape(input[:id].to_s)
          )
        )
        params = Hearth::Query::ParamList.new
        http_req.append_query_param_list(params)

        http_req.headers['Content-Type'] = 'application/json'
        data = {}
        data[:high_score] = Builders::HighScoreParams.build(input[:high_score]) unless input[:high_score].nil?
        http_req.body = StringIO.new(Hearth::JSON.dump(data))
      end
    end
  end
end
