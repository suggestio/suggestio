define ["SioPR"], (SioPR) ->

  SioPR = new SioPR()

  class Facebook
    API_ID = 967620393262021

    constructor: (@ws, ctx, onComplete) ->
      console.log "Fb init"

      options =
        appId      : API_ID
        xfbml      : true
        cookie     : true
        version    : "v2.2"

      window.fbAsyncInit = () =>
        FB.init options

        SioPR.registerService ctx, onComplete

      @loadSdk document, 'script', 'facebook-jssdk'


    sendF = (json) ->
      message = JSON.stringify json
      @ws.send message

    statusChangeCallback: (response) ->
      console.log "statusChangeCallback"
      console.log response

      if response.status == "connected"
        @testAPI()
      else if response.status == "not_authorized"
        console.log "Please log into this app."
        @login()
      else
        console.log "Please log into Facebook."
        @login()

    checkLoginState: () ->
      FB.getLoginStatus(
        (response) =>
          @statusChangeCallback(response)
      )


    login: () ->

      loginParams =
        scope: "user_actions.news,user_photos,publish_actions"

      FB.login(
        (response) =>
          console.log "--login--"
          console.log response
          console.log "--login--"
          if response.authResponse
            @publicatePost()
          else
            @ctx._status = "error"
            @ctx._error = "auth error"
            console.log "---auth error---"
            console.log @ctx
            @onComplete @ctx, sendF
        loginParams
      )

    publicatePost: () ->
      params =
        message: @ctx._ads[0].content.fields[0].text
        picture: @ctx._ads[0].rendered.sioUrl
        link: @ctx._target.href

      callback = (response) ->
        if !response || response.error
          console.log "Error occured"
          @ctx._status = "error"
          @ctx._error = response.error
        else
          console.log "Post ID: #{response.id}"
          @ctx._status = "success"

        @onComplete @ctx, sendF

      FB.api(
        "/me/feed"
        "post"
        params
        callback
      )


    testAPI: () ->
      console.log "Welcome to Facebook API!  Fetching your information.... "
      FB.api(
        "/me"
        (response) ->
          console.log response
          console.log "Successful login for: #{response.name}"
      )

    loadSdk: (d, s, id) ->
      js = d.getElementsByTagName(s)[0]
      fjs = d.getElementsByTagName(s)[0]
      if d.getElementById(id) then return
      js = d.createElement(s)
      js.id = id
      js.src = "//connect.facebook.net/en_US/sdk.js"
      fjs.parentNode.insertBefore js, fjs

    handleTarget: (ctx, onComplete) ->
      console.log "fb handle target"

      @ctx = ctx
      @onComplete = onComplete

      #@testAPI()
      @login()