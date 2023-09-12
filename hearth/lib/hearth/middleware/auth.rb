# frozen_string_literal: true

module Hearth
  module Middleware
    # A middleware that resolves identities for signing requests.
    # @api private
    class Auth
      # @param [Class] app The next middleware in the stack.
      # @param [#resolve(auth_params)] auth_resolver A class that responds to a
      #  `resolve(auth_params)` method where `auth_params` is a struct with an
      #  operation_name. For a given operation_name, the method must return an
      #  ordered list of {Hearth::AuthOption} objects to be considered for
      #  authentication.
      # @param [Struct] auth_params A struct with an operation_name and other
      #  parameters that may be used to resolve auth options.
      # @param [Array<Hearth::AuthScheme::Base>] auth_schemes A list of
      #  auth schemes to consider for authentication.
      # @param [Hash<Symbol, Class>] identity_resolver_map A map of identity
      #  resolver config names to identity types.
      def initialize(app, auth_resolver:, auth_params:, auth_schemes:,
                     identity_resolver_map:, **kwargs)
        @app = app
        @auth_resolver = auth_resolver
        @auth_params = auth_params
        @auth_schemes = auth_schemes.to_h { |s| [s.scheme_id, s] }
        @identity_resolver_map = identity_resolver_map

        @identity_resolvers = {}
        kwargs.each do |key, value|
          next unless key.end_with?('_identity_resolver')

          type = @identity_resolver_map[key]
          raise "Unknown identity resolver type #{key}" unless type

          @identity_resolvers[type] = value
        end
      end

      # @param input
      # @param context
      # @return [Output]
      def call(input, context)
        auth_options = @auth_resolver.resolve(@auth_params)
        context.auth = resolve_auth(auth_options)
        @app.call(input, context)
      end

      private

      ResolvedAuth = Struct.new(
        :signer,
        :signer_properties,
        :identity,
        :identity_properties,
        keyword_init: true
      )

      def resolve_auth(auth_options)
        failures = []

        auth_options.each do |auth_option|
          auth_scheme = @auth_schemes[auth_option.scheme_id]
          resolved_auth = try_load_auth_scheme(
            auth_option,
            auth_scheme,
            failures
          )

          return resolved_auth if resolved_auth
        end

        raise failures.join("\n")
      end

      def try_load_auth_scheme(auth_option, auth_scheme, failures)
        scheme_id = auth_option.scheme_id
        unless auth_scheme
          failures << "Auth scheme #{scheme_id} was not enabled " \
                      'for this request'
          return
        end

        identity_resolver = auth_scheme.identity_resolver(@identity_resolvers)
        unless identity_resolver
          failures << "Auth scheme #{scheme_id} did not have an " \
                      'identity resolver configured'
          return
        end

        identity_properties = auth_option.identity_properties
        identity = identity_resolver.identity(identity_properties)

        ResolvedAuth.new(
          identity: identity,
          identity_properties: auth_option.identity_properties,
          signer: auth_scheme.signer,
          signer_properties: auth_option.signer_properties
        )
      end
    end
  end
end
