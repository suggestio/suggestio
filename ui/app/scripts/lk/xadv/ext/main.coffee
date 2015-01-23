define [], () ->

  class SioPR
    instance = undefined
    ws = null
    serviceList = new Array()
    regServiceCount = 0

    sendF = (json) ->
      message = JSON.stringify json
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
        () ->
          # регистрируем и инициализируем новые сервисы
          for index in [0...arguments.length]
            service = arguments[index]

            serviceList[service.name] = new service(ws, ctx, onComplete)
      )

    registerService: (ctx, onComplete) ->
      regServiceCount += 1

      if regServiceCount == Object.keys(serviceList).length
        ctx._status = "success"
        onComplete ctx, sendF

    handleTarget: (ctx, onComplete) ->
      serviceList["Vk"].handleTarget(ctx, onComplete)

      #serviceList["vk"].handleTarget ctx, OnComplete
