cbca = {}

$(document).ready ->

  cbca.popup = CbcaPopup
  cbca.pc = PersonalCabinet

  cbca.pc.init()
  cbca.popup.init()

isTouchDevice = () ->
  if document.ontouchstart != null
    false
  else
    if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
      false
    else
      true



PersonalCabinet =

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


  ##################################################################################################################
  ## Слайд блоки: сворачиваемые блоки с заголовком, клик по которому сворачивает-разворачивает блок.
  ##################################################################################################################
  slideBlock:

    # возвращает jQuery объект который нужно слайдить или false
    getSlideBlock: ($slideBlockBtn)->
      href = $slideBlockBtn.attr 'href'

      if href && href.charAt(0) == '#'
        $slideBlock = $ href
      else
        $slideBlockWrap = $slideBlockBtn.closest '.js-slide-w'
        ## :first потому что может быть вложенный слайд блок
        $slideBlock = $slideBlockWrap.find '.js-slide-cnt:first'

      if $slideBlock.is ':animated' || $slideBlock.data 'active'
        $slideBlock = false

      return $slideBlock

    slideToggle: ($slideBlock, $btn)->
      $btn = $btn || $slideBlock.parent().find '.js-slide-btn'
      href = $btn.attr 'href'

      if href && href.charAt(0) != '#'
        $slideBlock.data
          'active': true

        $.ajax(
          url: href
          success: (data) ->
            $data = $ data
            $slideBlock.append $data
            $slideBlock.slideDown()

            $btn.removeAttr 'href'
            $slideBlock.data
              'active': false

            cbca.pc.common.photoSlider()
        )
      else
        $slideBlock.slideToggle()

      $btn
      .toggleClass '__js-open'
      .closest '.js-slide-title'
      .toggleClass '__js-open'

    init: ()->
      event = if isTouchDevice() then 'touchend' else 'click'

      $ document
      .on event, '.js-slide-btn', (e)->
        e.preventDefault()
        e.stopPropagation() # чтобы не сработал обработчик на js-slide-title
        $this = $ this
        href = $this.attr 'href'
        $slideBlock = PersonalCabinet.slideBlock.getSlideBlock $this

        if $slideBlock
          PersonalCabinet.slideBlock.slideToggle $slideBlock, $this

      $(document).on 'click', '.js-slide-title', (e)->
        $this = $ this
        $btn = $this.find '.js-slide-btn'
        href = $this.attr 'href'
        $slideBlock = PersonalCabinet.slideBlock.getSlideBlock $this

        if $slideBlock
          PersonalCabinet.slideBlock.slideToggle $slideBlock, $btn


  common:

    ##################################################################################################################
    ## Слайдер с фотографиями объекта ##
    ##################################################################################################################
    photoSlider: () ->

      if $().bxSlider
        $ '.js-photo-slider'
        .each ()->
          $this = $ this

          if !$this.data 'sliderInit'
            $this
            .bxSlider(
              auto: true,
              pager: false,
              infiniteLoop: false,
              hideControlOnEnd: true
            )
            .data(
              'sliderInit': true
            )

    ##################################################################################################################
    ## Чекбоксы ##
    ##################################################################################################################
    checkbox: () ->

      ## Отображение рекламных карточек
      $ document
      .on 'change', '.ads-list-block__controls input[type = "checkbox"]', (e)->
        $this = $ this
        lvl = $this.attr 'data-level'
        adId = $this.attr 'data-adid'
        value = $this.is ':checked'


        jsRoutes.controllers.MarketAd.updateShowLevelSubmit(adId).ajax
          type: 'post'
          data:
            levelId: lvl
            levelEnabled: value
          success: (data)->
            console.log data
          error: (error)->
            console.log error


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

      # если есть ошибка внутри контейнера, его нужно отобразить
      $errors = $elements.find '.__error'
      if $errors.size()
        $elements.show()
      else
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

      #$ document
      #.on "blur", "#indexRegForm input[name = email]", (e)->
      #  $this = $ this
      #  $regForm = $ "#indexRegForm"
      #  value = $this.val().trim()

      #  if !value
      #    $hiddenBlock = $regForm.find ".js-hidden"
      #    $hiddenBlock.fadeOut()

      #маска для телефонов
      if $().mask
        $ 'input[mask = tel]'
        .mask '+7 (999) 999-99-99'

      ##выставит ьвысоту textarea
      $ 'textarea'
      .each ()->
        $this = $ this
        scrollHeight = $this.prop 'scrollHeight'
        $this.height scrollHeight

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

      event = if isTouchDevice() then 'touchend' else 'click'

      # клик по событию из списка
      $ document
      .on "click", ".js-event", (e)->
        e.preventDefault()
        $this = $ this

        $this.toggleClass "__act"

      # добавление тектсового блока в редакторе карточки
      $ document
      .on "click", ".js-ad-editor_add-text-field-btn", (e) ->
        e.preventDefault()
        $this = $ this
        href = $this.attr "href"
        $field = $ ".js-ad-editor_field-title"

        index = $field.size()
        height = $("input[name = 'offer.height']").val()
        width = $("input[name = 'offer.width']").val()

        jsRoutes.controllers.MarketAd.newTextField(index, height, width).ajax(
          success: (data) ->
            newHtml = "<div class='edit-ad_block-field __title js-ad-editor_field-title'>#{data}</div>"
            $lastField = $field.filter ":last"
            $lastField.after newHtml

            $newLastField = $ ".js-ad-editor_field-title:last"
            market.init_colorpickers $newLastField
            # вызываем keyup, чтобы обновить превью
            $newLastField.find("textarea").trigger "keyup"
        )

      # выбор цвета для описания в редакторе карточки
      $ document
      .on "click", ".js-color-block", (e) ->
        e.preventDefault()
        $this = $ this
        $colorSelect = $ "#descr_bgColor"
        color = $this.data "color"

        $colorSelect.val color
        $colorSelect.trigger "change"

      $ document
      .on event, '.js-input-btn', (e)->
        e.preventDefault()
        $this = $ this

        if $this.hasClass '__js-act'
          return true

        dataFor = $this.attr 'data-for'
        value = $this.attr 'data-value'

        # присваиваем значение input'у
        $input = $ dataFor
        $input.val value

        # меняем активный элемент
        $ '.js-input-btn.__js-act'
        .removeClass '__js-act'
        $this.addClass '__js-act'

      $ document
      .on event, '.js-stop-bubble', (e)->
        e.stopPropagation()

      $ document
      .on event, '.js-btn', (e)->
        e.preventDefault()
        $this = $ this

        if $this.closest('.js-slide-title').size()
          e.stopPropagation()

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
              .append(data)
              .show()

              cbca.popup.showPopup '#'+popupId
              cbca.pc.common.photoSlider()
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

  init: () ->

    cbca.pc.common.setEqualHeightBlocks()
    cbca.pc.common.setBorderLineHeight()
    cbca.pc.common.hideElements()
    cbca.pc.common.inputs()
    cbca.pc.common.checkbox()
    cbca.pc.common.buttons()
    cbca.pc.common.photoSlider()

    cbca.pc.slideBlock.init()
    cbca.pc.advRequest()

