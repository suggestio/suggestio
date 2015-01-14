define ["main"], (Main) ->

  ###*
    @constant {Number} id приложения для социальной сети ВКонтакте
  ###
  API_ID = 4705589

  ###*
    @constant {Number} уровень прав доступа, необходимый для работы приложения
  ###
  ACCESS_LVL = 5

  # TODO временные константы
  ALBUM_ID = 209833141


  userId = null

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
    постит запись на стену
    @param (Object) параметры записи href - ссылка, image - параметры изображения, message - сообщение
  ###
  post: (options) ->

    attachments = new Array()
    if options.href
      attachments.push options.href
    if options.image
      attachments.push "photo#{options.image.owner_id}_#{options.image.id}"
    attachments.join ","

    params =
      message: options.message
      attachments: attachments

    callback = (response) ->
      console.log response

    @.call "wall.post", params, callback

  getWallUploadServer: () ->

    params =
      group_id: USER_ID

    callback = (data) ->
      options =
        url: data.response.upload_url
        image: image
      Main.uploadFile uploadUrl,

    @.call "photos.getWallUploadServer", params, callback


  setUserId: () ->

    authInfo = (response) ->
      if response.session
        userId = response.session.mid
      else
        #console.log "user not auth"
        VK.Auth.login authInfo, ACESS_LVL

    VK.Auth.getLoginStatus authInfo

  ###*
    Инициализация модуля для социальной сети ВКонтакте
  ###
  init: () ->
    console.log "init vk module"

    VK.init
      apiId: API_ID

    @.setUserId()