define ["SioPR"], (SioPR) ->

  SioPR = new SioPR()

  class Facebook

    @serviceName: "Facebook"

    constructor: (@ws, @ctx, @onComplete) ->
      console.log "Fb init"

      try
        API_ID = @ctx._service.appId
      catch error
        @ctx._status = "error"
        @complete()

      options =
        appId      : API_ID
        xfbml      : true
        cookie     : true
        version    : "v2.2"

      window.fbAsyncInit = () =>
        FB.init options

        SioPR.registerService @ctx, onComplete

      @loadSdk document, 'script', 'facebook-jssdk'

    ###*
      отправляет сообщения на сервер
    ###
    complete: ()->

      sendF = (json) =>
        message = JSON.stringify json
        @ws.send message

      @onComplete @ctx, sendF

    ###*
      авторизация пользователя
    ###
    login: () ->

      loginParams =
        scope: "publish_actions"

      FB.login(
        (response) =>
          if response.authResponse
            @checkUserType()
          else
            @ctx._status = "error"
            @ctx._error =
              msg: "e.ext.adv.unathorized"
              info: response
            @complete()
        loginParams
      )

    ###*
       проверка цели размещения: группа или пользователь
    ###
    checkUserType: ()->
      REGEXP  = /// /(?!.+/)(.+)$ ///
      url     = @ctx._target.url
      # если на конце url стоит '/', удалить его
      if url.slice(-1) == "/" then url = url.substring(0, url.length - 1)

      if url.indexOf("/groups/") > 0
        match = url.match REGEXP
        try
          groupId = match[1]
          @publicatePostInGroup groupId
        catch exception
          console.log exception
      else
       @publicatePost()

    ###*
      публикует пост в группе
      @param {Number} id группы
    ###
    publicatePostInGroup: (groupId)->
      callback = (response)=>
        if !response || response.error
          console.log "Error occured"
          @ctx._status = "error"
          @ctx._error =
            msg: "e.ext.adv.permissions"
            info: response.error
        else
          @ctx._status = "success"
        @complete()

      try
        options =
          picture: @ctx._ads[0].rendered.sioUrl
          message: @ctx._ads[0].content.fields[0].text
          link: @ctx._target.href
          name: "testName"
          caption: "suggest.io"
          description: "testDescription"
          privacy:
            value: "EVERYONE"
      catch error
        @ctx._status = "error"
        @complete()

      FB.api(
        "/#{groupId}/feed"
        "POST"
        options
        callback
      )

    ###*
      публикация на стене у текущего пользователя
    ###
    publicatePost: () ->
      try
        params =
          picture: @ctx._ads[0].rendered.sioUrl
          message: @ctx._ads[0].content.fields[0].text
          link: @ctx._target.href
          name: "testName"
          caption: "suggest.io"
          description: "testDescription"
          privacy:
            value: "EVERYONE"
      catch error
        @ctx._status = "_error"
        @complete()

      callback = (response) =>
        if !response || response.error
          console.log "Error occured"
          @ctx._status = "error"
          @ctx._error = response.error
        else
          console.log "Post ID: #{response.id}"
          @ctx._status = "success"

        @complete()

      FB.api(
        "/me/feed"
        "post"
        params
        callback
      )

    ###*
      загрузка fb api
    ###
    loadSdk: (d, s, id) ->
      js = d.getElementsByTagName(s)[0]
      fjs = d.getElementsByTagName(s)[0]
      if d.getElementById(id) then return
      js = d.createElement(s)
      js.id = id
      js.src = "//connect.facebook.net/en_US/sdk.js"
      fjs.parentNode.insertBefore js, fjs

    ###*
      на основе контекста от сервера принимает решение о действиях, которые будут выполнены на этом шаге
    ###
    handleTarget: (ctx, onComplete) ->
      console.log "fb handle target"

      @ctx = ctx
      @onComplete = onComplete

      @login()