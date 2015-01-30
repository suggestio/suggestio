define [], ()->

  $doc = $ document
  $app = $ "#socialApp"

  $.fn.sioSerializeObject = () ->
    result = new Object()
    $form = $ this
    $input = $form.find "input[data-xname]"

    $input.each ()->
      $thisInput = $ this
      # если не  выставлен чекбокс, не записываем свойство
      if $thisInput.attr("type") == "checkbox" && !$thisInput.prop("checked")
        return true
      xname = $thisInput.data "xname"
      result[xname] = $thisInput.val()

    return result

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
        e.preventDefault()
        $this = $ e.currentTarget
        $form = $this.closest ".js-social_add-target-form"
        href = $this.attr "href"

        if href == "#"
          $form.remove()

        $.ajax(
          type: "Post"
          url: href
          statusCode:
            204: ()->
              $form.remove()
            404: ()->
              $form.remove()
            403: ()->
              # TODO сделать нормальную обработку ошибки
              alert "Повторите попытку"
        )

      $doc.on "submit", "#js-social-target-list", (e)->
        $form = $ e.currentTarget
        adv = new Array()

        $(".js-social_add-target-form").each (index)->
          $targetForm = $ this
          adv.push $targetForm.sioSerializeObject()

        html = ""
        console.log adv
        for target in adv
          if target.return
            html += "<input type='hidden' name='adv[#{_i}].tg_id' value='#{target.tg_id}' />"
            html += "<input type='hidden' name='adv[#{_i}].return' value='#{target.return}' />"

        $form.prepend html
        console.log html
        return true





  return new SocialView()
