define ["SioPR"], (SioPR) ->

  SioPR = new SioPR()

  class Vk
    ACESS_LVL = 8197

    @serviceName: "Vk"

    constructor: (@ws, @ctx, @onComplete) ->
      console.log "vk init"

      window.vkAsyncInit = () =>

        try
          VK.init
            apiId: @ctx._service.appId
        catch error
          @ctx._status = "error"
          @complete()

        SioPR.registerService @ctx, @onComplete

      @loadSdk()

    ###*
      отправляет сообщения на сервер
    ###
    complete: ()->

      sendF = (json) =>
        message = JSON.stringify json
        @ws.send message

      @onComplete @ctx, sendF

    checkGroup: (onSuccess) ->
      params =
        group_id: @ctx.user_id
        fields: "can_post"

      callback = (data) =>
        console.log "check group callback"
        console.log data
        try
          canPost = data.response[0].can_post
          if canPost == 1
            onSuccess()
          else
            @ctx._status = "error"
            @ctx._error =
              msg: "e.ext.adv.permissions.group"
              args: ["#{data.response[0].name}"]

            @complete()
        catch error
          @ctx._status = "error"
          @complete()

      VK.Api.call "groups.getById", params, callback

    ###*
      записывает в контекст userId или groupId на стену, которого будет размещена запись
      @param {Object} callback функция, которая выполняется при успешном завершении
    ###
    setOwnerId: (onSuccess) ->
      REGEXP  = /// /(?!.+/)(.+)$ ///
      url     = @ctx._target.url

      match = url.match REGEXP

      callback = (data) =>
        try
          @ctx.user_id = data.response.object_id
          if data.response.type == "group"
            @ctx.is_group = true
            @checkGroup onSuccess
            return false
        catch error
          console.log error

        onSuccess()

      # если в url была какая-то некорректная строка, возвращается status error
      try
        params =
          screen_name: match[1]
      catch error
        @ctx.__status = "error"
        @ctx.__error = "e.ext.adv.target.url.invalid"
        @complete()
        return false

      VK.Api.call "utils.resolveScreenName", params, callback

    ###*
      сохраняет фотографию на стену
      @param {Object} информация о сохраняемой фотографии
    ###
    saveWallPhoto: (savedPicture) ->

      console.log "save wall photo"
      if !@ctx.user_id?
        onSuccess = () =>
          @saveWallPhoto(savedPicture)

        @login onSuccess
        return false

      savedPicture = JSON.parse savedPicture

      callback = (data) =>
        attachments = new Array()
        try
          attachments.push data.response[0].id
          attachments.push @ctx._target.href
          attachments = attachments.join ","

          @wallPost attachments
        catch exception
          console.log exception
          @ctx._status = "error"
          @complete()

      params =
        user_id: @ctx.user_id
        server: savedPicture.server
        photo: savedPicture.photo
        hash: savedPicture.hash

      VK.Api.call "photos.saveWallPhoto", params, callback

    ###*
      создаёт пост на стене
      @param {Object} media файлы, ссылки и т.д.
    ###
    wallPost: (attachments) ->
      post = new Object()

      if @ctx.is_group
        ownerId = "-#{@ctx.user_id}"
      else
        ownerId = @ctx.user_id

      post.owner_id = ownerId
      post.attachments = attachments

      post.message = @ctx._ads[0].content.descr || " "

      callback = (data) =>
        if data.error
          @ctx._status = "error"
          @ctx._error = data.error
        else
          @ctx._status = "success"

        @complete()

      VK.Api.call "wall.post", post, callback

    ###*
      получает url сервера для загрузки фотографии на сервер vk
    ###
    getWallUploadServer: () ->

      if !@ctx.user_id?
        onSuccess = () =>
          @getWallUploadServer()

        @login onSuccess
        return false

      params =
        user_id: @ctx.user_id

      callback = (data) =>
        try
          @ctx._status = "fillCtx"
          @ctx._ads[0].rendered.upload =
            mode: "s2s"
            url: data.response.upload_url
            partName: "photo"
        catch error
          @ctx._status = "error"
          @ctx._error =
            msg: error

        @complete()

      VK.Api.call "photos.getWallUploadServer", params, callback

    ###*
      проверяет авторизован ли пользователь, если нет выдаёт окно авторизации
      @param {Object} callback функция, выполняется при успешном завершении
    ###
    login: (onSuccess) ->

      loginCallback = (response) =>
        if response.session
          @setOwnerId onSuccess
        else
          @ctx._status = "error"
          @ctx._error =
            msg: "e.ext.adv.unathorized"
            info: response

          @complete()

      getLoginStatusCallback = (response) =>
        if response.session
          @setOwnerId onSuccess
        else
          VK.Auth.login loginCallback, ACESS_LVL

      VK.Auth.getLoginStatus getLoginStatusCallback

    ###*
      на основе контекста от сервера принимает решение о действиях, которые будут выполнены на этом шаге
    ###
    handleTarget: (ctx, onComplete) ->
      console.log "vk handle target"

      @ctx = ctx
      @onComplete = onComplete

      # фотография уже загружена на сервер vk
      if ctx._ads[0].rendered.saved
        @saveWallPhoto ctx._ads[0].rendered.saved
        return false

      if ctx._ads[0].rendered.sioUrl
        @getWallUploadServer()

    ###*
      загружает vk api
    ###
    loadSdk: () ->
      vkApiTransport = document.createElement "div"
      vkApiTransport.id = "vk_api_transport"

      body = document.getElementsByTagName("body")[0]
      body.appendChild vkApiTransport

      setTimeout(
        () ->
          el = document.createElement "script"
          el.type = "text/javascript"
          el.src = "//vk.com/js/api/openapi.js"
          el.async = true
          document.getElementById("vk_api_transport").appendChild(el)
        0
      )
