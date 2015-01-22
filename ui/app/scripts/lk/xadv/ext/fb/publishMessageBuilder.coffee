define ["IPublishMessageBuilder"], (IPublishMessageBuilder) ->

  class FbPublishMessageBuilder extends IPublishMessageBuilder

    execute: (onSuccess, onError) ->
      console.log "FbPublishMessageBuilder execute"

      onSuccess @ws, @ctx