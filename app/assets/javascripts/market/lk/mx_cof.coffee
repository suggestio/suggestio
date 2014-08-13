$(document).ready ->

  cbca.popup = CbcaPopup
  cbca.pc = PersonalCabinet

  cbca.pc.init()
  cbca.popup.init()

PersonalCabinet =

  ###################################################################################################################
  ## Уведомления в шапке сайта ##
  ###################################################################################################################
  statusBar:

    close: ($bar) ->
      if $bar.data 'open'
        $bar.data 'open', false
        $bar.slideUp()

    show: ($bar) ->
      if !$bar.data 'open'
        $bar.data 'open', true
        $bar.slideDown()

    init: ()->

      $ '.status-bar'
      .each ()->
        $this = $ this

        cbca.pc.statusBar.show $this
        close_cb = () ->
          cbca.pc.statusBar.close $this

        setTimeout close_cb, 5000

      $ document
      .on 'click', '.status-bar', ()->
        $this = $ this
        cbca.pc.statusBar.close $this

  ##################################################################################################################
  ## Форма опроса при запросе инвайта для владельца WiFi сети ##
  ##################################################################################################################
  wifi: () ->

    $ '.js-quiz__checkbox'
    .removeAttr 'disabled'
    .removeAttr 'checked'

    $ document
    .on 'submit', '#wifiJoinForm', (e)->
      $form = $ this

      $form
      .find '.js-quiz__checkbox'
      .removeAttr 'disabled'

      return true

    $ document
    .on 'click', '.js-quiz__checkbox', (e)->
      $this = $ this
      nextSelector = $this.attr 'data-next'
      quizElement = $this.closest '.js-quiz__element'
      thisIndex = quizElement.attr 'data-index'

      $ '.js-quiz__element:visible'
      .not this
      .each () ->
        $element = $ this

        if $element.attr('data-index') > thisIndex
          $element
          .hide()
          .find 'input'
          .removeAttr 'disabled'
          .removeAttr 'checked'

        if $element.attr('data-index') == thisIndex
          $element
          .find 'input'
          .removeAttr 'disabled'

      $this.attr 'disabled', 'disabled'
      $ '.js-quiz__result'
      .hide()

      ## вместе с описательным текстом показать кнопку Написать ##
      if nextSelector == '#text0' || nextSelector == '#text1'
        nextSelector += ',#text3'

      $ nextSelector
      .show()

  login: () ->

    #################################################################################################################
    ## CAPTCHA ##
    #################################################################################################################
    $ document
    .on 'click', '#captchaReload', (e)->
      e.preventDefault()
      $this = $ this
      $captchaImage = $ '#captchaImage'
      $parent = $captchaImage.parent()
      random = Math.random()

      $captchaImage.remove()
      $parent.prepend '<img class="captcha_img" id="captchaImage" src="/captcha/get/' + $('#captchaId').val() + '?v='+random+'" />'

    $newPasswordForm = $ '#newPasswordForm'
    if $newPasswordForm.size()
      cbca.popup.showPopup '#newPasswordForm'

    $ document
    .on 'submit', '#recoverPwForm form', (e)->
      e.preventDefault()
      $form = $ this
      action = $form.attr 'action'

      $.ajax(
        type: "POST",
        url: action,
        data: $form.serialize(),
        success: (data)->
          $recoverPwForm = $ '#recoverPwForm'

          $recoverPwForm
          .find 'form'
          .remove()

          $recoverPwForm
          .append data

        error: (error)->

          $ '#recoverPwForm'
          .remove()

          $ '#popupsContainer'
          .append error.responseText

          cbca.popup.showPopup '#recoverPwForm'
      )

  billing: () ->

    $ document
    .on 'click', '#getTransactions', (e)->
      e.preventDefault()
      $this = $ this
      $transactionsHistory = $ '#transactionsHistory'
      $transactionsList = $ '#transactionsList'

      $.ajax(
        url: $this.attr 'href'
        success: (data)->
          if $this.attr 'data-init'
            $transactionsList
            .find 'tr'
            .not ':first'
            .remove()
          else
            $this
            .closest 'tr'
            .remove()

          $transactionsList
          .append data

          if $this.attr 'data-init'
            $transactionsHistory
            .slideDown(
              600,
              () ->
                cbca.pc.common.setBorderLineHeight $transactionsList
            )

        error: (error)->
          console.log error
      )

  advRequest: () ->

    $ document
    .on 'click', '.js-adv-req-forms', (e)->
      e.preventDefault()
      $ '#advReqRefuse, #advReqAccept'
      .toggle()

    $ document
    .on 'submit', '#advReqRefuse', (e)->
      $this = $ this
      $textarea = $this.find 'textarea'

      if !$textarea.val()
        $textarea
        .closest '.input'
        .addClass '__error'
        return false
      else
        return true

  adsList: () ->

    $ document
    .on 'click', '.ads-list-block__preview_add-new', ()->
      $this = $ this

      $this
      .parent()
      .find('.ads-list-block__link')[0]
      .click()

  common:

    ##################################################################################################################
    ## Чекбоксы ##
    ##################################################################################################################
    checkbox: () ->

      $ '.lk input[type = "checkbox"]'
      .each ()->
        $this = $ this
        checked = $this.attr 'data-checked'

        if checked == 'checked'
          this.checked = true
        else
          $this.removeAttr 'checked'

      ## Набор чекбоксов, где можно выбрать только один вариант
      $ document
      .on 'click', '.js-one-checkbox', (e)->
        e.stopPropagation()
        $this = $ this
        dataName = $this.attr 'data-name'
        dataFor = $this.attr 'data-for'
        value = $this.attr 'data-value'

        if this.checked
          $ '.js-one-checkbox[data-name = "'+dataName+'"]'
          .filter ':checked'
          .removeAttr 'checked'

          this.checked = true

          $ '#'+dataFor
          .val value
        else
          $ this
          .removeAttr 'checked'

    ##################################################################################################################
    ## Блоки одинаковой высоты ##
    ##################################################################################################################
    setEqualHeightBlocks: () ->
      $blocks = $ '.js-equal-height'
      height = 0

      $blocks.each () ->
        $this = $ this
        thisHeight = $this.height()

        if thisHeight > height
          height = thisHeight

      $blocks.height height

    ##################################################################################################################
    ## Скрытые элементы ##
    ##################################################################################################################
    hideElements: ($obj) ->
      $obj = $obj || $ 'html'
      $elements = $obj.find '.js-hidden'

      $elements.hide()

    ##################################################################################################################
    ## Высота вертикальных линий ##
    ##################################################################################################################
    setBorderLineHeight: ($obj) ->
      $obj = $obj || $ 'html'
      $lines = $obj.find '.js-vertical-line'

      $lines
      .each () ->
        $this = $ this
        $parent = $this.parent()

        if $this.attr 'data-inherit-height'
          lineHeight = $parent.height()
        else
          lineHeight = $parent.height() - 10

        $this.height lineHeight

    ##################################################################################################################
    ## Элементы ввода ##
    ##################################################################################################################
    inputs: () ->

      $ document
      .on 'focus', '.js-input-w input, .js-input-w textarea', (e)->
        $ this
        .closest '.input-w'
        .toggleClass '__focus', true

      $ document
      .on 'blur', '.js-input-w input, .js-input-w textarea', (e)->
        $ this
        .closest '.input-w'
        .removeClass '__focus'


    ##################################################################################################################
    ## Стандартные обработчики нажатия кнопок ##
    ##################################################################################################################
    buttons: () ->

      $ document
      .on 'click', '.js-btn', (e)->
        e.preventDefault()
        $this = $ this
        href = $this.attr 'href'

        if !href
          return false

        if href && href.charAt(0) == '#'
          cbca.popup.showPopup href
        else
          $.ajax(
            url: href,
            success: (data)->
              $ajaxData = $ data
              popupId = $ajaxData.attr 'id'

              cbca.popup.hidePopup()

              $ '#'+popupId
              .remove()

              $ '#popupsContainer'
              .append data

              cbca.popup.showPopup '#'+popupId
          )

      $ document
      .on 'click', '.js-submit-btn', (e)->
        e.preventDefault()
        $this = $ this
        dataFor = $this.attr 'data-for'

        if dataFor
          $form = $ dataFor
        else
          $form = $this.closest 'form'

        $form.trigger 'submit'

      $(document).on 'click', '.js-slide-btn', (e)->
        e.preventDefault()
        $this = $ this
        href = $this.attr 'href'

        if href
          if href.charAt(0) == '#'
            $ href
            .slideToggle()
          else
            $.ajax(
              url: href
              success: (data) ->
                $data = $ data
                $siomBlock = $this.closest '.js-slide-w'
                ## :first потому что может быть вложенный siomBlock
                $siomBLockCnt = $siomBlock.find '.js-slide-cnt:first'

                $siomBLockCnt.append $data

                $siomBLockCnt
                .slideDown(
                  600,
                  () ->
                    cbca.pc.common.setBorderLineHeight $siomBLockCnt
                )

                $this.removeAttr 'href'
            )
        else
          $this
          .closest '.js-slide-w'
          .find '.js-slide-cnt:first' ## :first потому что может вложенный siomBlock
          .slideToggle()

        $this.toggleClass '__open'
        if !$this.attr 'data-fix-title'
          if $this.hasClass '__open'
            $this.html 'Свернуть'
          else
            $this.html 'Развернуть'

  init: () ->

    cbca.pc.common.setEqualHeightBlocks()
    cbca.pc.common.setBorderLineHeight()
    cbca.pc.common.hideElements()
    cbca.pc.common.inputs()
    cbca.pc.common.checkbox()
    cbca.pc.common.buttons()

    cbca.pc.statusBar.init()
    cbca.pc.wifi()
    cbca.pc.login()
    cbca.pc.billing()
    cbca.pc.advRequest()
    cbca.pc.adsList()

