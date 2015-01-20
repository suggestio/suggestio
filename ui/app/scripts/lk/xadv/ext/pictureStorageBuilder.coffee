define [], () ->

  class PictureStorageBuilder

    constructor: (@ws, @ctx) ->

    setName: (name) ->
      return @

    setDescription: (description) ->
      return @

    execute: () ->
      console.log "picture storage execute"