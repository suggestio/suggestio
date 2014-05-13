###############################
## Showcase с поддержкой blocks
###############################
siomart =
  config :
    css : '/assets/stylesheets/market/showcase.min.css?v=33'
    index_action : window.siomart_index
    sm_layout_class : 'sio-mart-showcase'
    sm_trigger_class : 'sio-mart-trigger'
    ontouchmove_offer_change_delta : 80
    welcome_ad_hide_timeout : 2500

  utils :

    is_touch_device : () ->
      if document.ontouchstart != null then false else true


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
        _className = if typeof child.className.baseVal != 'undefined' then child.className.baseVal else child.className
        if typeof _className != 'undefined'
          if siomart.utils.is_array _className.match _class_match_regexp
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
      if !elt || elt == null
        return false

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
      if elt == null
        return false

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

  events :

    touchmove_lock_delta : 2
    is_touch_locked : false

    document_touchmove : ( event ) ->

      cx = event.touches[0].pageX
      cy = event.touches[0].pageY

      if typeof siomart.events.document_touch_x == 'undefined'
        siomart.events.document_touch_x = cx
        siomart.events.document_touch_y = cy

      siomart.events.document_touch_x_delta = siomart.events.document_touch_x - cx
      siomart.events.document_touch_y_delta = siomart.events.document_touch_y - cy

      if Math.abs( siomart.events.document_touch_x_delta ) > siomart.events.touchmove_lock_delta || Math.abs( siomart.events.document_touch_y_delta ) > siomart.events.touchmove_lock_delta
        siomart.events.is_touch_locked = true

    document_touchend : ( event ) ->

      cb = () ->
        siomart.events.is_touch_locked = false
        delete siomart.events.document_touch_x
        delete siomart.events.document_touch_y

      setTimeout cb, 100

    document_keyup_event : ( event ) ->

      if !event
        return false

      ## Exc button
      if event.keyCode == 27
        siomart.close_node_offers_popup()
        siomart.navigation_layer.back()

      if event.keyCode == 39
        siomart.node_offers_popup.next_block()

      if event.keyCode == 37
        siomart.node_offers_popup.prev_block()


  notifications :
    show : ( message ) ->
      n_dom = siomart.utils.ge 'smNotification'
      n_data_dom = siomart.utils.ge 'smNotificationData'

      n_dom.style.display = 'block'
      n_data_dom.innerHTML = message

      hide_cb = () ->
        siomart.notifications.hide()

      setTimeout hide_cb, 1200

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
    
    perform : ( request ) ->
      url = '/market/ads/' + siomart.config.mart_id + '?a.q=' + request + '&a.rcvr=' + siomart.config.mart_id
      siomart.request.perform url

    queue_request : ( request ) ->

      if typeof siomart.search.search_timer != 'undefined'
        clearTimeout siomart.search.search_timer

      search_cb = () ->
        siomart.search.perform request.toLowerCase()

      siomart.search.search_timer = setTimeout search_cb, siomart.search.request_delay

  ## Карточки ноды верхнего уровня
  load_index_ads : () ->
    url = '/market/ads/' + siomart.config.mart_id + '?a.rcvr=' + siomart.config.mart_id
    siomart.request.perform url

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
  request :
    request_timeout : 2000

    on_request_error : () ->
      siomart.notifications.show "НЕ УДАЛОСЬ ВЫПОЛНИТЬ ЗАПРОС"

    perform : ( url ) ->

      timeout_cb = () ->
        siomart.request.on_request_error()

      siomart.request.request_timeout_timer = setTimeout timeout_cb, siomart.request.request_timeout

      js_request_attrs =
        type : 'text/javascript'
        src : siomart.config.host + url
      js_request = siomart.utils.ce "script", js_request_attrs
      siomart.utils.ge_tag("head")[0].appendChild js_request
  ##################################################
  ## Получить результаты по последнему отправленному
  ## зпросу и передать их в нужный callback
  ##################################################
  receive_response : ( data ) ->
    if typeof siomart.request.request_timeout_timer != 'undefined'
      clearTimeout siomart.request.request_timeout_timer

    if data.html == ''
      siomart.notifications.show "КАРТОЧЕК НЕ НАЙДЕНО, ПОПРОБУЙТЕ ДРУГОЙ ЗАПРОС"
      return false

    if data.action == 'martIndex'
      container = this.utils.ge 'sioMartLayout'
      container.innerHTML = data.html

      cbca_grid.init()

      siomart.welcome_ad.init()
      siomart.init_navigation()

      siomart.set_window_class()

    if data.action == 'producerAds'
      screensContainer = siomart.utils.ge 'sioMartNodeOffersRoot'
      screensContainer.innerHTML += data.html
      screensContainer.style.display = 'block'

      siomart.utils.ge('smCategoriesScreen').style.display = 'none'
      siomart.node_offers_popup.init()
      siomart.navigation_layer.close()

    if data.action == 'findAds' || data.action == 'searchAds'
      grid_container_dom = siomart.utils.ge 'sioMartIndexGrid'

      grid_container_dom.innerHTML = data.html
      cbca_grid.init()
      siomart.init_shop_links()

      if data.action == 'searchAds'
        siomart.navigation_layer.close true
      else
        siomart.navigation_layer.close()

  close_node_offers_popup : ( event ) ->

    siomart.utils.re 'sioMartNodeOffers'
    siomart.utils.ge('sioMartNodeOffersRoot').style.display = 'none'
    delete siomart.node_offers_popup.requested_ad_id
    delete siomart.node_offers_popup.active_block_index

    if event
      event.preventDefault()

  node_offers_popup :

    nav_pointer_size : 14

    show_block_by_index : ( block_index ) ->
      this.active_block_dom.style.display = 'none'
      siomart.utils.removeClass this.active_block_dom, 'double-size'

      this.show_block this.sm_blocks[block_index]

      siomart.utils.removeClass siomart.utils.ge('smNodeOffersNavPointer' + this.active_block_index), 'sm-nav-block__pointer_active'
      this.active_block_index = block_index
      siomart.utils.addClass siomart.utils.ge('smNodeOffersNavPointer' + this.active_block_index), 'sm-nav-block__pointer_active'

    show_block : ( sm_block ) ->

      sm_block.style.opacity = 0
      sm_block.style.display = 'block'
      this.active_block_dom = sm_block

      cw = sm_block.offsetWidth
      ch = sm_block.offsetHeight

      if cbca_grid.ww > 600
        #sm_block.style.width = cw*2 + 'px'
        #sm_block.style.height = ch*2 + 'px'
        siomart.utils.addClass sm_block, 'double-size'

      if cbca_grid.ww > 600
        this._block_container.style.width = cw*2 + 'px'
      else
        this._block_container.style.width = cw + 'px'

      sm_block.style.opacity = 1

    next_block : () ->
      if typeof this.active_block_index == 'undefined'
        return false

      if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
        return false

      next_index = this.active_block_index + 1

      if next_index == this.sm_blocks.length
        next_index = 0
      this.show_block_by_index next_index

    prev_block : () ->
      if typeof this.active_block_index == 'undefined'
        return false

      if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
        return false

      prev_index = this.active_block_index - 1

      if prev_index < 0
        prev_index = this.sm_blocks.length - 1

      this.show_block_by_index prev_index

    init : () ->

      this._block_container = siomart.utils.ge('sioMartNodeOffersBlockContainer')
      this._container = siomart.utils.ge('sioMartNodeOffers')

      this._container_nav = siomart.utils.ge_class this._container, 'js-popup-nav'
      this._container_nav = this._container_nav[0]

      this.sm_blocks = sm_blocks = siomart.utils.ge_class this._container, 'sm-block'
      nav_pointers_html = ''

      for sm_block,i in sm_blocks

        mad_id = sm_block.getAttribute 'data-mad-id'

        if ( typeof this.requested_ad_id == 'undefined' && i == 0 ) || mad_id == this.requested_ad_id
          this.show_block sm_block
          this.active_block_dom = sm_block
          this.active_block_index = i

        if ( typeof this.requested_ad_id == 'undefined' && i == 0 ) || mad_id == this.requested_ad_id
          _nav_pointer_class = 'sm-nav-block__pointer sm-nav-block__pointer_active'
        else if i == sm_blocks.length-1
          _nav_pointer_class = 'sm-nav-block__pointer sm-nav-block__pointer_no-margin'
        else
          _nav_pointer_class = 'sm-nav-block__pointer'

        nav_pointers_html += '<div id="smNodeOffersNavPointer' + i + '" onclick="siomart.node_offers_popup.show_block_by_index(\'' + i + '\');" class="' + _nav_pointer_class + '"><i></i></div>'

      this._container_nav.innerHTML = nav_pointers_html
      this._container_nav.style.width = this.nav_pointer_size * sm_blocks.length + 'px'

      ## События
      _e = if siomart.utils.is_touch_device() then 'touchend' else 'click'

      ## Кнопка возврата на главный экран
      siomart.utils.add_single_listener siomart.utils.ge('closeNodeOffersPopupButton'), _e, siomart.close_node_offers_popup

      ## Переход к следующему блоку при клике на текущий
      siomart.utils.add_single_listener siomart.utils.ge('sioMartNodeOffersBlockContainer'), _e, () ->
        siomart.node_offers_popup.next_block()

  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart_index_page : () ->
    this.request.perform siomart.config.index_action

  ##################################################
  ## Показать / скрыть экран с категориями и поиском
  ##################################################
  navigation_layer :
    open : () ->
      siomart.utils.ge('smCategoriesScreen').style.display = 'block'
      siomart.utils.ge('smSearchBar').style.display = 'block'

    close : ( all_except_search ) ->
      siomart.utils.ge('smCategoriesScreen').style.display = 'none'
      siomart.utils.ge('smShopListScreen').style.display = 'none'

      if all_except_search != true
        siomart.utils.ge('smSearchBar').style.display = 'none'


    back : () ->
      shop_list_screen_dom = siomart.utils.ge('smShopListScreen')

      if shop_list_screen_dom.style.display == 'block'
        shop_list_screen_dom.style.display = 'none'
      else
        siomart.navigation_layer.close()


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

  load_for_shop_id : ( shop_id, ad_id ) ->

    if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
      return false

    url = '/market/ads/' + siomart.config.mart_id + '?a.shopId=' + shop_id

    siomart.node_offers_popup.requested_ad_id = ad_id
    siomart.request.perform url

  ## Загрузить все офферы для магазина
  load_for_cat_id : ( cat_id ) ->

    if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
      return false

    url = '/market/ads/' + siomart.config.mart_id + '?a.catId=' + cat_id
    siomart.request.perform url

  ########################################
  ## картинка приветствия торгового центра
  ########################################
  welcome_ad :

    hide_timeout : 2000
    fadeout_transition_time : 1000

    fit : ( image_dom ) ->
      image_w = parseInt image_dom.getAttribute "data-width"
      image_h = parseInt image_dom.getAttribute "data-height"

      if image_w / image_h < cbca_grid.ww / cbca_grid.wh
        nw = cbca_grid.ww
        nh = nw * image_h / image_w
      else
        nh = cbca_grid.wh
        nw = nh * image_w / image_h

      image_dom.style.width = nw + 'px'
      image_dom.style.height = nh + 'px'
      image_dom.style.marginLeft = - nw / 2 + 'px'
      image_dom.style.marginTop = - nh / 2 + 'px'

    hide : () ->

      siomart.utils.addClass siomart.welcome_ad.img_dom, 'sm-welcome-ad__img_fade-out'

      dn_cb = () ->
        siomart.welcome_ad.img_dom.style.display = 'none'

      setTimeout dn_cb, siomart.welcome_ad.fadeout_transition_time

    init : () ->
      this.img_dom = siomart.utils.ge 'smWelcomeAd'
      this.fit this.img_dom

      setTimeout siomart.welcome_ad.hide, this.hide_timeout


  ##################################################
  ## Забиндить события на навигационные кнопари
  ## Вызывается только при инициализации marketIndex
  ##################################################
  init_navigation : () ->

    siomart.utils.add_single_listener window, 'touchmove', siomart.events.document_touchmove
    siomart.utils.add_single_listener window, 'touchend', siomart.events.document_touchend

    ## Кнопка выхода
    siomart.utils.add_single_listener document, 'keyup', siomart.events.document_keyup_event

    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'

    siomart.utils.add_single_listener this.utils.ge('smCloseButton'), _event, siomart.open_close_screen

    this.utils.add_single_listener this.utils.ge('smCloseConfirmedButton'), _event, siomart.close_mart
    this.utils.add_single_listener this.utils.ge('smExitCloseScreenButton'), _event, siomart.exit_close_screen

    this.utils.add_single_listener this.utils.ge('smShopListButton'), _event, siomart.open_shopList_screen

    this.utils.add_single_listener this.utils.ge('sioMartTrigger'), _event, siomart.open_mart

    ## поле ввода поискового запроса
    this.utils.add_single_listener this.utils.ge('smSearchField'), 'keyup', () ->
      this.value = this.value.toUpperCase()
      siomart.search.queue_request this.value

    ## Кнопка вызова окна с категориями
    this.utils.add_single_listener this.utils.ge('smCategoriesButton'), _event, siomart.navigation_layer.open

    this.utils.add_single_listener this.utils.ge('smNavigationLayerBackButton'), _event, siomart.navigation_layer.back


    ## Возврат на индекс выдачи
    this.utils.add_single_listener this.utils.ge('rootNodeLogo'), _event, siomart.load_index_ads

    this.utils.add_single_listener this.utils.ge('smSearchIcon'), _event, () ->
      siomart.utils.ge('smSearchField').focus()

    this.init_shop_links()

  init_shop_links : () ->

    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'
    blocks_w_actions = siomart.utils.ge_class document, 'js-shop-link'
    for _b in blocks_w_actions
      cb = ( b ) ->
        producer_id = b.getAttribute 'data-producer-id'
        ad_id = b.getAttribute 'data-ad-id'
        siomart.utils.add_single_listener b, _event, () ->
          console.log producer_id
          siomart.load_for_shop_id producer_id, ad_id
      cb _b

  set_window_class : () ->

    _window_class = ''

    if cbca_grid.ww <= 980
      _window_class = 'sm-w-980'

    if cbca_grid.ww <= 800
      _window_class = 'sm-w-800'

    if cbca_grid.ww <= 620
      _window_class = 'sm-w-400'

    siomart.utils.ge('sioMartLayout').className = _window_class

  ## Инициализация Sio.Market
  init : () ->

    this.utils.is_touch_device()

    siomart.config.mart_id = window.siomart_id
    siomart.config.host = window.siomart_host

    this.utils.set_vendor_prefix()
    this.load_stylesheets()
    this.draw_layout()

    this.load_mart_index_page()

    resize_cb = () ->

      if typeof siomart.window_resize_timer != 'undefined'
        clearTimeout siomart.window_resize_timer

      grid_resize = () ->
        cbca_grid.resize()
        siomart.set_window_class()

      siomart.window_resize_timer = setTimeout grid_resize, 300

    this.utils.add_single_listener window, 'resize', resize_cb

window.siomart = siomart
siomart.init()