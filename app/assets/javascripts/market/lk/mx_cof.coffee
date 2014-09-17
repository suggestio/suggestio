$(document).ready ->

  cbca.slider = Slider
  cbca.popup = CbcaPopup
  cbca.pc = PersonalCabinet

  cbca.pc.init()
  cbca.popup.init()
  cbca.slider.init()

isTouchDevice = () ->
  if document.ontouchstart != null
    false
  else
    if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
      false
    else
      true

IndexPage =

  centeredContent: ()->
    minTop = 60
    delta = 100
    $window = $ window
    $cnt = $ '#centerContent'
    winHeight = $window.height()
    cntHeight = $cnt.height()
    diff = winHeight - cntHeight
    top = Math.ceil(diff/2) - delta

    if top < minTop
      top = minTop

    $cnt.css 'padding-top',top

  init: ()->
    IndexPage.centeredContent()
    $window = $ window
    $window.resize ()->
      IndexPage.centeredContent()

IndexPage.init()


Slider =

  $slider    : $ '#indexSlider'
  $window    : $ '.slider_window'
  slideWidth : 560
  itemsCount : 0
  currIndex  : 0
  process    : false
  cardStatus : new Array() ## загружен ли контент для карточки

  open: (index)->
    index = index || 0

    $ '.slider-w'
    .show()
    Slider.$window.show()
    Slider.goToSlide(index)

    $window = $ window
    if $window.width() < 1024
      $window.scrollTop(0)


  close: ()->
    $ '.slider-w'
    .hide()
    Slider.$window.hide()

  updateSlideWidth: ()->
    $window = $ window
    if $window.width() <= 1024
      Slider.slideWidth = $window.width()
    else
      Slider.slideWidth = 560

    Slider.$slider
    .find '.slider_i'
    .width Slider.slideWidth

    Slider.setPhoneScrolling()
    Slider.goToSlide(Slider.currIndex, 0)

  setSliderHeight: ()->
    $window       = $ window
    winHeight     = $window.height()
    itemMaxHeight = Slider.getMaxHeightOfItems()

    itemMaxHeight > winHeight && winHeight = itemMaxHeight+50 ## отсупы от краев
    Slider.$window.height winHeight

    if $window.width() <= 1024
      Slider.$window.height ''

  getMaxHeightOfItems: ()->

    if Slider.itemsCount <= 0
      return false

    maxHeight = 0
    Slider.$window
    .find '.card'
    .each ()->
      $item       = $ this
      itemHeight  = $item.height()

      itemHeight > maxHeight && maxHeight = itemHeight

    return maxHeight

  ## вставляем html вместо прелоадера
  setData: (html, index)->
    $card = Slider.$slider
    .find '.slider_i'
    .eq index

    $card.html html
    Slider.cardStatus[index] = true

    $images = $card.find 'img'

    if $images.size()
      $images.on 'load', () ->
        Slider.setSliderHeight()
        Slider.setCardPosition $card
    else
      Slider.setSliderHeight()
      Slider.setCardPosition $card

  setPhoneScrolling: ()->
    $window = $ window
    windowHeight = $window.height()

    $ '.overflow-scrolling'
    .height windowHeight
    $ '.card-w'
    .css 'min-height', windowHeight+1

  setCardPosition: ($card)->
    $window = $ window
    minTop  = 5
    $card = $card.find '.card, .slider_preloader'
    cardHeight = $card.height()

    containerHeight = $window.height()
    top = Math.ceil( (containerHeight - cardHeight)/2 )

    if top < minTop
      top = minTop
    $card.css 'top', top

  phoneSlide: ()->
    xStart      = 0
    yStart      = 0
    xEnd        = 0
    yEnd        = 0
    gorizontal  = false
    move        = false

    $ document
    .on 'touchstart', '.slider_i', (e)->
      gorizontal = false
      move = false
      xStart = e.originalEvent.touches[0].pageX
      yStart = e.originalEvent.touches[0].pageY

    $ document
    .on 'touchmove', '.slider_i', (e)->
      x = e.originalEvent.touches[0].pageX
      y = e.originalEvent.touches[0].pageY

      yDelta = Math.abs(y - yStart)
      xDelta = x - xStart

      if !move && Math.abs(xDelta) > yDelta
        gorizontal = true

      move = true
      if gorizontal
        e.preventDefault()
        e.stopPropagation()

        animationLength = -Slider.currIndex*Slider.slideWidth + xDelta
        $ '#indexSlider'
        .css 'transition-duration', '0s'
        .css '-webkit-transition-duration', '0s'
        .css 'transform', "translate3d("+animationLength+"px,0,0)"

    $ document
    .on 'touchend', '.slider_i', (e)->
      xEnd = e.originalEvent.changedTouches[0].pageX
      xDelta = xEnd - xStart

      if gorizontal

        if xDelta < 0
          cbca.slider.goToNextSlide()

        if xDelta > 0
          cbca.slider.goToPrevSlide()

        if Math.abs(xDelta) < 50
          Slider.goToSlide(Slider.currIndex, 0.5)

  ## возвращает url для получения содержимого карточки по её номеру
  getLink: (index)->
    $ '#wifiPoints'
    .find '.js-wifi-point'
    .eq index
    .find '.js-card-btn'
    .attr 'data-href'

  ## возвращает содержимое карточки по её номеру
  getData: (index, callback)->
    url = Slider.getLink(index)

    $.ajax(
      url: url,
      success: (data)->
        callback(data)
    )

  updateControls: ()->
    if Slider.currIndex == 0
      $ '.__left-arrow'
      .hide()
    else
      $ '.__left-arrow'
      .show()

    if Slider.currIndex == Slider.itemsCount - 1
      $ '.__right-arrow'
      .hide()
    else
      $ '.__right-arrow'
      .show()

  ## навигация по слайдам
  goToNextSlide: ()->
    newIndex = Slider.currIndex + 1

    if newIndex < Slider.itemsCount
      Slider.goToSlide(newIndex, 0.5)
    else
      Slider.goToSlide(Slider.currIndex, 0.5)

  goToPrevSlide: ()->
    newIndex = Slider.currIndex - 1

    if newIndex >= 0
      Slider.goToSlide(newIndex, 0.5)
    else
      Slider.goToSlide(Slider.currIndex, 0.5)

  goToSlide: (index, duration)->
    duration = duration || 0
    cbca.slider.process = true

    animationLength = -index*Slider.slideWidth

    Slider.currIndex = index
    Slider.updateControls()

    if !Slider.cardStatus[index]
      Slider.getData(
        index
        (data)->
          Slider.setData(data, index)

          $ '#indexSlider'
          .css 'transition-duration', duration+'s'
          .css '-webkit-transition-duration', duration+'s'
          .css 'transform', "translate3d("+animationLength+"px,0,0)"
      )
    else
      $ '#indexSlider'
      .css 'transition-duration', duration+'s'
      .css '-webkit-transition-duration', duration+'s'
      .css 'transform', "translate3d("+animationLength+"px,0,0)"

      $card = Slider.$slider
      .find '.slider_i'
      .eq index
      Slider.setCardPosition $card

  ## добавляем прелоадеры по количеству точек
  setPreloaders: ()->
    html = ''
    $wifiPoints = $ '#wifiPoints'

    if isTouchDevice()
      wrapClass = 'slider_i flex overflow-scrolling'
    else
      wrapClass = 'slider_i flex'

    $wifiPoints.find '.js-wifi-point'
    .each ()->
      html += '<div class="'+wrapClass+'" style="height: 400px;"><div class="slider_preloader"></div></div>'
      Slider.itemsCount += 1
      Slider.cardStatus.push false

    Slider.$slider.append html

    $ '.slider_i'
    .each ()->
      $this = $ this
      Slider.setCardPosition $this
      $this.height ''

  init: ()->

    event = if isTouchDevice() then 'touchend' else 'click'

    cbca.slider.phoneSlide()
    Slider.setPreloaders()
    Slider.updateSlideWidth()
    Slider.setPhoneScrolling()

    $ document
    .on event, '.js-close-slider', (e)->
      e.preventDefault()
      Slider.close()

    $ document
    .on event, '.slider_controls', (e)->
      e.preventDefault()
      e.stopPropagation()

      $target = $ e.target
      targetClass = $target.attr 'class'

      if $target.hasClass '__right-arrow'
        cbca.slider.goToNextSlide()

      if $target.hasClass '__left-arrow'
        cbca.slider.goToPrevSlide()

    ## кнопки точек на главной
    $ document
    .on event, '.js-card-btn', (e)->
      e.preventDefault()
      $this  = $ this
      parent = $this.parent()[0]
      index = $('#wifiPoints').find('.js-wifi-point').index(parent)

      if Slider.cardStatus[index] == true
        Slider.open(index)
      else
        Slider.getData(
          index,
          (data)->
            cbca.popup.hidePopup()

            Slider.open(index)
            Slider.setData(data, index)
        )

    $ document
    .on 'click', '.slider-w', (e)->
      e.preventDefault()
      Slider.close()

    $ document
    .on 'click', '.card', (e)->
      e.stopPropagation()

    $window = $ window
    $window.resize ()->
      Slider.setPhoneScrolling()
      Slider.updateSlideWidth()
      Slider.setSliderHeight()

      if $window.width() < 1024
        $window.scrollTop(0)




