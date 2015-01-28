define [], ()->

  $doc = $ document
  $app = $ "#socialApp"

  class SocialView

    constructor: ()->
      @bindEvents()

    bindEvents: ()->

      $doc.on "click", "#socialApiAddTargetBtn", (e)=>
        e.preventDefault()
        $this = $ e.currentTarget
        href = $this.attr "href"

        $.ajax(
          url: href
          success: (data)->
            console.log data
            $app.append data
        )

      $doc.on "change", ".js-social-target_it input", (e)->
        console.log "change event"
        $this = $ e.currentTarget
        $form = $this.closest ".js-social_add-target-form"
        console.log $this.val()

        $form.trigger "submit"

      $doc.on "submit", ".js-social_add-target-form", (e)->
        e.preventDefault()
        $this = $ e.currentTarget
        action = $this.attr "action"
        data = $this.serialize()

        ###
          $.ajax(
            type: "Post"
            url: action
            data: data
            success: (data)->
              console.log data
          )
        ###


  return new SocialView()
