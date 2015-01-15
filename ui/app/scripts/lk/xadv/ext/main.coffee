define [], () ->

  class SioPR
    instance = undefined
    ws = null
    serviceList = new Array()

    constructor: ->
      if instance?
        return instance
      else instance = @

    @setWs: (_ws) ->
      ws = _ws

    @prepareEnsureReady: () ->
      return new PrepareEnsureReadyBuilder(ws)

    @prepareEnsureServiceReady: (serviceName, ctx1) ->
      newService = new PrepareEnsureServiceReadyBuilder(ws, serviceName, ctx1)
      serviceList[serviceName] = newService
      return newService

    service: (name) ->
      return serviceList[name]

  class PrepareEnsureReadyBuilder

    constructor: (@ws) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureReadyBuilder execute"
      testObject = new Object()
      onSuccess @ws, testObject

  class PrepareEnsureServiceReadyBuilder

    constructor: (@ws, @name, @ctx1) ->

    setServiceName: (name) ->
      @name = name
      return @

    initService: () ->

      if @name == "vk"
        VK_API_ID = 4705589
        VK.init
          apiId: VK_API_ID

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureServiceReadyBuilder execute"
      @initService()
      onSuccess(@ws, {})



  class IAdapter

    constructor: (@ws) ->

    preparePublishMessageBuilder: (ctx0) ->
      return new IPublishMessageBuilder(ws, ctx0)

  class VkAdapter extends IAdapter

    preparePublishMessageBuilder: (ctx0) ->
      return new VkPublishMessageBuilder(ws, ctx0)



  #  Абстрактный класс-заглушка билдеров запросов отправки сообщений на стену.
  class IPublishMessageBuilder

    constructor: (@ws, @ctx0) ->

    setUrl: (url) ->
      return @

    execute: (onSuccess, onError) ->
      onError(ws, "execute() not implemented!")

  #  Реализация адаптера IAdapter в рамках некоторой абстрактной соц.сети
  class VkPublishMessageBuilder extends IPublishMessageBuilder
    url = null
    message = null

    setUrl: (_url) ->
      url = _url
      return @

    setMessage: (_message) ->
      message = _message
      return @

    execute: (onSuccess, onError) ->
      params =
        message: message

      VK.Api.call "wall.post", params, onSuccess


  ###*
    Инициализация social api
    @param (Json) контекст, в котором вызывается api
  ###
  init: () ->
    $input = $ "#socialApiConnection"
    url = $input.val()

    ws = new WebSocket url

    SioPR.setWs ws

    ws.onmessage = (event) =>
      message = $.parseJSON event.data
      console.log message
      if message["type"] == "js"
        eval message["data"]