###################################################################################################################
## Виджет для работы с датами ##
###################################################################################################################

class DateModel

  # значения start, end хранятся в timestamp
  # interval - в днях
  constructor: (_start = false, _end = false, _interval = false)->
    @start = _start
    @end = _end
    @interval = _interval


class DateController
  MIN_INTERVAL = 1 # задается в днях
  dateModel = false
  _instance = false

  constructor: ()->
    dateModel = new DateModel()
    _instance = this

  start:

    set: (time = false)->
      today = new Date()
      today.getTime()

      if !( time = @validate(time) ) || time < today
        time = today

      dateModel.start = time
      _instance.end.checkForUpdate()
      return true

    get: ()->
      new Date( dateModel.start )

    validate: (time)->
      time = Date.parse time

      if isNaN(time)
        return false

      return time

  end:

    set: (time = false)->
      if !( time = @validate(time) )
        return false

      dateModel.end = time

    get: ()->
      new Date( dateModel.end )

    checkForUpdate: ()->
      if dateModel.start >= dateModel.end || dateModel.interval != 'custom'
        @update()

    update: ()->

      if dateModel.interval == 'custom' || !dateModel.end
        interval = MIN_INTERVAL
      else
        interval = dateModel.interval

      newEnd = new Date(dateModel.start)
      day = newEnd.getDate() + interval
      newEnd.setDate(day)
      dateModel.end = newEnd.getTime()

    validate: (time)->
      time = Date.parse time

      if isNaN(time) || time <= dateModel.start
        return false

      return time

  interval:

    set: (interval = false)->
      if !@validate(interval)
        return false

      dateModel.interval = interval

      if interval != 'custom'
        _instance.start.set() # если интервал не кастомный, размещение с сегодняшнего дня
        _instance.end.checkForUpdate()

      return true

    get: ()->
      dateModel.interval

    validate: (interval)->

      if interval != 'custom'
        interval = parseInt interval

        if isNaN(interval)
          return false

      return true


