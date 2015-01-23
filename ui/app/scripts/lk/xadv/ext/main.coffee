define [], () ->

  class SioPR
    instance = undefined
    ws = null
    serviceList = new Array()

    sendF = (json) ->
      message = JSON.stringify(json)
      console.log message
      ws.send message

    # singletone
    constructor: ->
      if instance?
        return instance
      else instance = @

    setWs: (_ws) ->
      ws = _ws

    ensureReady: (ctx, onComplete) ->
      console.log "ensureReady"

      requirejs(
        ["facebook", "vk"]
        (Facebook, Vk) ->
          serviceList["facebook"] = new Facebook()
          serviceList["facebook"].init()

          serviceList["vk"] = new Vk()
          serviceList["vk"].init()
      )

      ctx._status = "success"
      onComplete ctx, sendF

    handleTarget: (ctx, onComplete) ->
      console.log "handleTarget"

      #serviceList["vk"].handleTarget ctx, OnComplete


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
