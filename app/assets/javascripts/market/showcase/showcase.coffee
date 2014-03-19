siomart =

  config :
    css : '/assets/stylesheets/market/showcase.min.css'

  utils :

    ######################
    ## Создать DOM элемент
    ######################
    ce : ( tag, attributes, inhtml ) ->
      ne = document.createElement tag

      for k,v of attributes
        ne.setAttribute k, v

      if( typeof( inhtml ) != 'undefined' )
        ne.innerHTML = inhtml
      ne

    ############################
    ## Найти DOM элемент по тегу
    ############################
    ge_tag : ( tag ) ->
      document.getElementsByTagName tag

    ##############################
    ## Найти DOM элемент/ы по тегу
    ##############################
    ge : () ->
      for e in arguments
        if typeof e == 'string' || typeof e == 'number'
          e = document.getElementById e

        if arguments.length == 1
          return e

        if (!ea)
          ea = new Array()

        ea.push e

      ea

    ##########################################
    ## Является ли переданный объект массивом?
    ##########################################
    is_array : (o) ->
      _t = Object.prototype.toString.call o
      if _t == "[object Array]"
        return true
      else
        return false

    ############################
    ## Удалить класс для объекта
    ############################
    removeClass : (element, value) ->
      element = this.ge element

      if element==null
        return 0

      if !element.className
        element.className = ''
      else
        newClassName = element.className.replace(value,'').replace(/\s{2,}/g, ' ')
        element.className = newClassName

    ############################
    ## Удалить класс для объекта
    ############################
    addClass : (element, value) ->
      element = this.ge element

      if element==null
        return 0

      element.className += ' ' + value

    ############################################
    ## Прицепить собтие(-я) к DOM элементу(-там)
    ############################################
    add_listener: (elt, eventType, listener) ->

      if this.is_array elt
        for s_elt in elt
          this.add_single_listener s_elt, eventType, listener
      else
        this.add_single_listener elt, eventType, listener

    ############################################
    ## Забиндить на ОДИН DOM объект ОДНО событие
    ############################################
    add_single_listener : (elt, eventType, listener) ->

      if elt.addEventListener
        elt.addEventListener eventType, listener, false
      else
        if elt.attachEvent
          elt.attachEvent 'on' + eventType, () ->
            listener.apply elt

    ##############################################
    ## Определить и сохранить размер окна браузера
    ##############################################
    set_window_size : () ->
      ww = wh = 0
      if typeof window.innerWidth == 'number'
        ww = window.innerWidth
        wh = window.innerHeight
      else if document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight )
        ww = document.documentElement.clientWidth
        wh = document.documentElement.clientHeight
      else if document.body && ( document.body.clientWidth || document.body.clientHeight )
        ww = document.body.clientWidth
        wh = document.body.clientHeight

      siomart.ww = ww
      siomart.wh = wh

  ############################
  ## Откадрировать изображения
  ############################

  fit_images : () ->
    images = this.utils.ge_tag 'img'

    for image in images
      if image.className == 'poster-photo'
        this.fit_image image

  fit_image : ( image ) ->

    image_w = parseInt image.getAttribute "data-width"
    image_h = parseInt image.getAttribute "data-height"

    if image_w / image_h < this.ww / this.wh
      nw = this.ww
      nh = nw * image_h / image_w
    else
      nh = this.wh
      nw = nh * image_w / image_h

    image.style.width = nw + 'px'
    image.style.height = nh + 'px'
    image.style.marginLeft = - nw / 2 + 'px'
    image.style.marginTop = - nh / 2 + 'px'

  ####################################
  ## Загрузить все нужные стили цсс'ки
  ####################################
  load_stylesheets : () ->
    stylesheet_attrs =
      type : 'text/css'
      rel : 'stylesheet'
      href : this.config.css

    stylesheet = this.utils.ce "link", stylesheet_attrs
    this.utils.ge_tag("head")[0].appendChild stylesheet

  #####################################################
  ## Добавить в DOM необходимую разметку для Sio.Market
  #####################################################
  draw_layout : () ->
    sm_layout_attrs =
      class : 'sio-mart-layout'
    sm_layout = this.utils.ce "div", sm_layout_attrs, '<div id="sioMartLayout"></div>'

    this.utils.ge_tag("body")[0].appendChild sm_layout

  ###################################
  ## Осуществить запрос к серверу sio
  ###################################
  perform_request : ( url ) ->
    js_request_attrs =
      type : 'text/javascript'
      src : url

    js_request = this.utils.ce "script", js_request_attrs
    this.utils.ge_tag("head")[0].appendChild js_request

  ##################################################
  ## Получить результаты по последнему отправленному
  ## зпросу и передать их в нужный callback
  ##################################################
  receive_response : ( data ) ->
    container = this.utils.ge "sioMartLayout"
    container.innerHTML = data.html

    this.fit_images()
    this.init_navigation()


  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart_index_page : () ->
    this.perform_request '/market/index'

  ######################################################
  ## Открыть экран с предупреждением о выходе из маркета
  ######################################################
  open_close_screen : ( event ) ->
    siomart.utils.ge('smCloseScreen').style.display = 'block'
    event.preventDefault()
    return false

  #########
  ## Офферы
  #########
  offers :

    auto_change_delay : 3000

    ## Инициализация слайдов и кнопок-контроллов
    initialize : () ->

      this.active_offer = 0

      _i = 0
      _offers0 = siomart.utils.ge('smOffers').getElementsByTagName 'div'
      for _o in _offers0
        is_offer = _o.className.match /sm-offer/g
        if siomart.utils.is_array _o.className.match /sm-offer/g
          _o.id = 'smOffer' + _i
          _i++

      _i = 0
      _as = siomart.utils.ge('smOffersController').getElementsByTagName 'a'
      for _a in _as
        _a.setAttribute 'data-index', _i
        _a.id = 'smOfferButton' + _i
        siomart.utils.add_single_listener _a, 'click', ( event ) ->
          event.preventDefault()
          siomart.offers.show_offer this.getAttribute 'data-index'
        _i++

      this.total_offers = _i
      this.next_offer()

    next_offer : () ->
      cb = () ->
        next_offer_index = if siomart.offers.active_offer == siomart.offers.total_offers - 1 then 0 else siomart.offers.active_offer + 1

        siomart.offers.show_offer next_offer_index
        siomart.offers.next_offer()

      setTimeout cb, this.auto_change_delay

    ## Открыть слайд с оффером по указанному индексу
    show_offer : ( index ) ->

      if index == this.active_offer
        return false

      if index > this.active_offer
        direction = 'rtl'
      else
        direction = 'ltr'

      # установить входящий слайд в нужную позицию
      siomart.utils.removeClass 'smOffer' + index, 'sm-hidden-offer'
      siomart.utils.addClass 'smOffer' + index, direction + '-in'

      # включить анимацию для слайдов и перегнать на новые места базирования
      cb = () ->
        siomart.utils.addClass 'smOffer' + index, 'animated'
        siomart.utils.removeClass 'smOffer' + index, direction + '-in'

        siomart.utils.addClass 'smOffer' + siomart.offers.active_offer, direction + '-out animated'

      setTimeout cb, 1

      cb1 = () ->
        #Расставить нужные классы для слайдов
        siomart.utils.removeClass 'smOffer' + siomart.offers.active_offer, 'animated'
        siomart.utils.removeClass 'smOffer' + siomart.offers.active_offer, direction + '-out'
        siomart.utils.addClass 'smOffer' + siomart.offers.active_offer, 'sm-hidden-offer'

        siomart.utils.removeClass 'smOffer' + index, 'animated'

        # выделить активную кнопку
        siomart.utils.removeClass 'smOfferButton' + siomart.offers.active_offer, 'active'
        siomart.utils.addClass 'smOfferButton' + index, 'active'

        siomart.offers.active_offer = index

      setTimeout cb1, 600

  #############################################
  ## Забиндить события на навигационные кнопари
  #############################################
  init_navigation : () ->

    ## Кнопка выхода
    this.utils.add_single_listener this.utils.ge('smCloseButton'), 'click', siomart.open_close_screen

    ## Кнопка возвращения на шаг назад


    ## Кнопка вызова окна с категориями
    this.utils.add_single_listener this.utils.ge('smCategoriesButton'), 'click'

    ## Контроллеры слайдов с офферами
    this.offers.initialize()

  init : () ->
    this.utils.set_window_size()
    this.load_stylesheets()
    this.draw_layout()

    this.load_mart_index_page()

    resize_cb = () ->
      console.log "resize"
      siomart.utils.set_window_size()
      siomart.fit_images()

    this.utils.add_single_listener window, 'resize', resize_cb


window.siomart = siomart

siomart_init_cb = () ->
  siomart.init()

setTimeout siomart_init_cb, 100