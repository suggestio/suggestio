define ["SioPR"], (SioPR) ->

  SioPR = new SioPR()

  class Vk
    API_ID = 4705589
    ACESS_LVL = 8197

    userId = null
    post = new Object()

    constructor: (@ws, @ctx, @onComplete) ->
      console.log "vk init"

      window.vkAsyncInit = () =>
        VK.init
          apiId: API_ID

        SioPR.registerService @ctx, @onComplete

      @loadSdk()

    sendF = (json) ->
      message = JSON.stringify json
      @ws.send message

    setText: (text) ->
      post.message = text
      return @

    setOwnerId: () ->
      REGEXP  = /// /(?!.+/)(.+)$ ///
      url     = @ctx._target.url

      # TODO убрать при выкате на продакшн
      url   = "trash"

      match = url.match REGEXP

      callback = (data) =>
        post.owner_id = data.response.object_id
        @wallPost()

      # если в url была какая-то некорректная строка, использовать current user id
      try
        params =
          screen_name: match[1]

        VK.Api.call "utils.resolveScreenName", params, callback
      catch error
        console.log error

        post.owner_id = userId
        @wallPost()

    saveWallPhoto: (savedPicture) ->

      savedPicture = JSON.parse savedPicture

      callback = (data) =>
        attachments = new Array()
        attachments.push data.response[0].id
        attachments.push @ctx._target.href
        attachments = attachments.join ","

        post.attachments = attachments
        @setOwnerId()

      params =
        user_id: userId
        server: savedPicture.server
        photo: savedPicture.photo
        hash: savedPicture.hash

      VK.Api.call "photos.saveWallPhoto", params, callback

    wallPost: () ->

      callback = (data) ->
        console.log data

      VK.Api.call "wall.post", post, callback

    getWallUploadServer: () ->
      console.log "getWallUploadServer"

      if !userId?
        onSuccess = () =>
          @getWallUploadServer()

        @login onSuccess
        return false

      params =
        user_id: userId

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

        console.log @ctx

        @onComplete @ctx, sendF

      VK.Api.call "photos.getWallUploadServer", params, callback

    login: (onSuccess) ->

      authInfo = (response) =>
        console.log response
        if response.session
          userId = response.session.mid
          onSuccess()
        else
          VK.Auth.login authInfo, ACESS_LVL
          @ctx._status = "error"
          @onComplete @ctx, sendF

      VK.Auth.getLoginStatus authInfo

    handleTarget: (ctx, onComplete) ->
      console.log "vk handle target"

      @ctx = ctx
      @onComplete = onComplete

      console.log ctx

      if ctx._ads[0].rendered.saved
        @saveWallPhoto ctx._ads[0].rendered.saved
        return false

      if ctx._ads[0].rendered.sioUrl then return @getWallUploadServer()
      #@getWallUploadServer()


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
