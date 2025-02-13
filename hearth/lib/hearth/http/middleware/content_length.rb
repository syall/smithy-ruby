# frozen_string_literal: true

module Hearth
  module HTTP
    module Middleware
      # A middleware that sets Content-Length for any body that has a size.
      # @api private
      class ContentLength
        def initialize(app, _ = {})
          @app = app
        end

        # @param input
        # @param context
        # @return [Output]
        def call(input, context)
          request = context.request
          if request.body.respond_to?(:size) &&
             !request.headers.key?('Content-Length')
            context.logger.debug(
              '[HTTP::Middleware::ContentLength] ' \
              'Started setting Content-Length'
            )
            length = request.body.size
            request.headers['Content-Length'] = length
            context.logger.debug(
              '[HTTP::Middleware::ContentLength] ' \
              'Finished setting Content-Length'
            )
          end

          @app.call(input, context)
        end
      end
    end
  end
end
