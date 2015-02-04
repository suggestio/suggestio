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
        ["vk", "facebook", "twitter"]
        () ->
          # регистрируем и инициализируем новые сервисы
          serviceList["count"] = arguments.length
          for index in [0...arguments.length]
            service = arguments[index]
            serviceList[service.serviceName] = new service(ws, ctx, onComplete)
      )

    registerService: (ctx, onComplete) ->
      regServiceCount += 1

      console.log "serviceList count = #{serviceList.count}"
      console.log "reg service count = #{regServiceCount}"

      if regServiceCount == serviceList.count
        ctx._status = "success"
        onComplete ctx, sendF

    handleTarget: (ctx, onComplete) ->
      console.log "--handleTarget---"
      console.log serviceList

      console.log ctx

      if ctx._domain.indexOf("facebook.com") >= 0
        serviceList["Facebook"].handleTarget(ctx, onComplete)

      if ctx._domain.indexOf("vk.com") >= 0
        serviceList["Vk"].handleTarget(ctx, onComplete)

      return true