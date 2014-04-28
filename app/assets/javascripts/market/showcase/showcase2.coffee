###############################
## Showcase с поддержкой blocks
###############################
siomart =
  config :
    css : '/assets/stylesheets/market/showcase.min.css'
    index_action : window.siomart_index
    sm_layout_class : 'sio-mart-showcase'
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

    ###########################
    ## Установить vendor_prefix
    ###########################
    set_vendor_prefix : () ->
      styles = window.getComputedStyle(document.documentElement, '')
      pre = (Array.prototype.slice.call(styles).join('').match(/-(moz|webkit|ms)-/) || (styles.OLink == '' && ['', 'o']))[1]

      obj =
        lowercase: pre
        css: '-' + pre + '-'
        js: pre[0].toUpperCase() + pre.substr(1)

      window.vendor_prefix = obj

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

      url = '/market/ads/' + siomart.config.mart_id + '?a.q=' + request + '&a.rcvr=' + siomart.config.mart_id
      siomart.perform_request url

    queue_request : ( event ) ->

      if typeof siomart.search.search_timer != 'undefined'
        clearTimeout siomart.search.search_timer

      search_request = this.value

      search_cb = () ->
        siomart.search.perform search_request

      siomart.search.search_timer = setTimeout search_cb, siomart.search.request_delay

  ####################################
  ## Загрузить все нужные стили цсс'ки
  ####################################
  load_stylesheets : () ->
    stylesheet_attrs =
      type : 'text/css'
      rel : 'stylesheet'
      href : siomart.config.host + this.config.css

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
      src : siomart.config.host + url

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

      cbca_grid.init()

    if data.action == 'findAds'
      screensContainer = siomart.utils.ge 'smScreens'
      screensContainer.innerHTML += data.html

      siomart.utils.ge('smCategoriesScreen').style.display = 'none'


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

  load_for_shop_id : ( shop_id ) ->
    alert 'load for shop_id ' + shop_id
    console.log 'load for shop id ' + shop_id
    url = '/market/ads/' + siomart.config.mart_id + '?a.shopId=' + shop_id
    siomart.perform_request url
    siomart.utils.ge('smCategoriesScreen').style.display = 'none'
    siomart.utils.ge('smShopListScreen').style.display = 'none'

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

  ## Инициализация Sio.Market
  init : () ->

    siomart.config.mart_id = window.siomart_id
    siomart.config.host = window.siomart_host

    this.utils.set_vendor_prefix()
    this.load_stylesheets()
    this.draw_layout()

    this.load_mart_index_page()

    resize_cb = () ->
      cbca_grid.resize()

    this.utils.add_single_listener window, 'resize', resize_cb

window.siomart = siomart
siomart.init()