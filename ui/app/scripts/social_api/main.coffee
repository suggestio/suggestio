define [], () ->

  ###*
    Отправляет на сервер сообщение об успешной инициализации
    @param (Object) экземпляр webSocket для отправки результата на сервер
    @param (Json) новое состояние системы
  ###
  onSuccess: (connection, context) ->
    message = JSON.stringify context
    connection.send message

  ###*
    Отправляет на сервер сообщение об ошибке инициализации
    @param (Object) экземпляр webSocket для отправки результата на сервер
    @param (String) строковое описание ошибки
  ###
  onError: (connection, reason) ->
    connection.send reason

  ###*
    Инициализация social api
    @param (Json) контекст, в котором вызывается api
  ###
  # TODO перенести context в параметры
  context =
    connectionUrl: "ws://example.org:12345/myapp"
    initializationStatus: false

  prepareEnsureReady: () ->

    connection = new WebSocket context.connectionUrl

    connection.onopen = () =>
      context.initializationStatus = true
      @.onSuccess(connection, context)

    connection.onerror = (error) =>
      @.onError(connection, error)