class DateView
  widgetId = false
  controller = false

  constructor: (_widgetId = false)->
    widgetId = _widgetId
    controller = new DateController()
    @init()

  inputValue: (element, value = false)->
    $input = $ "input[data-#{element}][data-widget-id = #{widgetId}]"
    if value
      $input.val value
    else
      $input.val()

  selectValue: (element, value = false)->
    $select = $ "select[data-#{element}][data-widget-id = #{widgetId}]"
    if value
      $select
      .find "option[val = #{value}]"
      .prop 'selected', 'selected'
    else
      $select
      .find 'option:selected'
      .val()

  intervalToString: (value)->
    switch value
      when 3 then 'P3D'
      when 7 then 'P1W'
      when 30 then 'P1M'
      else 'custom'

  intervalToInt: (value)->
    switch value
      when 'P3D' then 3
      when 'P1W' then 7
      when 'P1M' then 30
      else 'custom'

  getFormattedDate: (time, format = 'default')->
    date = new Date( time )
    rusMonth = ['января','февраля','марта','апреля','мая','июня', 'июля','августа','сентября','октября','ноября','декабря']

    day   = date.getDate()
    fullDay = ("0" + day ).slice(-2)
    month = parseInt( date.getMonth() )
    fullMonth = ("0" + (month + 1)).slice(-2)
    year  = date.getFullYear()

    if format == 'rus'
      "#{day} #{rusMonth[ month ]} #{year}"
    else
      "#{year}-#{fullMonth}-#{fullDay}"

  updateView: ()->
    start = controller.start.get()
    end = controller.end.get()
    interval = @intervalToString( controller.interval.get() )

    @inputValue 'start', @getFormattedDate(start)
    @inputValue 'end', @getFormattedDate(end)

    $ '#advManagementDateStartValue'
    .text @getFormattedDate(start, 'rus')

    $ '#advManagementDateEndValue'
    .text @getFormattedDate(end, 'rus')

    if interval == 'custom'
      $ '#advManagementDateStart, #advManagementDateEnd'
      .show()
    else
      $ '#advManagementDateStart, #advManagementDateEnd'
      .hide()

  init: ()->

    _instance = this
    interval = @intervalToInt( @selectValue('interval') )

    if interval == 'custom'
      start = @inputValue 'start'
      controller.end.set @inputValue 'end'
    else
      start = new Date()

    controller.interval.set interval
    controller.start.set start

    @updateView()

    $ document
    .on 'change', "input[data-start][data-widget-id = #{widgetId}]", (e)->
      $this = $ this
      value = $this.val()

      if !controller.start.set value
        _instance.inputValue 'start', _instance.getFormattedDate( controller.start.get() )
      _instance.updateView()

    $ document
    .on 'change', "input[data-end][data-widget-id = #{widgetId}]", (e)->
      $this = $ this
      value = $this.val()

      if !controller.end.set value
        _instance.inputValue 'end', _instance.getFormattedDate( controller.end.get() )
      _instance.updateView()

    $ document
    .on 'change', "select[data-interval][data-widget-id = #{widgetId}]", (e)->
      $this = $ this
      value = $this.find('option:selected').val()
      value = _instance.intervalToInt value

      if !controller.interval.set value
        interval = _instance.intervalToString( controller.interval.get() )
        _instance.selectValue 'interval', interval
      _instance.updateView()


