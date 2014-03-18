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
      Object.prototype.toString.call o == '[object Array]'

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

  ####################################
  ## Инициировать слайдеры для офферов
  ####################################
  initialize_offers : () ->
    _i = 0
    _as = this.utils.ge('smOffersController').getElementsByTagName 'a'

    for _a in _as
      _a.setAttribute 'data-index', _i
      this.utils.add_single_listener _a, 'click', () ->
        alert this.getAttribute 'data-index'
      _i++

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
    this.initialize_offers()

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