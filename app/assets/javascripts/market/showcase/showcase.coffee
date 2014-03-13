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

    this.ww = ww
    this.wh = wh

  fit_image : () ->

    photo = document.getElementById 'poster-photo'

    image_w = parseInt photo.getAttribute "data-width"
    image_h = parseInt photo.getAttribute "data-height"

    if image_w / image_h < this.ww / this.wh
      nw = this.ww
      nh = nw * image_h / image_w
    else
      nh = this.wh
      nw = nh * image_w / image_h

    photo.style.width = nw + 'px'
    photo.style.height = nh + 'px'
    photo.style.marginLeft = - nw / 2 + 'px'
    photo.style.marginTop = - nh / 2 + 'px'

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
      id : 'sioMartLayout'
    sm_layout = this.utils.ce "div", sm_layout_attrs

    this.utils.ge_tag("body")[0].appendChild sm_layout

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

  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart_index_page : () ->
    this.perform_request '/market/index'


  init : () ->
    this.load_stylesheets()
    this.draw_layout()

    this.load_mart_index_page()


window.siomart = siomart

siomart_init_cb = () ->
  siomart.init()

setTimeout siomart_init_cb, 1500