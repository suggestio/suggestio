define ["SioPR"], (SioPR) ->

  SioPR = new SioPR()

  class Vk
    API_ID = 4705589
    ACESS_LVL = 8197

    userId = null

    constructor: (@ws, ctx, onComplete) ->
      console.log "vk init"

      window.vkAsyncInit = () =>
        VK.init
          apiId: API_ID

        SioPR.registerService ctx, onComplete

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

      # если в url была какая-то некорректная строка, использовать current user id
      try
        params =
          screen_name: match[1]
      catch exception
        params =
          screen_name: @ctx.user_id

      callback = (data) =>
        post.owner_id = data.response.object_id
        @wallPost()

      VK.Api.call "utils.resolveScreenName", params, callback

    saveWallPhoto: () ->
      savedPhoto = JSON.parse(@ctx._picture.saved)

      callback = (data) =>
        attachments = new Array()
        attachments.push data.response[0].id
        attachments.push @ctx._target.href
        attachments = attachments.join ","

        post.attachments = attachments

        @setOwnerId()

      params =
        user_id: @ctx.user_id
        server: savedPhoto.server
        photo: savedPhoto.photo
        hash: savedPhoto.hash

      VK.Api.call "photos.saveWallPhoto", params, callback

    wallPost: () ->

      callback = (data) ->
        console.log data

      VK.Api.call "wall.post", post, callback

    getWallUploadServer: (ctx, onComplete) ->
      params =
        user_id: userId

      callback = (data) =>
        if data.error
          onError data.error

        ctx._picture =
          size:
            width: 600
            height: 500
          upload:
            mode: "s2s"
            url: data.response.upload_url
            partName: "photo"

        onComplete ctx, sendF

      VK.Api.call "photos.getWallUploadServer", params, callback

    login: () ->
      console.log "vk login"

      authInfo = (response) =>
        console.log response
        if response.session
          userId = response.session.mid
        else
          VK.Auth.login authInfo, ACESS_LVL

      VK.Auth.getLoginStatus authInfo

    handleTarget: (ctx) ->
      console.log "vk handle target"

      if !userId? then return @login()
      if ctx._ads[0].rendered.saved then return @getWallUploadServer(ctx, onComplete)


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