#######################################################################################################################
## Всплывающие окна ##
#######################################################################################################################
CbcaPopup =

  $overlay: $ '#overlay'
  $container: $ '#popupsContainer'
  $body: $ 'body'

  showOverlay: () ->
    this.$overlay.show()
    this.$container.show()
    this.$body.addClass 'ovh'

  hideOverlay: () ->
    this.$overlay.hide()
    this.$container.hide()
    this.$body.removeClass 'ovh'

  setPopupPosition: (popupSelector) ->
    $popup = $ popupSelector
    ## независимые цифры, подобраны согласно внешнему виду получаемого результата
    minTop = 25

    if !$popup.size()
      $popup = $ '.popup:visible'

    popupHeight = $popup.height()
    containerHeight = this.$container.height()
    diffHeight = containerHeight - popupHeight

    if diffHeight > minTop*2
      top = Math.ceil( (containerHeight - popupHeight)/2 )
      $popup.css 'top', top
    else
      $popup.css 'top', minTop

  setOverlayHeight: (popupHeight)->
    if !popupHeight
      popupHeight = 0

      $ '.popup'
      .each () ->
        $this = $ this
        thisHeight = $this.height()

        if thisHeight > popupHeight
          popupHeight = thisHeight

    popupsContainerHeight = CbcaPopup.$container.height()

    if popupHeight > popupsContainerHeight
      this.$overlay.height popupHeight + 50
    else
      this.$overlay.height popupsContainerHeight

  showPopup: (popupSelector) ->
    this.showOverlay()
    popupSelector = popupSelector || '.popup'
    $popup = $ popupSelector
    $popup.show()

    console.log popupSelector

    $popup
    .find '.sm-block'
    .addClass 'double-size'

    popupHeight = $popup.height()
    this.setOverlayHeight popupHeight

    cbca.pc.common.hideElements $popup
    cbca.pc.common.setBorderLineHeight

    this.setPopupPosition popupSelector

  hidePopup: (popupSelector) ->
    popupSelector = popupSelector || '.popup'
    $popup = $ popupSelector

    this.hideOverlay()
    $popup.hide()

    $ '#overlayData'
    .hide()

  init: () ->

    $ window
    .resize () ->
      cbca.popup.setOverlayHeight()
      cbca.popup.setPopupPosition()

    $ document
    .on 'click', '.js-close-popup', (e)->
      e.preventDefault()
      $this = $ this
      $popup = $this.closest '.popup'
      popupId = $popup.attr 'id'
      popupSelector = '#'+popupId

      cbca.popup.hidePopup popupSelector

    $ document
    .on 'click', '#overlay', ()->

      cbca.popup.hidePopup()

    ## Если после перезагрузки страницы в попапе есть поля с ошибками, нужно его отобразить
    $ '.popup .__error, .js-popup .__error'
    .each ()->
      $this = $ this
      $popup = $this.closest '.popup'
      popupId = $popup.attr 'id'
      popupSelector = '#'+popupId

      cbca.popup.showPopup popupSelector

    ## Кнопка Назад внутри попапа
    $ document
    .on 'click', '.js-popup-back', (e)->
      $this = $ this
      targetPopupHref = $this.attr 'href'
      $targetPopup = $ targetPopupHref

      $this
      .closest '.popup'
      .hide()

      cbca.popup.hidePopup this
      cbca.popup.showPopup targetPopupHref

    $ document
    .on 'click', '.js-remove-popup', (e)->
      $this = $ this
      $popup = $this.closest '.popup'
      popupId = $popup.attr 'id'
      popupSelector = '#'+popupId

      cbca.popup.hidePopup popupSelector

      $ popupSelector
      .remove()

    ## esc button
    $ document
    .bind 'keyup', (e) ->
      if e.keyCode == 27
        cbca.popup.hidePopup()


