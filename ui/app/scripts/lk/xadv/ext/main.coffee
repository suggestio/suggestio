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
        () ->
          # регистрируем и инициализируем новые сервисы
          for index in [0...arguments.length]
            service = arguments[index]

            serviceList[service.name] = new service()
            serviceList[service.name].init()

            if index == arguments.length - 1
              ctx._status = "success"
              onComplete ctx, sendF
      )

    handleTarget: (ctx, onComplete) ->
      console.log "handleTarget"

      #serviceList["vk"].handleTarget ctx, OnComplete
