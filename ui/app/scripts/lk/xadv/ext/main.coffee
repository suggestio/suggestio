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
      return new PrepareEnsureServiceReadyBuilder(ws, serviceName, ctx1)

    @setService: (name, adapter) ->
      serviceList[name] = adapter

    @service: (name) ->
      return serviceList[name]

  class PrepareEnsureReadyBuilder

    constructor: (@ws) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureReadyBuilder execute"
      ctx1 = new Object()
      onSuccess @ws, ctx1

  class PrepareEnsureServiceReadyBuilder
    adapter = null

    constructor: (@ws, @name, @ctx1) ->
      adapter = new VkAdapter(@ws)
      SioPR.setService @name, adapter

    setServiceName: (name) ->
      @name = name
      return @

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureServiceReadyBuilder execute"
      ctx2 = new Object()
      #SioPR.service("vk").preparePublishMessageBuilder()
      onSuccess(@ws, ctx2)



  class IAdapter

    constructor: (@ws) ->

    @preparePublishMessageBuilder: (ctx0) ->
      return new IPublishMessageBuilder(ws, ctx0)

  class VkAdapter extends IAdapter
    API_ID = 4705589

    constructor: (@ws) ->
      VK.init
        apiId: API_ID

      authInfo = (response) ->
        if response.session
          userId = response.session.mid
          console.log "user_id = #{userId}"
        else
          #console.log "user not auth"
          VK.Auth.login authInfo, ACESS_LVL

      VK.Auth.getLoginStatus authInfo

    preparePublishMessageBuilder: (ctx0) ->
      console.log "prepare adapter for msg"
      return new VkPublishMessageBuilder(@ws, ctx0)



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

