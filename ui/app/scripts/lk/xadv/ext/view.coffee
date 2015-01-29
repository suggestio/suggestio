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

        $.ajax(
          type: "Post"
          url: action
          data: data
          success: (data)->
            console.log "---form submit callback---"
            console.log data
        )

      $doc.on "change", ".js-social-target_option", (e)->
        $this = $ e.currentTarget
        checked = $this.prop "checked"
        $form = $this.closest ".js-social_add-target-form"

        $form.find(".js-social-target_option").not($this).prop("checked", false)

      $doc.on "click", ".js-delete-social-target", (e)->
        console.log "js delete target"
        e.preventDefault()
        $this = $ e.currentTarget
        href = $this.attr "href"

        $.ajax(
          type: "POST"
          url: href
          success: (data)->
            console.log data
        )

      $doc.on "submit", "#js-social-target-list", (e)->
        console.log "submit"
        e.preventDefault()



  return new SocialView()
