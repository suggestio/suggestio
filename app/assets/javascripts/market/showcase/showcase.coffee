siomart =

  config :
    css : '/assets/stylesheets/market/showcase.min.css'
    index_action : window.siomart_index
    sm_layout_class : 'sio-mart-layout'
    sm_trigger_class : 'sio-mart-trigger'
    ontouchmove_offer_change_delta : 80
    welcome_ad_hide_timeout : 2500

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

    ge_class : ( parent, _class ) ->

      childs = parent.getElementsByTagName '*'

      _class_match_regexp = new RegExp( _class ,"g")

      elts = []

      for child in childs
        if siomart.utils.is_array child.className.match _class_match_regexp
          elts.push child

      elts

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

    ############################
    ## Удалить DOM элемент по id
    ############################
    re : ( id ) ->
      elt = this.ge id
      elt.parentNode.removeChild elt

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

      siomart_layout = siomart.utils.ge 'sioMartRoot'
      if siomart_layout == null
        return false
      layout_class = 'large'

      if ww < 1024
        layout_class = 'medium'

      if ww < 640
        layout_class = 'small'

      siomart_layout.className = siomart.config.sm_layout_class + ' ' + layout_class

  ########################
  ## Обработка тач событий
  ########################
  touch_events :

    window_touchstart : ( event ) ->
      event.preventDefault()

    touchstart : ( event ) ->
      this.page_x = event.changedTouches[0].pageX

    touchmove : ( event ) ->

      _delta = this.page_x - event.changedTouches[0].pageX

      if _delta > siomart.config.ontouchmove_offer_change_delta
        siomart.screens.objects['smScreen' + siomart.screens.active_screen].next_offer(true)

      if _delta < -siomart.config.ontouchmove_offer_change_delta
        siomart.screens.objects['smScreen' + siomart.screens.active_screen].next_offer(true, true)

  notifications :
    show : ( message ) ->
      n_dom = siomart.utils.ge 'smNotification'
      n_data_dom = siomart.utils.ge 'smNotificationData'

      n_dom.style.display = 'block'
      n_data_dom.innerHTML = message

      hide_cb = () ->
        siomart.notifications.hide()

      setTimeout hide_cb, 1700

    hide : ( message ) ->
      n_dom = siomart.utils.ge 'smNotification'
      n_data_dom = siomart.utils.ge 'smNotificationData'

      n_dom.style.display = 'none'
      n_data_dom.innerHTML = ''

  ########
  ## Поиск
  ########

  search :

    request_delay : 600
    request_timeout : 800

    on_request_error : () ->
      console.log 'request error'
      siomart.notifications.show "НЕ УДАЛОСЬ ВЫПОЛНИТЬ ЗАПРОС, ПОПРОБУЙТЕ ЧЕРЕЗ НЕКОТОРОЕ ВРЕМЯ"
      return false

    perform : ( request ) ->

      timeout_cb = () ->
        siomart.search.on_request_error()

      siomart.search.search_request_timeout_timer = setTimeout timeout_cb, siomart.search.request_timeout

      url = '/market/ads/' + siomart.config.mart_id + '?a.q=' + request
      siomart.perform_request url

    queue_request : ( event ) ->

      if typeof siomart.search.search_timer != 'undefined'
        clearTimeout siomart.search.search_timer

      search_request = this.value

      search_cb = () ->
        siomart.search.perform search_request

      siomart.search.search_timer = setTimeout search_cb, siomart.search.request_delay

  ############################
  ## Откадрировать изображения
  ############################

  fit_images : () ->
    images = this.utils.ge_tag 'img'

    for image in images
      if image.className.match(/poster-photo/ig)
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
    ## Интерфейс маркета
    sm_layout_attrs =
      class : this.config.sm_layout_class
      id : 'sioMartRoot'
    sm_layout = this.utils.ce "div", sm_layout_attrs, '<div id="sioMartLayout"></div>'

    this.utils.ge_tag("body")[0].appendChild sm_layout

    ## Иконка для быстрого вызова маркета
    sm_trigger_attrs =
      class : this.config.sm_trigger_class
      id : 'sioMartTrigger'
    sm_trigger = this.utils.ce "div", sm_trigger_attrs, 'Sio.Market'

    this.utils.ge_tag("body")[0].appendChild sm_trigger
    
    meta_viewport_attrs =
      name : 'viewport'
      content : 'width=320,initial-scale=1,user-scalable=no'
    meta_viewport = this.utils.ce "meta", meta_viewport_attrs
    this.utils.ge_tag('head')[0].appendChild meta_viewport

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

    if data.html == ''
      siomart.notifications.show "КАРТОЧЕК НЕ НАЙДЕНО, ПОПРОБУЙТЕ ДРУГОЙ ЗАПРОС"
      return false

    if data.action == 'martIndex'
      container = this.utils.ge 'sioMartLayout'
      container.innerHTML = data.html

      this.utils.set_window_size()
      this.init_navigation()

    if data.action == 'findAds'
      screensContainer = siomart.utils.ge 'smScreens'
      screensContainer.innerHTML += data.html

      siomart.utils.ge('smCategoriesScreen').style.display = 'none'

    this.screens.init()
    this.screens.show_last()

    this.fit_images()

  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart_index_page : () ->
    this.perform_request siomart.config.index_action

  #########################################
  ## Показать / скрыть экран с категориями и поиском
  #########################################
  open_categories_screen : () ->
    siomart.utils.ge('smCategoriesScreen').style.display = 'block'
    return false

  close_categories_screen : ( event ) ->
    siomart.utils.ge('smCategoriesScreen').style.display = 'none'
    event.preventDefault()
    return false

  #########################################
  ## Показать / скрыть экран со списком магазинов
  #########################################
  open_shopList_screen : ( event ) ->
    siomart.utils.ge('smShopListScreen').style.display = 'block'
    event.preventDefault()
    return false

  close_shopList_screen : ( event ) ->
    siomart.utils.ge('smShopListScreen').style.display = 'none'
    event.preventDefault()
    return false

  ######################################################
  ## Открыть экран с предупреждением о выходе из маркета
  ######################################################
  open_close_screen : ( event ) ->
    siomart.utils.ge('smCloseScreen').style.display = 'block'
    event.preventDefault()
    return false

  exit_close_screen : ( event ) ->
    siomart.utils.ge('smCloseScreen').style.display = 'none'
    event.preventDefault()
    return false

  ###############################
  ## Скрыть / показать sio.market
  ###############################
  close_mart : ( event ) ->
    siomart.utils.ge('sioMartRoot').style.display = 'none'
    siomart.utils.ge('smCloseScreen').style.display = 'none'
    event.preventDefault()
    return false

  open_mart : ( event ) ->
    siomart.utils.ge('sioMartRoot').style.display = 'block'
    event.preventDefault()
    return false

  index_navigation :
    hide : () ->
      _dom = siomart.utils.ge 'smIndexNavigation'
      siomart.utils.addClass _dom, 'hidden'
    show : () ->
      _dom = siomart.utils.ge 'smIndexNavigation'
      siomart.utils.removeClass _dom, 'hidden'

  #########
  ## Скрины
  #########

  screens :
    init : () ->

      this.objects = {}

      divs = siomart.utils.ge('smScreens').getElementsByTagName 'div'
      _i = 0

      for _d in divs
        if siomart.utils.is_array _d.className.match /sm-screen/g
          ## Инициализировать скрин
          _d.id = 'smScreen' + _i

          siomart.screens.objects['smScreen' + _i] = new siomart.screen()
          siomart.screens.objects['smScreen' + _i].screen_id = 'smScreen' + _i
          siomart.screens.objects['smScreen' + _i].screen_container_dom = document.getElementById 'smScreen' + _i
          siomart.screens.objects['smScreen' + _i].initialize_offers()

          this.bind_events _d

          _i++

      this.total_screens = _i-0
      true

    bind_events : ( screen_dom ) ->

      # Exit button
      for elt in siomart.utils.ge_class screen_dom, 'siomart-close-button'
        for _event in ['click', 'touchend']
          siomart.utils.add_single_listener elt, _event, siomart.open_close_screen

      # Back button
      for elt in siomart.utils.ge_class screen_dom, 'siomart-back-button'
        for _event in ['click', 'touchend']
          siomart.utils.add_single_listener elt, _event, siomart.screens.prev_screen


    ## Переключиться на последний экран
    show_last : () ->
      ## Если у нас один экран — значит, он индексный и уже активен
      if this.total_screens == 1
        this.active_screen = 0
        return false

      active_screen_index = this.active_screen
      target_screen_index = parseInt( parseInt( this.total_screens ) - 1 )

      this.animate active_screen_index, target_screen_index

    ## Переключиться на предыдущий экран
    prev_screen : ( event ) ->

      _this = siomart.screens

      active_screen_index = _this.active_screen
      target_screen_index = parseInt( _this.active_screen - 1 )

      _this.animate active_screen_index, target_screen_index
      event.preventDefault()

    animate : ( active_screen_index, target_screen_index ) ->

      if active_screen_index < target_screen_index
        direction = 'forward'
      else
        direction = 'backward'

      # установить входящий слайд в нужную позицию
      siomart.utils.removeClass 'smScreen' + target_screen_index, 'sm-hidden-screen'
      siomart.utils.addClass 'smScreen' + target_screen_index, direction + '-in'

      # включить анимацию для слайдов и перегнать на новые места базирования
      cb = () ->
        siomart.utils.addClass 'smScreen' + target_screen_index, 'animated'
        siomart.utils.removeClass 'smScreen' + target_screen_index, direction + '-in'

        siomart.utils.addClass 'smScreen' + active_screen_index, direction + '-out animated'

      setTimeout cb, 100

      cb1 = () ->

        #Расставить нужные классы для слайдов
        siomart.utils.removeClass 'smScreen' + active_screen_index, 'animated'
        siomart.utils.removeClass 'smScreen' + active_screen_index, direction + '-out'
        siomart.utils.addClass 'smScreen' + active_screen_index, 'sm-hidden-screen'

        siomart.utils.removeClass 'smScreen' + target_screen_index, 'animated'

        if direction == 'backward'
          siomart.utils.re 'smScreen' + siomart.screens.active_screen

        siomart.screens.active_screen = target_screen_index

      setTimeout cb1, 700

  screen : () ->
    this.screen_id = undefined
    this.screen_container_dom = undefined
    this.is_offers_locked = false
    this.is_locked = false
    this.active_offer = 1
    this.total_offers = undefined

  load_for_shop_id : ( shop_id ) ->
    console.log 'load for shop id ' + shop_id
    url = '/market/ads/' + siomart.config.mart_id + '?a.shopId=' + shop_id
    siomart.perform_request url

  ## Загрузить все офферы для магазина
  load_for_cat_id : ( cat_id ) ->
    url = '/market/ads/' + siomart.config.mart_id + '?a.catId=' + cat_id
    siomart.perform_request url
    siomart.utils.ge('smCategoriesScreen').style.display = 'none'

  #############################################
  ## Забиндить события на навигационные кнопари
  #############################################
  init_navigation : () ->

    ## Кнопка выхода
    for _event in ['click', 'touchend']
      this.utils.add_single_listener this.utils.ge('smCloseConfirmedButton'), _event, siomart.close_mart
      this.utils.add_single_listener this.utils.ge('smExitCloseScreenButton'), _event, siomart.exit_close_screen

      this.utils.add_single_listener this.utils.ge('smShopListButton'), _event, siomart.open_shopList_screen
      this.utils.add_single_listener this.utils.ge('smCloseShopListButton'), _event, siomart.close_shopList_screen

      this.utils.add_single_listener this.utils.ge('smCloseCategoriesButton'), _event, siomart.close_categories_screen
      this.utils.add_single_listener this.utils.ge('sioMartTrigger'), _event, siomart.open_mart

      ## поле ввода поискового запроса
      this.utils.add_single_listener this.utils.ge('smSearchField'), 'keyup', siomart.search.queue_request

    ## Тач события
    sm_layout = this.utils.ge('sioMartLayout')

    this.utils.add_single_listener sm_layout, 'touchstart', siomart.touch_events.touchstart
    this.utils.add_single_listener sm_layout, 'touchmove', siomart.touch_events.touchmove

    this.utils.add_single_listener this.utils.ge_tag('body')[0], 'touchstart', siomart.touch_events.window_touchstart

    ## Кнопка возвращения на шаг назад

    ## Кнопка вызова окна с категориями
    this.utils.add_single_listener this.utils.ge('smCategoriesButton'), 'click'

    ## Контроллеры слайдов с офферами
    this.screens.init()

  init : () ->

    this.load_stylesheets()
    this.draw_layout()

    this.utils.set_window_size()
    this.load_mart_index_page()

    siomart.config.mart_id = window.siomart_id

    resize_cb = () ->
      siomart.utils.set_window_size()
      siomart.fit_images()

    this.utils.add_single_listener window, 'resize', resize_cb

    orientation_change_cb = ( event ) ->
      console.log 'window width :' + window
      console.log 'window height :' + window

    this.utils.add_single_listener window, 'orientationchange', orientation_change_cb

    hide_welcome_ad_cb = () ->
      welcome_ad = siomart.utils.ge 'smWelcomeAd'

      welcome_ad.style.display = 'none'

    setTimeout hide_welcome_ad_cb, siomart.config.welcome_ad_hide_timeout

