define [], () ->

  ###*
    @constant {Number} id приложения для социальной сети ВКонтакте
  ###
  API_ID = 4705589

  ###*
    Вызывает метод api
    @param {String} имя метода
    @param {Object} параметры вызова
    @param {Function} callback функция
  ###
  call: (name, params, callback) ->
    VK.Api.call name, params, callback

  ###*
    @callback отправляет данные об успешной иницализации модуля
  ###
  onSuccess: () ->
    console.log "all good"

  ###*
    @callback отправляет данные об ошибке инициализации модуля
    @param (Object) экземпляр webSocket для отправки результата на сервер
    @param (String) описание ошибки
  ###
  onError: (connection, reason) ->
    connection.send reason

  ###*
    Инициализация модуля для социальной сети ВКонтакте
  ###
  init: () ->
    #VK.init
    #  apiId: API_ID

    console.log "inti vk module"
