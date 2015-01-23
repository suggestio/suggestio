define ["VkPrepareEnsureServiceReadyBuilder", "FbPrepareEnsureServiceReadyBuilder"], (VkPrepareEnsureServiceReadyBuilder, FbPrepareEnsureServiceReadyBuilder) ->

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

    ensureReady: (ctx, onComplete) ->
      sendF = (json) ->
        message = JSON.stringify(json)
        console.log message
        ws.send message

      onComplete ctx, sendF

    prepareEnsureServiceReady: (serviceName, ctx) ->
      if serviceName == "vk"
        return new VkPrepareEnsureServiceReadyBuilder(ws, serviceName, ctx)

      if serviceName == "fb"
        return new FbPrepareEnsureServiceReadyBuilder(ws, serviceName, ctx)

    registerService: (name, adapter) ->
      serviceList[name] = adapter

    service: (name) ->
      return serviceList[name]


  class PrepareEnsureReadyBuilder

    constructor: (@ws, @ctx) ->

    execute: (onComplete) ->
      console.log "PrepareEnsureReadyBuilder execute"
      onComplete @ws, @ctx


  return SioPR