#######################################################################################################################
## Всплывающие окна ##
#######################################################################################################################
CbcaPopup =

  $container: $ '.popups-container'
  $body: $ 'body'

  showOverlay: () ->
    this.$body.addClass 'ovh'
    cbca.popup.$container.css 'visibility', 'visible'
    $ '#popupsContainer'
    .css 'visibility', 'visible'

    $window = $ window
    if $window.width() <= 1024
      cbca.popup.$container.show()

  hideOverlay: () ->
    this.$body.removeClass 'ovh'
    cbca.popup.$container.css 'visibility', 'hidden'
    $ '#popupsContainer'
    .css 'visibility', 'hidden'

    $window = $ window
    if $window.width() <= 1024
      cbca.popup.$container.hide()

  setPopupPosition: (popupSelector) ->
    $window = $ window
    $popup  = $ popupSelector
    ## независимые цифры, подобраны согласно внешнему виду получаемого результата
    minTop  = 25

    if !$popup.size()
      $popup = $ '.popup:visible'

    popupHeight = $popup.height()
    containerHeight = this.$container.height()
    diffHeight = containerHeight - popupHeight


    if diffHeight > minTop*2 && $window.width() > 767
      top = Math.ceil( (containerHeight - popupHeight)/2 )
      $popup.css 'top', top
    else
      $popup.css 'top', minTop

  showPopup: (popupSelector) ->
    popupSelector = popupSelector || '.popup'
    $popup = $ popupSelector
    $popup.show()

    cbca.pc.common.hideElements $popup
    cbca.pc.common.setBorderLineHeight

    $images = $popup.find 'img'

    if $images.size()
      $images.on 'load', () ->
        cbca.popup.showOverlay()
        cbca.popup.setPopupPosition popupSelector
    else
      cbca.popup.showOverlay()
      cbca.popup.setPopupPosition popupSelector

    $window = $ window
    if $window.width() <= 1024
      $window.scrollTop(0)

  phoneScroll: ()->
    $window = $ window

    if $window.width() <= 1024
      windowHeight = $window.height()

      $ '.overflow-scrolling'
      .height windowHeight
      $ '#popupsContainer'
      .css 'min-height', windowHeight+1

  hidePopup: (popupSelector) ->
    popupSelector = popupSelector || '.popup'
    $popup = $ popupSelector

    this.hideOverlay()
    $popup.hide()
    ## закрытие клавиатуры на мобильном устройстве
    if $(window).width() < 1024
      $ "input"
      .blur()

    $ '#overlayData'
    .hide()

  init: () ->

    event = if isTouchDevice() then 'touchend' else 'click'

    cbca.popup.hideOverlay()
    cbca.popup.phoneScroll()

    $ window
    .resize () ->
      cbca.popup.setPopupPosition()
      cbca.popup.phoneScroll()
      $ window
      .scrollTop(0)

    $ document
    .on event, '.js-close-popup', (e)->
      e.preventDefault()
      $this = $ this
      $popup = $this.closest '.popup'
      popupId = $popup.attr 'id'
      popupSelector = '#'+popupId

      cbca.popup.hidePopup popupSelector

    $ document
    .on 'click', '#popups, #popupsContainer', (e)->
      cbca.popup.hidePopup()

    $ document
    .on 'click', '.popup, .js-popup', (e)->
      e.stopPropagation()

    ## Если после перезагрузки страницы в попапе есть поля с ошибками, нужно его отобразить
    $ '.popup .__error, .js-popup .__error, .js-popup .error-msg, .js-popup:visible'
    .each ()->
      $this = $ this
      $popup = $this.closest '.popup'
      popupId = $popup.attr 'id'
      popupSelector = '#'+popupId

      cbca.popup.showPopup popupSelector

    ## Кнопка Назад внутри попапа
    $ document
    .on event, '.js-popup-back', (e)->
      e.preventDefault()
      $this = $ this
      targetPopupHref = $this.attr 'href'
      $targetPopup = $ targetPopupHref

      $this
      .closest '.popup'
      .hide()

      cbca.popup.hidePopup this
      cbca.popup.showPopup targetPopupHref

    $ document
    .on event, '.js-remove-popup', (e)->
      e.preventDefault()
      $this = $ this
      $popup = $this.closest '.js-popup'
      popupId = $popup.attr 'id'
      popupSelector = '#'+popupId

      cbca.popup.hidePopup popupSelector
      $popup.remove()

    ## esc button
    $ document
    .bind 'keyup', (e) ->
      if e.keyCode == 27
        cbca.popup.hidePopup()

    ## фикс отступа, который появляется при скрытии клавиатуры в открытом попапе
    $ document
    .on 'blur', '.js-popup input', (e)->
      setTimeout(
        ()->
          $activeInputs = $ 'input:focus'
          if $activeInputs.size() == 0
            $ window
            .scrollTop(0)
        100
      )


