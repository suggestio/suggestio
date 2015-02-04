define ["SioPR"], (SioPR) ->

  SioPR = new SioPR()

  class Twitter

    @serviceName: "Twitter"

    constructor: (@ws, @ctx, @onComplete) ->
      console.log "Twitter init"

      callback = () =>
        console.log twttr
        twttr.widgets.load()
        SioPR.registerService @ctx, @onComplete

        @test()

      $.getScript "http://platform.twitter.com/widgets.js", callback


    test: ()->

      console.log "twitter api test"

      options =
        size: "large"
        via: "twitterdev"
        related: "twitterapi,twitter"
        text: "custom share text"
        hashtags: "example,demo"

      twttr.widgets.createShareButton(
        "https:\/\/dev.twitter.com\/web\/tweet-button"
        document.getElementById("tweet-container")
        options
      )