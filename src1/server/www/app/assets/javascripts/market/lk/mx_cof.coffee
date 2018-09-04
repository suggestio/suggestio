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

