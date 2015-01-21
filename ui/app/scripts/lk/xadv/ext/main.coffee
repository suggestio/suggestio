define ["VkPrepareEnsureServiceReadyBuilder"], (VkPrepareEnsureServiceReadyBuilder) ->

  class SioPR
    instance = undefined
    ws = null
    serviceList = new Array()

    # singletone
    constructor: ->
      if instance?
        return instance
      else instance = @

    setWs: (_ws) ->
      ws = _ws

    prepareEnsureReady: (ctx) ->
      return new PrepareEnsureReadyBuilder(ws, ctx)

    prepareEnsureServiceReady: (serviceName, ctx) ->
      console.log VkPrepareEnsureServiceReadyBuilder
      return new VkPrepareEnsureServiceReadyBuilder(ws, serviceName, ctx)

    registerService: (name, adapter) ->
      serviceList[name] = adapter

    service: (name) ->
      return serviceList[name]


  class PrepareEnsureReadyBuilder

    constructor: (@ws, @ctx) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureReadyBuilder execute"
      onSuccess @ws, @ctx


  return SioPR