##########################################
## Прототип для работы с офферами в скрине
##########################################
siomart.screen.prototype =

  offer_auto_change_delay : 7000

  clear_auto_change_timer : () ->
    if typeof this.auto_change_timer != 'undefined'
      clearTimeout this.auto_change_timer

  set_auto_change_timer : ( cb ) ->
    this.auto_change_timer = setTimeout cb, this.offer_auto_change_delay

  next_offer : ( is_without_delay, is_backward ) ->

    is_backward = is_backward || false
    is_without_delay = is_without_delay || false

    #this.clear_auto_change_timer()

    _this = this

    cb = () ->
      if is_backward == true
        next_offer_index = if _this.active_offer == 0 then _this.total_offers - 1 else _this.active_offer - 1
      else
        next_offer_index = if _this.active_offer == _this.total_offers - 1 then 0 else _this.active_offer + 1

      _this.show_offer next_offer_index
      #this.next_offer()

    if is_without_delay == true
      cb()
    else
      this.set_auto_change_timer cb

  ## Открыть слайд с оффером по указанному индексу
  show_offer : ( index ) ->

    _t = this

    if this.is_locked == true
      return false

    this.is_locked = true
    index = parseInt index

    if index == this.active_offer
      return false

    _offer =  siomart.utils.ge 'smOffer' + this.screen_id + 'o' + index

    is_mart_offert = _offer.getAttribute 'data-is-mart-offer'
    if ( is_mart_offert == 'true' )
      siomart.index_navigation.hide()
    else
      siomart.index_navigation.show()

    if index > this.active_offer
      direction = 'rtl'
    else
      direction = 'ltr'

    # установить входящий слайд в нужную позицию
    siomart.utils.removeClass 'smOffer' + _t.screen_id + 'o' + index, 'sm-hidden-offer'
    siomart.utils.addClass 'smOffer' + _t.screen_id + 'o' + index, direction + '-in'

    # включить анимацию для слайдов и перегнать на новые места базирования
    cb = () ->
      siomart.utils.addClass 'smOffer' + _t.screen_id + 'o' + index, 'animated'
      siomart.utils.removeClass 'smOffer' + _t.screen_id + 'o' + index, direction + '-in'

      siomart.utils.addClass 'smOffer' + _t.screen_id + 'o' + _t.active_offer, direction + '-out animated'

    setTimeout cb, 100

    cb1 = () ->

      #Расставить нужные классы для слайдов
      siomart.utils.removeClass 'smOffer' + _t.screen_id + 'o' + _t.active_offer, 'animated'
      siomart.utils.removeClass 'smOffer' + _t.screen_id + 'o' + _t.active_offer, direction + '-out'
      siomart.utils.addClass 'smOffer' + _t.screen_id + 'o' + _t.active_offer, 'sm-hidden-offer'

      siomart.utils.removeClass 'smOffer' + _t.screen_id + 'o' + index, 'animated'

      # выделить активную кнопку
      siomart.utils.removeClass 'smOfferButton' + _t.screen_id + 'o' + _t.active_offer, 'active'
      siomart.utils.addClass 'smOfferButton' + _t.screen_id + 'o' + index, 'active'

      _t.active_offer = index
      _t.is_locked = false
    setTimeout cb1, 700

  initialize_offers : () ->

    this.active_offer = 0

    _i = 0
    _offers0 = this.screen_container_dom.getElementsByTagName 'div'

    for _o in _offers0
      is_offer = _o.className.match /sm-sngl-offer/g

      if siomart.utils.is_array _o.className.match /sm-sngl-offer/g
        _o.id = 'smOffer' + this.screen_id + 'o' + _i
        _i++

    _i = 0
    _as = this.screen_container_dom.getElementsByTagName 'a'

    for _a in _as
      if siomart.utils.is_array _a.className.match /sm-offer-control/g
        _a.setAttribute 'data-index', _i
        _a.setAttribute 'data-screen-id', this.screen_id

        _a.id = 'smOfferButton' + this.screen_id + 'o' + _i

        if _a.className.match /active/g
          this.active_offer = _i

        siomart.utils.add_single_listener _a, 'click', ( event ) ->
          event.preventDefault()

          screen_id = this.getAttribute 'data-screen-id'
          index = this.getAttribute 'data-index'
          siomart.screens.objects[screen_id].show_offer index
        _i++

    this.total_offers = _i


window.siomart = siomart

siomart_init_cb = () ->
  siomart.init()

setTimeout siomart_init_cb, 1000