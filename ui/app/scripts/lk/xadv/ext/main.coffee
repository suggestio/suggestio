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
      console.log "reg service"
      regServiceCount += 1
      #console.log regServiceCount
      #console.log Object.keys(serviceList).length

      if regServiceCount == Object.keys(serviceList).length
        console.log "success"
        ctx._status = "success"
        onComplete ctx, sendF

    handleTarget: (ctx, onComplete) ->
      console.log "handleTarget"

      serviceList["Vk"].handleTarget(ctx)

      #serviceList["vk"].handleTarget ctx, OnComplete
