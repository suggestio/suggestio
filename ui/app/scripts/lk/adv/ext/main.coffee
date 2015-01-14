define [], () ->

  serviceList = new Array()
  ws = null

  ###*
    @callback отправляет на сервер сообщение об успешной инициализации
    @param (Object) экземпляр webSocket для отправки результата на сервер
  ###
  onSuccess: (connection) ->
    console.log "connection success"

  ###*
    @callback отправляет на сервер сообщение об ошибке инициализации
    @param (Object) экземпляр webSocket для отправки результата на сервер
    @param (String) описание ошибки
  ###
  onError: (connection, reason) ->
    console.log "connection error"

  ###*
    @callback обработка сообщения от сервера
    @param (Object) объект события, содержит сообщение сервера
  ###
  onMessage: (event) ->
    message = $.parseJSON event.data
    console.log message
    if message["type"] == "js"
      eval message["data"]

  ###*
    Получить модуль конкретной социальной сети
    @param (String) название модуля социальной сети
  ###
  initServiceByName: (name) ->
    require(
      [name]
      (service) ->
        serviceList[name] = service
        serviceList[name].init()
    )

  post: (serviceName, options) ->
    serviceList[serviceName].post options


  class PrepareEnsureReadyBuilder

    execute: (onSuccess, onError) ->
      onSuccess ws, {}
      console.log ws

  class PrepareEnsureServiceReadyBuilder

    @name = undefined

    setServiceName: (name) ->
      @name = name
      return @

    execute: (onSuccess, onError) ->
      onSuccess(ws, {})

  class SioPR
    instance = undefined

    constructor: ->
      if instance?
        return instance
      else instance = @

    @prepareEnsureReady: ()->
      return new PrepareEnsureReadyBuilder()

  ###*
    Инициализация social api
    @param (Json) контекст, в котором вызывается api
  ###
  init: () ->
    $input = $ "#socialApiConnection"
    url = $input.val()

    ws = new WebSocket url

    ws.onmessage = (event) =>
      message = $.parseJSON event.data
      console.log message
      if message["type"] == "js"
        eval message["data"]
