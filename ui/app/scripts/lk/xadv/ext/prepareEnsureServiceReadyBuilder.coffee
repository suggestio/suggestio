define [], () ->

 class PrepareEnsureServiceReadyBuilder

    constructor: (@ws, @name, @ctx) ->

    execute: (onSuccess, onError) ->
      onSuccess @ws, @ctx

