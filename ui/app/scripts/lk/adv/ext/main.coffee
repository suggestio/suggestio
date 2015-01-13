define [], () ->

  ###*
    @callback отправляет на сервер сообщение об успешной инициализации
    @param (Object) экземпляр webSocket для отправки результата на сервер
    @param (Json) новое состояние системы
  ###
  onSuccess: (connection, context) ->
    message = JSON.stringify context
    connection.send message

  ###*
    @callback отправляет на сервер сообщение об ошибке инициализации
    @param (Object) экземпляр webSocket для отправки результата на сервер
    @param (String) описание ошибки
  ###
  onError: (connection, reason) ->
    connection.send reason

  ###*
    Получить модуль конкретной социальной сети
    @param (String) название модуля социальной сети
  ###
  getServiceByName: (name) ->
    require(
      [name]
      (service) ->
        console.log service.init()
    )

  ###*
    Инициализация social api
    @param (Json) контекст, в котором вызывается api
  ###
  init: () ->
    # TODO перенести context в параметры
    context =
      connectionUrl: "ws://example.org:12345/myapp"
      initializationStatus: false

    #connection = new WebSocket context.connectionUrl

    #connection.onopen = () =>
    #  context.initializationStatus = true
    #  @.onSuccess(connection, context)

    #connection.onerror = (error) =>
    #  @.onError(connection, error)

    #@.getServiceByName "vk"

    console.log "main module init332"