dateView = new DateView('advManagementDateWidget')




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

  images: ()->

    #фотографии в галерее профиля
    $ document
    .on 'click', '.js-crop-image-btn', (e)->
      e.preventDefault()

      img_key = $('input', $(this).parent()).val()
      img_name = $('input', $(this).parent()).attr 'name'

      if img_key == ''
        alert 'сначала нужно загрузить картинку'
        return false

      width   = 790
      height  = 250

      $.ajax
        url : "/img/crop/#{img_key}?width=#{width}&height=#{height}"
        success : ( data ) ->
          $('#popupsContainer').html data
          CbcaPopup.showPopup()
          market.img.crop.init( img_name )

    # удаление изображения
    $ document
    .on 'click', '.js-remove-image', (e)->
      e.preventDefault()
      $this = $ this
      $preview = $this.closest '.js-preview'

      # находим кнопку для загрузки изображении для этого поля
      $input = $preview.find '.js-image-key'
      name = $input.attr 'name'

      $ ".js-file-upload[data-name = '#{name}']"
      .closest '.js-image-upload'
      .show()

      $preview.remove()
      market.ad_form.queue_block_preview_request()

    #################################################################################################################
    ## Работа с изображениями ##
    #################################################################################################################
    $ document
    .on 'change', '.js-file-upload', (e)->
      e.preventDefault()
      $this = $ this
      formData = new FormData()

      if $this[0].type == 'file'
        formData.append $this[0].name, $this[0].files[0]

      request =
        url : $this.attr "data-action"
        method : 'post'
        data : formData
        contentType: false
        processData: false
        success : ( respData ) ->

          # TODO зарефакторить загрузку изображении в редакторе карточек
          is_w_block_preview = $this.attr 'data-w-block-preview'
          if typeof is_w_block_preview != 'undefined'
            $('#' + $this.attr('data-related-field-id'))
            .find '.js-image-key'
            .val respData.image_key
            market.ad_form.queue_block_preview_request()

          multiple = false
          previewClass = $this.attr 'data-preview-class'

          # dom элемент в который нужно положить превью
          previewRoot = $this.attr 'data-preview-root'
          $previewRoot = $ previewRoot

          # имя поля для загрузки фотографии
          fieldName = $this.attr 'data-name'

          # класс у кнопки редактирования, по умолчанию remove
          editBtnClass = $this.attr('data-edit-btn') || 'image_remove-btn js-remove-image'

          # загрузка одной или нескольких фотографии
          dataMultiple = $this.attr 'data-multiple'
          dataMultiple && multiple = true

          if multiple
            # если уже есть загруженные фотографии
            $previews = $previewRoot.find '.js-preview'
            previewCounts = $previews.length
            fieldName = "#{fieldName}[#{previewCounts}]"

          html =  """
                   <div class="image js-preview #{previewClass}">
                   <input class="js-image-key" type="hidden" name="#{fieldName}" value="#{respData.image_key}"/>
                   <img class="image_src js-image-preview" src="#{respData.image_link}" />
                   <a class="#{editBtnClass}" title="Удалить файл"><span></span></a>
                   </div>
                  """

          $previewRoot.append html

          if !multiple
            $this.parent().hide()
      # загрузка изображения
      $.ajax request

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

      request =
        url: $this.attr 'href'
        success: (data)->
          $transactionsHistory
          .find 'tr:last'
          .after data
        error: (error)->
          console.log error

      $.ajax request

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

    ## удаляем зигзаг там, где он не нужен
    $ '.adv-item .sm-block.height-300, .adv-item .sm-block.height-140'
    .each ()->
      $this = $ this
      $advItem = $this.closest '.adv-item'

      $advItem
      .find '.adv-item_preview-border'
      .remove()


    $ document
    .on 'click', '.ads-list-block__preview_add-new', ()->
      $this = $ this

      $this
      .parent()
      .find('.ads-list-block__link')[0]
      .click()

  ##################################################################################################################
  ## Слайд блоки ##
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

      $(document).on 'click', '.js-slide-btn', (e)->
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

  ##################################################################################################################
  ## Управление рекламой ##
  ##################################################################################################################
  advManagement: ()->
    # TODO зарефакторить
    city = -1
    type = -1

    # показать типы узлов текущего города
    showNodeTypes = ()->
      $ ".js-select-node_w, .js-select-type_w[data-city != #{city}]"
      .hide()
      if city < 0
        return false
      $ ".js-select-type_w[data-city = #{city}]"
      .show()

    # выделить активную вкладку типа
    setActiveType = ()->
      $ ".js-select-type.__js-act"
      .removeClass '__js-act'
      $ ".js-select-type[data-value = #{type}]"
      .addClass '__js-act'

    # показать узлы текущего города и типа
    showNodes = ()->
      if type < 0
        $ ".js-select-node_w[data-city = #{city}]"
        .show()
        return false

      $ ".js-select-node_w[data-type != #{type}]"
      .hide()
      $ ".js-select-node_w[data-city = #{city}][data-type = #{type}]"
      .show()

    # поставить или снять галочки со всех типов
    checkTypes = (value = false)->
      $ ".js-select-type_w[data-city = #{city}] input:checkbox:enabled"
      .prop 'checked', value
      .attr 'value', value

    # поставить или снять галочки со всех узлов текущего города и типа
    checkNodes = (value = false)->
      $ ".js-select-node_w[data-city = #{city}][data-type = #{type}] input:checkbox:enabled"
      .prop 'checked', value
      .attr 'value', value

      if type < 0
        $ ".js-select-node_w[data-city = #{city}] input:checkbox:enabled"
        .prop 'checked', value
        .attr 'value', value

    # проверяет все ли узлы данного типа в текущем городе выбраны и возвращает количество активных узлов + true || false
    allNodesChecked = (_type)->
      activeNodes = 0
      checked = true
      $ ".js-select-node_w[data-city = #{city}][data-type = #{_type}] .js-slide-title input:checkbox:enabled"
      .each ()->
        $this = $ this
        if $this.prop 'checked'
          activeNodes += 1
        else
          checked = false

        return true

      return {
        'activeNodes': activeNodes
        'checked':   checked
      }

    # просматриваем типы узлов текущего города
    typesObserver = ()->
      checked = true
      $ ".js-select-type_w[data-city = #{city}] .js-select-type:gt(0) input:checkbox:enabled"
      .each ()->
        $this = $ this
        if !$this.prop 'checked'
          checked = false
          return false

      $ ".js-select-type_w[data-city = #{city}] .js-select-type:eq(0) input:checkbox:enabled"
      .prop 'checked', checked
      .attr 'value', checked

    # просматриваем галочки у узлов в текущем городе и меняем по ним информацию в типах узлов
    nodesObserver = ()->

      $ ".js-select-type_w[data-city = #{city}] .js-select-type"
      .each ()->
        $this = $ this
        thisType = $this.attr 'data-value'
        if thisType < 0
          return true
        else
          checked = false

          nodesChecked = allNodesChecked(thisType)
          if nodesChecked.activeNodes > 0
            checked = true
            $this
            .filter "[data-value = #{thisType}]"
            .find '.js-active-nodes-count'
            .html "(#{nodesChecked.activeNodes})"
          else
            $this
            .filter "[data-value = #{thisType}]"
            .find '.js-active-nodes-count'
            .html ""

          $this
          .filter "[data-value = #{thisType}]"
          .find 'input:checkbox:enabled'
          .prop 'checked', nodesChecked.checked
          .attr 'value', nodesChecked.checked


    $ document
    .on 'click', '.js-select-city', (e)->
      $this = $ this
      $title = $ '.js-city'
      $slideBlock = $this.closest '.js-slide-cnt'
      cityName = $this.text()
      city = parseInt( $this.attr 'data-value' )

      $title.text cityName
      PersonalCabinet.slideBlock.slideToggle $slideBlock
      showNodeTypes()

    $ document
    .on 'click', '.js-select-type', (e)->
      $this = $ this
      type = $this.attr 'data-value'

      setActiveType()
      showNodes()

    # чекбоксы у типов
    $ document
    .on 'click', '.js-select-type_w input', (e)->
      e.stopPropagation()
      $this = $ this
      type = parseInt( $this.parent().attr 'data-value' )
      checked = $this.prop 'checked'

      setActiveType()
      checkNodes(checked)
      showNodes()
      if type < 0
        checkTypes(checked)
      else
        # снять чекбокс с элемента Все места
        $ ".js-select-type_w[data-city = #{city}] .js-select-type[data-value = '-1'] input"
        .prop 'checked', false
        .attr 'value', false

      nodesObserver()
      typesObserver()

    # чекбоксы у заголовков узлов
    $ document
    .on 'click', '.js-select-node_w .js-slide-title input:checkbox', (e)->
      e.stopPropagation()
      $this = $ this
      checked = $this.prop 'checked'
      $slideWrap = $this.closest '.js-slide-w'

      # управление чекбоксами внутри узла
      $slideWrap.find '.js-slide-cnt input:enabled'
      .prop 'checked', checked
      .attr 'value', checked
      nodesObserver()
      typesObserver()

    # чекбоксы внутри узлов
    $ document
    .on 'click', '.js-select-node_w .js-slide-cnt input', (e)->
      e.stopPropagation()
      $this = $ this
      checked = $this.prop 'checked'
      $slideCnt = $this.closest '.js-slide-cnt'
      $slideWrap = $this.closest '.js-slide-w'

      titleInputChecked = true


      $slideWrap
      .find '.js-slide-title input:checkbox'
      .prop 'checked', titleInputChecked
      .attr 'value', titleInputChecked

      nodesObserver()
      typesObserver()



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

      $ '.lk input[type = "checkbox"]'
      .filter ':enabled'
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

      $ document
      .on event, '.js-stop-bubble', (e)->
        e.stopPropagation()

      $ document
      .on event, '.js-btn', (e)->
        e.preventDefault()
        $this = $ this
        href = $this.attr 'href'


        if $this.closest('.js-slide-title').size()
          e.stopPropagation()

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
    cbca.pc.statusBar.init()
    cbca.pc.login()
    cbca.pc.billing()
    cbca.pc.advRequest()
    cbca.pc.advManagement()
    cbca.pc.adsList()
    cbca.pc.images()

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

    cbca.slider.close()

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

      $ '.js-file-upload___DISABLED'
      .unbind "change"
      .bind "change", (e) ->
        e.preventDefault()
        $this = $ this
        $parent = $this.closest '.js-image-upload'
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

              html = ['<div class="image __preview">',
                      '<input class="js-image-key" type="hidden" name="',
                      fieldName,
                      '" value=""/>',
                      '<img class="image_src js-image-preview" src="" />',
                      '<a class="image_remove-btn siom-remove-image-btn js-remove-image" title="Удалить файл">Удалить</a>',
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
            url : '/img/crop/' + img_key + '?width=' + width + '&height=' + height + '&marker=' + marker # маркер тут вроде не нужен, если удалить ничего не меняется
            success : ( data ) ->
              $('#popupsContainer').html data
              CbcaPopup.showPopup()

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
            $('input[name=\'' + market.img.crop.img_name + '\']').val img_data.image_key
            market.ad_form.queue_block_preview_request request_delay=10

            $('#overlay, #overlayData').hide()
            $('#overlayData').html ''


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


      $('#advsFormBlock input, #advsFormBlock select').bind 'change', () ->
        $this = $ this
        value = $this.attr 'value'

        if $this.is ':checkbox'
          if value == false
            $this.attr 'value', false
          else
            $this.attr 'value', true


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