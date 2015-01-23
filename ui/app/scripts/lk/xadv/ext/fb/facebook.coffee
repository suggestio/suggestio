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
          if response.authResponse
            console.log "Welcome!  Fetching your information.... "
            @testAPI()
          else
            console.log "User cancelled login or did not fully authorize."
        loginParams
      )

    publicatePost: () ->
      params =
        message: "Reading JS SDK documentation"
        picture: "http://cs621520.vk.me/v621520368/c705/AZFdRlNVAvA.jpg"
        link: "suggest.io"

      callback = (response) ->
        if !response || response.error
          console.log "Error occured"
          console.log response
        else
          console.log "Post ID: #{response.id}"

      FB.api(
        "/me/feed"
        "post"
        params
        callback
      )

    uploadPhotoByUrl: () ->

      params =
        url: "http://cs621520.vk.me/v621520368/c705/AZFdRlNVAvA.jpg"

      callback = (response) ->
        console.log response

      FB.api(
        "/me/photos",
        "POST",
        params
        callback
      )

    testAPI: () ->
      console.log "Welcome to Facebook API!  Fetching your information.... "
      FB.api(
        "/me"
        (response) ->
          console.log "Successful login for: #{response.name}"
      )

      @publicatePost()

      #@uploadPhotoByUrl()

    loadSdk: (d, s, id) ->
      js = d.getElementsByTagName(s)[0]
      fjs = d.getElementsByTagName(s)[0]
      if d.getElementById(id) then return
      js = d.createElement(s)
      js.id = id
      js.src = "//connect.facebook.net/en_US/sdk.js"
      fjs.parentNode.insertBefore js, fjs