######################
## TODO: отрефакторить
######################
market =
  styles :
    init : () ->
      style_tags = document.getElementsByTagName('code')
      css = ''

      for s in style_tags
        css = css.concat( s.innerHTML )

      style_dom = document.createElement('style')
      style_dom.type = "text/css"
      style_dom.innerHTML = ''
      style_dom.appendChild(document.createTextNode(css))
      head = document.getElementsByTagName('head')
      head[0].appendChild(style_dom)

  init_colorpickers : () ->

    $ '.js-custom-color'
    .each () ->

      current_value = $(this).attr 'data-current-value'

      cb = ( _this ) ->
        i = Math.random()
        _this.ColorPicker
      	  color: current_value
      	  onShow: (colpkr) ->
      	    $(colpkr).fadeIn(500)
      	  onHide: (colpkr) ->
      	    $(colpkr).fadeOut(500)
      	  onChange: (hsb, hex, rgb) ->
      	    market.ad_form.queue_block_preview_request()
      	    _this.find('input').val(hex).trigger('change')
      	    _this.css
      	      'background-color' : '#' + hex
      cb( $(this) )

  ## Главная страница ЛК торгового центра
  mart :
    init : () ->
      market.init_colorpickers()

  ###################################################################################################################
  ## Класс для работы с картинками ##################################################################################
  ###################################################################################################################
  img :

    init_upload : () ->

      $ '.w-async-image-upload'
      .unbind 'change'
      .bind 'change', () ->
        $this = $ this
        relatedFieldId = $this.attr 'data-related-field-id'
        form_data = new FormData()

        is_w_block_preview = $this.attr 'data-w-block-preview'

        if $this[0].type == 'file'
          form_data.append $this[0].name, $(this)[0].files[0]

        request_params =
          url : $this.attr "data-action"
          method : 'post'
          data : form_data
          contentType: false
          processData: false
          success : ( resp_data ) ->

            if typeof is_w_block_preview != 'undefined'
              market.ad_form.queue_block_preview_request()

            $('#' + relatedFieldId + ' .image-key, #' + relatedFieldId + ' .js-image-key').val(resp_data.image_key).trigger('change')
            $('#' + relatedFieldId + ' .image-preview').show().attr "src", resp_data.image_link

        $.ajax request_params

        return false

      $ document
      .on 'click', '.js-remove-image', (e)->
        e.preventDefault()
        $this = $ this
        $parent = $this.parent()

        $parent
        .next '.add-file-w'
        .show()
        $parent.remove()

        cbca.editAdPage.updatePreview()


      $ document
      .on 'mouseenter', '.add-file-w', () ->
        $ this
        .find('.add-file-w_btn')
        .addClass '__hover'

      $ document
      .on 'mouseleave', '.add-file-w', () ->
        $ this
        .find('.add-file-w_btn')
        .removeClass '__hover'

      $ document
      .on 'mousedown', '.add-file-w', () ->
        $ this
        .find('.add-file-w_btn')
        .addClass '__active'

      $ document
      .on 'mouseup', '.add-file-w', () ->
        $ this
        .find('.add-file-w_btn')
        .removeClass '__active'

      $ '.js-file-upload'
      .unbind "change"
      .bind "change", (e) ->
        e.preventDefault()
        $this = $ this
        $parent = $this.closest '.add-file-w'
        form_data = new FormData()

        is_w_block_preview = $this.attr 'data-w-block-preview'

        if $this[0].type == 'file'
          form_data.append $this[0].name, $this[0].files[0]

        request_params =
          url : $this.attr "data-action"
          method : 'post'
          data : form_data
          contentType: false
          processData: false
          success : ( resp_data ) ->

            if typeof is_w_block_preview != 'undefined'
              market.ad_form.queue_block_preview_request()

              $('#' + $this.attr('data-related-field-id'))
              .find '.js-image-key'
              .val resp_data.image_key
              .trigger 'change'

            else

              fieldName = $this.attr 'data-name'

              if $this.attr 'multiple'
                i = $parent
                    .parent()
                    .find '.__preview'
                    .size()
                fieldName = fieldName + '[' + i + ']'

              html = ['<div class="add-file-w __preview">',
                      '<input class="js-image-key" type="hidden" name="',
                      fieldName,
                      '" value=""/>',
                      '<img class="add-file-w_image js-image-preview" src="" />',
                      '<a class="add-file-w_btn siom-remove-image-btn js-remove-image" title="Удалить файл"></a>',
                      '</div>'].join ''

              $parent.before html

              $parent
              .prev()
              .find '.js-image-key'
              .val resp_data.image_key
              .trigger 'change'

              $parent
              .prev()
              .find '.js-image-preview'
              .show()
              .attr 'src', resp_data.image_link

              if !$this.attr 'multiple'
                $parent.hide()

        $.ajax request_params


    crop :
      init_triggers : () ->
        $('.js-img-w-crop').unbind 'click'
        $('.js-img-w-crop').bind 'click', () ->

          img_key = jQuery('input', $(this).parent()).val()
          img_name = jQuery('input', $(this).parent()).attr 'name'

          if img_key == ''
            alert 'сначала нужно загрузить картинку'
            return false

          width = $('.sm-block').attr 'data-width'
          height = $('.sm-block').attr 'data-height'

          marker = $(this).attr 'data-marker'

          $.ajax
            url : '/img/crop/' + img_key + '?width=' + width + '&height=' + height + '&marker=' + marker
            success : ( data ) ->
              $('#overlay, #overlayData').show()
              $('#overlayData').html data

              market.img.crop.init( img_name )

          return false

      save_crop : ( form_dom ) ->

        offset_x = parseInt( $('#imgCropTool img').css('left').replace('px', '') ) || 0
        c_offset_x = this.container_offset_x
        #offset_x = offset_x - parseInt c_offset_x

        offset_y = parseInt( $('#imgCropTool img').css('top').replace('px', '') ) || 0
        c_offset_y = this.container_offset_y
        #offset_y = offset_y - parseInt c_offset_y

        ci = this.crop_tool_img_dom

        sw = parseInt ci.attr 'data-width'
        sh = parseInt ci.attr 'data-height'

        rw = parseInt ci.width()
        rh = parseInt ci.height()

        offset_x = sw * offset_x / rw
        offset_y = sh * offset_y / rh

        console.log 'offset_x : ' + offset_x
        console.log 'offset_y : ' + offset_y

        target_offset = "+" + Math.round( Math.abs(offset_x) ) + "+" + Math.round(Math.abs(offset_y))

        target_size = rw + 'x' + rh

        tw = parseInt this.crop_tool_dom.attr 'data-width'
        th = parseInt this.crop_tool_dom.attr 'data-height'

        resize = rw*2 + 'x' + rh*2

        if sw / sh > tw / th
          ch = sh
          cw = ch * tw / th

        else
          cw = sw
          ch = cw * th / tw

        crop_size = Math.round( cw ) + 'x' + Math.round( ch )

        jQuery('input[name=crop]', form_dom).val( crop_size + target_offset )
        jQuery('input[name=resize]', form_dom).val( resize )

        form_dom1 = $('#imgCropTool form')
        image_name = this.image_name

        $.ajax
          url : form_dom1.attr 'action'
          method : 'post'
          data : form_dom1.serialize()
          success : ( img_data ) ->
            console.log market.img.crop.img_name
            $('input[name=\'' + market.img.crop.img_name + '\']').val img_data.image_key
            market.ad_form.queue_block_preview_request request_delay=10

            $('#overlay, #overlayData').hide()
            $('#overlayData').html ''


      init : (img_name) ->
        this.img_name = img_name
        this.crop_tool_dom = $('#imgCropTool')
        this.crop_tool_container_dom = jQuery('.js-crop-container', this.crop_tool_dom)
        this.crop_tool_container_div_dom = jQuery('div', this.crop_tool_container_dom)
        this.crop_tool_img_dom = jQuery('img', this.crop_tool_dom)

        width = parseInt this.crop_tool_dom.attr 'data-width'
        height = parseInt this.crop_tool_dom.attr 'data-height'

        img_width = parseInt this.crop_tool_img_dom.attr 'data-width'
        img_height = parseInt this.crop_tool_img_dom.attr 'data-height'

        this.crop_tool_container_dom.css
          'width' : width + 'px'
          'height' : height + 'px'


        ## отресайзить картинку по нужной стороне

        wbh = width/height
        img_wbh = img_width/img_height

        if wbh > img_wbh
          img_new_width = width
          img_new_height = img_height * img_new_width / img_width
        else
          img_new_height = height
          img_new_width = img_new_height * img_width / img_height

        container_offset_x = parseInt img_new_width - width
        container_offset_y = parseInt img_new_height - height

        this.crop_tool_img_dom.css
          'width' : img_new_width + 'px'
          'height' : img_new_height + 'px'

        this.crop_tool_container_div_dom.css
          'margin-left' : -container_offset_x + 'px'
          'margin-top' : -container_offset_y + 'px'

        this.container_offset_x = container_offset_x
        this.container_offset_y = container_offset_y

        x1 = this.crop_tool_container_div_dom.offset()['left']
        y1 = this.crop_tool_container_div_dom.offset()['top']

        x2 = x1 + container_offset_x
        y2 = y1 + container_offset_y

        this.crop_tool_img_dom.draggable
          'containment' : [x1,y1,x2,y2]

        ## Забиндить событие на сохранение формы
        $('#imgCropTool form').bind 'submit', () ->
          market.img.crop.save_crop $(this)

          return false

  ################################
  ## Размещение рекламной карточки
  ################################
  adv_form :
    update_price : () ->
      $.ajax
        url : $('#advsPriceUpdateUrl').val()
        method : 'post'
        data : $('#advsFormBlock form').serialize()
        success : ( data ) ->
          $('.js-pre-price').html data

      this.tabs.refine_counters()

    tabs :
      init : () ->
        ## табы с разными типами нод
        $('.mt-tab').bind 'click', () ->
          mt = $(this).attr 'data-member-type'
          $('.mt-block').hide()
          $('.mt-tab').removeClass 'advs-form-block__tabs-single-tab_active'
          $(this).addClass 'advs-form-block__tabs-single-tab_active'
          $('.mt-' + mt + '-block').show()

        this.refine_counters()

      refine_counters : () ->
        $('.mt-tab').each () ->
          mt = $(this).attr 'data-member-type'

          active_nodes = $('.mt-' + mt + '-block .advs-nodes__node_active').length

          mt_tab_counter_c = $('.mt-tab-' + mt + '-counter-c')
          mt_tab_counter = $('.mt-tab-' + mt + '-counter')

          console.log mt + ' : ' + active_nodes

          if active_nodes != 0
            mt_tab_counter_c.show()
            mt_tab_counter.html active_nodes
          else
            mt_tab_counter_c.hide()
            mt_tab_counter.html 0


    submit : () ->
      $('#advsFormBlock form').submit()

    init : () ->

      this.tabs.init()

      ## Datepickers
      $('.js-datepicker').each () ->
        $(this).datetimepicker
          lang:'ru'
          i18n:
            ru:
              months:['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь']
              dayOfWeek:["Вс", "Пн", "Вт", "Ср","Чт", "Пт", "Сб"]
          timepicker:false
          format:'Y-m-d'

      $('#advsSubmitButton').bind 'click', () ->
        market.adv_form.submit()
      $('#advsFormBlock input').bind 'change', () ->
        cf_id = $(this).attr 'data-connected-field'
        cf = $('#' + cf_id)
        if typeof cf_id != 'undefined'
          if $(this).is(':checked')
            cf.addClass 'advs-nodes__node_active'
            cf.find('.advs-nodes__node-dates').removeClass 'advs-nodes__node-dates_hidden'
            cf.find('.advs-nodes__node-options').removeClass 'advs-nodes__node-options_hidden'
          else
            cf.removeClass 'advs-nodes__node_active'
            cf.find('.advs-nodes__node-dates').addClass 'advs-nodes__node-dates_hidden'
            cf.find('.advs-nodes__node-options').addClass 'advs-nodes__node-options_hidden'

        market.adv_form.update_price()

  ##############################
  ## Редактор рекламной карточки
  ##############################
  ad_form :

    preview_request_delay : 300

    request_block_preview : ( is_with_auto_crop ) ->
      action = $('.js-ad-block-preview-action').val()

      if(action)
        $.ajax
          url : action
          method : 'post'
          data : $('#promoOfferForm').serialize()
          success : ( data ) ->

            if is_with_auto_crop == true
              console.log 'необходим авто кроп'
            $('#adFormBlockPreview').html data
            market.styles.init()
            $('.js-mvbl').draggable
              stop : () ->
                connected_input = $(this).attr 'data-connected-input'
                pos = $(this).position()

                $('input[name=\'' + connected_input + '.coords.x\']').val pos['left']
                $('input[name=\'' + connected_input + '.coords.y\']').val pos['top']


    queue_block_preview_request : ( request_delay, is_with_auto_crop ) ->
      request_delay = request_delay || this.preview_request_delay

      if typeof this.block_preview_request_timer != 'undefined'
        clearTimeout this.block_preview_request_timer

      is_with_auto_crop = is_with_auto_crop || false

      cb = () ->
        market.ad_form.request_block_preview is_with_auto_crop

      this.block_preview_request_timer = setTimeout cb, request_delay

    init_block_editor : () ->
      market.img.init_upload()
      market.img.crop.init_triggers()

      $('.js-align-editor').each () ->
        input = $(this).find 'input'
        buttons = $(this).find '.js-ae-button'

        buttons.bind 'click', () ->
          align_value = $(this).attr 'data-align'
          input.val align_value

          buttons.removeClass 'align-editor__button_active'
          $(this).addClass 'align-editor__button_active'

          market.ad_form.queue_block_preview_request()


      $('.js-custom-font-select, .js-custom-font-family-select').bind 'change', () ->
        market.ad_form.queue_block_preview_request()

      $('.js-input-w-block-preview').bind 'keyup', () ->
        market.ad_form.queue_block_preview_request()

      $('.js-block-height-editor-button').bind 'click', () ->

        _cell_size = 140
        _cell_padding = 20

        _direction = $(this).attr 'data-change-direction'

        _p = $(this).parent()
        _value_dom = _p.find('input')

        _cur_value = parseInt _value_dom.val()
        _min_value = parseInt _value_dom.attr 'data-min-value'
        _max_value = parseInt _value_dom.attr 'data-max-value'

        _cur_cells_value = Math.floor _cur_value / _cell_size

        if _direction == 'decrease'
          _cur_cells_value--
        else
          _cur_cells_value++

        _new_value = _cur_cells_value * _cell_size + ( _cur_cells_value - 1 ) * _cell_padding

        if _new_value > _max_value
          _new_value = _max_value

        if _new_value < _min_value
          _new_value = _min_value

        _value_dom.val _new_value

        console.log _value_dom.val()
        market.ad_form.queue_block_preview_request request_delay=10

    set_descr_editor_bg : () ->
      hex = '#' + $('#ad_descr_bgColor').val()
      $('#ad_descr_text_ifr').contents().find('body').css 'background-color': hex

    init : () ->

      $('#promoOfferForm').bind 'submit', () ->

        tinyMCE.triggerSave()
        tinyMCE.remove()

        $('.tinymce_editor, .tinymce .select-color').hide()

        data = $('.js-tinymce').val()
        data = data.replace /<p(.*?)><\/p>/g, "<p$1>&nbsp;</p>"
        data = data.replace /<span(.*?)><\/span>/g, "<span$1>&nbsp;</span>"

        $('.js-tinymce').val data
        $('#promoOfferForm').unbind 'submit'

        console.log $('.js-tinymce').val()

        submit_cb = () ->
          $('#promoOfferForm').submit()

        setTimeout submit_cb, 1

        return false

      tinymce.init(
        selector:'textarea.js-tinymce',
        width: 615,
        height: 300,
        menubar: false,
        statusbar : false,
        plugins: 'link, textcolor, paste, colorpicker',
        toolbar: ["styleselect | fontsizeselect | alignleft aligncenter alignright | bold italic | colorpicker | link | removeformat" ],
        content_css: '/assets/stylesheets/market/descr.css',
        fontsize_formats: '10px 12px 14px 16px 18px 22px 26px 30px 34px 38px 42px 46px 50px 54px 58px 62px 66px 70px 74px 80px 84px',

        style_formats: [
          {title: 'Favorit Light Cond C Regular', inline: 'span', styles: { 'font-family':'favoritlightcondcregular'}},
          {title: 'Favorit Cond C Bold', inline: 'span', styles: { 'font-family':'favoritcondc-bold-webfont'}},
          {title: 'Helios Thin', inline: 'span', styles: { 'font-family':'heliosthin'}},
          {title: 'Helios Cond Light', inline: 'span', styles: { 'font-family':'helioscondlight-webfont'}},
          {title: 'Helios Ext Black', inline: 'span', styles: { 'font-family':'HeliosExtBlack'}},
          {title: 'PF Din Text Comp Pro Medium', inline: 'span', styles: { 'font-family':'PFDinTextCompPro-Medium'}},
          {title: 'Futur Fut C', inline: 'span', styles: { 'font-family':'futurfutc-webfont'}},
          {title: 'Pharmadin Condensed Light', inline: 'span', styles: { 'font-family':'PharmadinCondensedLight'}},
          {title: 'Newspaper Sans', inline: 'span', styles: { 'font-family':'newspsan-webfont'}},
          {title: 'Rex Bold', inline: 'span', styles: { 'font-family':'rex_bold-webfont'}},
          {title: 'Perforama', inline: 'span', styles: { 'font-family':'perforama-webfont'}},
          {title: 'Decor C', inline: 'span', styles: { 'font-family':'decorc-webfont'}},
          {title: 'BlocExt Cond', inline: 'span', styles: { 'font-family':'blocextconc-webfont'}},
          {title: 'Bodon Conc', inline: 'span', styles: { 'font-family':'bodonconc-webfont'}},
          {title: 'Confic', inline: 'span', styles: { 'font-family':'confic-webfont'}}
        ],

        language: 'RU_ru'

        font_size_style_values : '1px,2px',
        setup: (editor) ->
          editor.on 'init', (e) ->
            market.ad_form.set_descr_editor_bg()
      )

      ## Предпросмотр карточки с описанием
      $('.js-ad-preview-button').bind 'click', () ->
        tinyMCE.triggerSave()

        data = $('.js-tinymce').val()
        data = data.replace /<p(.*?)><\/p>/g, "<p$1>&nbsp;</p>"
        data = data.replace /<span(.*?)><\/span>/g, "<span$1>&nbsp;</span>"

        $('.js-tinymce').val data

        $.ajax
          url : $('.js-ad-block-full-preview-action').val()
          method : 'post'
          data : $('#promoOfferForm').serialize()
          success : ( data ) ->

            $ '#adFullPreview'
            .remove()
            $ '#popupsContainer'
            .append '<div class="popup" id="adFullPreview"><div class="popup_header"><a class="close f-right js-close-popup"></a></div><div class="popup_cnt"><div class="sio-mart-showcase">' + data + '</div></div></div>'
            $ '#adFullPreview .sm-block'
            .addClass 'double-size'
            cbca.popup.showPopup '#adFullPreview'

            market.styles.init()

        return false

      $(document).on 'change', '#ad_descr_bgColor', (e)->
        market.ad_form.set_descr_editor_bg()

      market.img.crop.init_triggers()
      this.request_block_preview()
      this.init_block_editor()

      icons_dom = $('#adFormBlocksList div')

      icons_dom.bind 'click', () ->

        icons_dom.removeClass 'blocks-list-icons__single-icon_active'
        $(this).addClass 'blocks-list-icons__single-icon_active'

        block_id = $(this).attr 'data-block-id'
        block_editor_action = $('#adFormBlocksList .block-editor-action').val()

        $('input[name=\'ad.offer.blockId\']').val block_id

        $.ajax
          url : block_editor_action
          method : 'post'
          data : $('#promoOfferForm').serialize()
          success : ( data ) ->
            $('#adFormBlockEditor').html data
            market.ad_form.init_block_editor()
            market.init_colorpickers()
            market.ad_form.request_block_preview()

  resize_preview_photos : () ->
    $('.preview .poster-photo').each () ->
      $this = $(this)

      image_w = parseInt $this.attr "data-width"
      image_h = parseInt $this.attr "data-height"

      cw = $this.closest('.preview').width()
      ch = $this.closest('.preview').height()

      if image_w / image_h < cw / ch
        nw = cw
        nh = nw * image_h / image_w
      else
        nh = ch
        nw = nh * image_w / image_h

      css_params =
        'width' : nw + 'px'
        'height' : nh + 'px'
        'margin-left' : - nw / 2 + 'px'
        'margin-top' : - nh / 2 + 'px'

      $this.css css_params



  init: () ->

    this.ad_form.init()
    $ document
    .ready () ->
      market.img.init_upload()
      market.resize_preview_photos()
      market.mart.init()
      market.adv_form.init()
      market.styles.init()

market.init()
window.market=market