define [], () ->

  class IPublishMessageBuilder

    constructor: (@ws, @ctx) ->

    setUrl: (url) ->
      return @

    execute: (onSuccess, onError) ->
      onError @ws, "execute() not implemented!"