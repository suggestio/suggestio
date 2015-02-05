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

        #@test()

      $.getScript "http://platform.twitter.com/widgets.js", callback

      $(document).on "click", "#superTwitterButton", (e)->
        console.log "click event"
        e.preventDefault()
        $this = $ this
        href = $this.attr "href"

        href = "https://twitter.com/share?url=#{encodeURIComponent("https://pbs.twimg.com/media/B9ELWbFCQAAI_wY.jpg")}&text=testSomethingText"

        window.open(href,'','toolbar=0,status=0,width=626,height=436');

        callback = (data)->
          w = window.open()
          $(w.document.body).html(data)


        #$.getScript href, callback

      #$("#superTwitterButton").trigger "click"

      return true

    test: ()->

      console.log "twitter api test"

      options =
        align: "left"

      twttr.widgets.createTweet(
        '511181794914627584'
        document.getElementById('tweet-container')
        options
      )
      .then(
        (el)->
          console.log "@ev's Tweet has been displayed."
      )


      callback = (data)->
        console.log data