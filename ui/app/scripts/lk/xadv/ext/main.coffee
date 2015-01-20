define [], () ->

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
      return new PrepareEnsureServiceReadyBuilder(ws, serviceName, ctx)

    setService: (name, adapter) ->
      serviceList[name] = adapter

    service: (name) ->
      return serviceList[name]


  class PrepareEnsureReadyBuilder

    constructor: (@ws, @ctx) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureReadyBuilder execute"
      onSuccess @ws, @ctx


  class PrepareEnsureServiceReadyBuilder

    constructor: (@ws, @name, @ctx) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureServiceReadyBuilder execute"

      requirejs(
        [@name]
        (VkAdapter) =>
          adapter = new VkAdapter(@ws)
          SioPR = new SioPR()

          SioPR.setService @name, adapter
          onSuccess @ws, @ctx
      )


  return SioPR