######################
## TODO: отрефакторить
######################
market =
  init_colorpickers : ($parent = false) ->

    if $parent
      $colorPickers = $parent.find ".js-custom-color"
    else
      $colorPickers = $ ".js-custom-color"

    console.log "$colorPickers size = #{$colorPickers.size()}"

    $colorPickers.each () ->

      $this = $ this
      current_value = $this.data "current-value"

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
      	    # если нужно раскрасить не только кнопку с выбором цвета,
      	    # добавляем атрибут data-for, в котором указываем jQuery селектор
      	    if _this.attr 'data-for'
      	      selector = _this.attr 'data-for'
      	      $ ".custom-color-style[data-for = '#{selector}']"
      	      .remove()
      	      style = """
      	                <style class="custom-color-style" data-for="#{selector}">
      	                  #{selector} {
      	                    background-color: ##{hex} !important;
      	                  }
      	                </style>
      	              """
      	      $ 'head'
      	      .append style

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

    crop :

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
            $input = $ 'input[name=\'' + market.img.crop.img_name + '\']'
            $input.val img_data.image_key
            $input
            .parent()
            .find 'img'
            .attr 'src', img_data.image_link
            market.ad_form.queue_block_preview_request request_delay=10

            $('#overlay, #overlayData').hide()
            $('#overlayData').html ''
            $('#imgCropTool').remove()


      init : (img_name) ->
        this.img_name = img_name
        this.crop_tool_dom = $('#imgCropTool')
        this.crop_tool_container_dom = $('.js-crop-container', this.crop_tool_dom)
        this.crop_tool_container_div_dom = $('div', this.crop_tool_container_dom)
        this.crop_tool_img_dom = jQuery('img', this.crop_tool_dom)

        width = parseInt this.crop_tool_dom.attr 'data-width'
        height = parseInt this.crop_tool_dom.attr 'data-height'

        img_width = parseInt this.crop_tool_img_dom.attr 'data-width'
        img_height = parseInt this.crop_tool_img_dom.attr 'data-height'

        this.crop_tool_container_dom.css
          'width' : width + 'px'
          'height' : height + 'px'

        # TODO зарефакторить
        this.crop_tool_dom.find '.js-remove-image'
        .attr 'data-for', img_name

        # отресайзить картинку по нужной стороне
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

        # надо высчитать top у попапа, когда он будет показан
        MIN_TOP  = 25
        $window = $  window

        popupHeight = this.crop_tool_dom.height()
        containerHeight = $window.height()
        diffHeight = containerHeight - popupHeight

        if diffHeight > MIN_TOP*2 && $window.width() > 767
          top = Math.ceil( (containerHeight - popupHeight)/2 )
        else
          top = MIN_TOP

        x1 = this.crop_tool_container_div_dom.offset()['left']
        y1 = this.crop_tool_container_div_dom.offset()['top'] + top

        x2 = x1 + container_offset_x
        y2 = y1 + container_offset_y


        this.crop_tool_img_dom.draggable
          'containment' : [x1,y1,x2,y2]

        ## Забиндить событие на сохранение формы
        $('#imgCropTool form').bind 'submit', () ->
          market.img.crop.save_crop $(this)
          CbcaPopup.hidePopup('#imgCropTool')
          return false

  ################################
  ## Размещение рекламной карточки
  ################################
  adv_form :
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

          if active_nodes != 0
            mt_tab_counter_c.show()
            mt_tab_counter.html active_nodes
          else
            mt_tab_counter_c.hide()
            mt_tab_counter.html 0

    init : () ->

      this.tabs.init()


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
            $('.js-mvbl').draggable
              stop : () ->
                $this = $ this
                connected_input = $this.data "connected-input"
                pos = $this.position()

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

      $ document
      .on "click", ".js-ae-button:not(.__act)", (e) ->
        $this = $ this
        $alignEditor = $this.closest ".js-align-editor"
        $input = $alignEditor.find "input"
        alignValue = $this.data "align"
        $input.val alignValue

        $oldAct = $alignEditor.find ".js-ae-button.__act"
        $oldAct.removeClass "__act"
        $this.addClass "__act"

        market.ad_form.queue_block_preview_request()


      $ document
      .on "change", ".js-custom-font-select, .js-custom-font-family-select", () ->
        market.ad_form.queue_block_preview_request()

      $ document
      .on "keyup", ".js-input-w-block-preview", () ->
        market.ad_form.queue_block_preview_request()

      $ document
      .on "change", ".js-input-w-block-preview", () ->
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

        market.ad_form.queue_block_preview_request request_delay=10

      $('.js-block-width-editor-button').bind 'click', () ->

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

        market.ad_form.queue_block_preview_request request_delay=10

    set_descr_editor_bg : () ->
      hex = '#' + $('#descr_bgColor').val()
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

        submit_cb = () ->
          $('#promoOfferForm').submit()

        setTimeout submit_cb, 1

        return false




      ## Предпросмотр карточки с описанием
      $('.js-ad-preview-button').bind 'click', (e) ->
        e.preventDefault()
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
            $data = $ data
            $data.find(".dotted-pattern").addClass "__popup"
            $smBlock = $data.find ".sm-block"

            mode = ""
            console.log "wide mode = #{$smBlock.data("wide-bg")}"
            if $smBlock.data("wide-bg")
              mode = "__wide"
              $data.find(".focused-ad_offer").addClass "__wide"
              $data.find(".focused-ad_descr").addClass "__wide"

            $ '#adFullPreview'
            .remove()
            $ '#popupsContainer'
            .append "<div class='popup' id='adFullPreview'><div class='popup_header'><a class='close f-right js-close-popup'></a></div><div class='popup_cnt'><div class='sio-mart-showcase #{mode}'>#{data}</div></div></div>"

            cbca.popup.showPopup '#adFullPreview'

        return false

      $(document).on 'change', '#descr_bgColor', (e)->
        market.ad_form.set_descr_editor_bg()

      this.request_block_preview()
      this.init_block_editor()

  init: () ->

    this.ad_form.init()
    $ document
    .ready () ->
      market.mart.init()
      market.adv_form.init()

market.init()
window.market=market
