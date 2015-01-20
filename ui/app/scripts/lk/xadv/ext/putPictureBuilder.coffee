define [], () ->

  class PutPictureBuilder

    constructor: (@ctx) ->

    setPictureUrl: (url) ->
      return @

    setDescription: (description) ->
      return @

    execute: (onSucess, onError) ->
      onSuccess()