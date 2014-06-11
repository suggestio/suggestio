###############################
## Showcase с поддержкой blocks
###############################
siomart =
  config :
    css : '/assets/stylesheets/market/showcase.min.css?v=35'
    index_action : window.siomart_index
    sm_layout_class : 'sio-mart-showcase'
    sm_trigger_class : 'sio-mart-trigger'
    ontouchmove_offer_change_delta : 80
    welcome_ad_hide_timeout : 2000

  ## Загрузить js- и css- засимости
  load_deps : () ->

    ## js : cbca_grid
    grid_js_attrs =
      type : 'text/javascript'
      src : siomart.config.host + '/assets/javascripts/market/showcase/grid.min.js'
    gid_js = this.utils.ce "script", grid_js_attrs

    ## css : showcase.css
    stylesheet_attrs =
      type : 'text/css'
      rel : 'stylesheet'
      href : siomart.config.host + this.config.css

    stylesheet = this.utils.ce "link", stylesheet_attrs

    ## Созданные элементы добавляем в head
    _head = this.utils.ge_tag("head")[0]

    _head.appendChild gid_js
    _head.appendChild stylesheet

  ## Забиндить оконные события
  bind_window_events : () ->
    resize_cb = () ->
      if typeof siomart.window_resize_timer != 'undefined'
        clearTimeout siomart.window_resize_timer

      grid_resize = () ->
        cbca_grid.resize()
        siomart.set_window_class()

        siomart.node_offers_popup.fit()
        siomart.node_offers_popup.show_block_by_index siomart.node_offers_popup.active_block_index

      siomart.window_resize_timer = setTimeout grid_resize, 300

    this.utils.add_single_listener window, 'resize', resize_cb

  ########
  ## Утиль
  ########
  utils :
    elts_cache : {}
    is_firefox : () ->
      navigator.userAgent.toLowerCase().indexOf('firefox') > -1

    is_touch_device : () ->
      if document.ontouchstart != null
        false
      else
        if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          false
        else
          true

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
    ge_tag : ( tag, force_no_cache ) ->
      force_no_cache = force_no_cache || false

      if force_no_cache != true && typeof this.elts_cache[tag] != 'undefined'
        console.log 'ge_tag ' + tag + ' : cached'
        this.elts_cache[tag]
      else
        console.log 'ge_tag ' + tag + ' : not cached'
        _elt = document.getElementsByTagName tag
        this.elts_cache[tag] = _elt
        _elt

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

          if typeof this.elts_cache[e] != 'undefined'
            e = this.elts_cache[e]
          else
            e = document.getElementById e
            this.elts_cache[e] = e

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
      styles = window.getComputedStyle document.documentElement, ''
      pre = (Array.prototype.slice.call(styles).join('').match(/-(moz|webkit|ms)-/) || (styles.OLink == '' && ['', 'o']))[1]

      obj =
        lowercase: pre
        css: '-'.concat( pre, '-')
        js: pre[0].toUpperCase().concat( pre.substr(1) )

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
      url = '/market/ads?a.q=' + request + '&a.rcvr=' + siomart.config.mart_id
      siomart.request.perform url

    queue_request : ( request ) ->

      if typeof siomart.search.search_timer != 'undefined'
        clearTimeout siomart.search.search_timer

      search_cb = () ->
        siomart.search.perform request.toLowerCase()

      siomart.search.search_timer = setTimeout search_cb, siomart.search.request_delay

  ## Карточки ноды верхнего уровня
  load_index_ads : () ->
    url = '/market/ads?a.rcvr=' + siomart.config.mart_id
    siomart.request.perform url

  #####################################################
  ## Добавить в DOM необходимую разметку для Sio.Market
  #####################################################
  draw_layout : () ->

    ## Иконка для быстрого вызова маркета
    sm_trigger_attrs =
      class : this.config.sm_trigger_class
      id : 'sioMartTrigger'
      style : 'background-color: #' + window.siomart_node_color
    sm_trigger = this.utils.ce 'div', sm_trigger_attrs, '<span class="trigger-helper"></span><img src=\'' + window.siomart_logo_src + '\' width=80/>'

    ## Интерфейс маркета
    sm_layout_attrs =
      class : this.config.sm_layout_class
      id : 'sioMartRoot'
    sm_layout = this.utils.ce "div", sm_layout_attrs, '<div id="sioMartLayout"></div>'

    _body = this.utils.ge_tag('body')[0]
    _body.appendChild sm_trigger
    _body.appendChild sm_layout

    _head = this.utils.ge_tag('head')[0]
    meta_viewport_attrs =
      name : 'viewport'
      content : 'width=320,initial-scale=1,user-scalable=no,minimal-ui'
    meta_viewport = this.utils.ce "meta", meta_viewport_attrs
    _head.appendChild meta_viewport

  ###################################
  ## Осуществить запрос к серверу sio
  ###################################
  request :
    request_timeout : 5000

    ## Обработать ошибку
    on_request_error : () ->
      siomart.notifications.show "НЕ УДАЛОСЬ ВЫПОЛНИТЬ ЗАПРОС"
      _sm_loading_dom = siomart.utils.ge('smLoading')

      if _sm_loading_dom != null
        _sm_loading_dom.style.display = 'none'

    ## Выполнить запрос по указанному url
    perform : ( url ) ->

      _sm_loading_dom = siomart.utils.ge('smLoading')

      if _sm_loading_dom != null
        _sm_loading_dom.style.display = 'block'

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
    console.log 'receive response, data : '
    console.log data

    if typeof siomart.request.request_timeout_timer != 'undefined'
      clearTimeout siomart.request.request_timeout_timer

    ## Пришла пустота — уведомить горемыку юзера
    if data.html == ''
      siomart.notifications.show "КАРТОЧЕК НЕ НАЙДЕНО, ПОПРОБУЙТЕ ДРУГОЙ ЗАПРОС"
      if siomart.utils.ge('smLoading') != null
        siomart.utils.ge('smLoading').style.display = 'none'
      return false

    ## Инициализация глагне
    if data.action == 'martIndex'
      cbca_grid.set_window_size()
      container = this.utils.ge 'sioMartLayout'
      container.innerHTML = data.html
      document.getElementById('sioMartIndexOffers').scrollTop = '0';
      siomart.utils.ge_tag('body')[0].style.overflow = 'hidden'

      ## Инициализация welcome_ad
      ## если возвращается false — значит картинки нет и
      ## нет смысла тянуть с дальнейшей инициализацией

      if siomart.welcome_ad.init() == false
        grid_init_timoeut = 1
      else
        grid_init_timoeut = siomart.welcome_ad.hide_timeout - 100

      grid_init_cb = () ->
        document.body.style.backgroundColor = '#ffffff'
        siomart.utils.ge('sioMartRoot').style.backgroundColor = "#ffffff"

        sm_wifi_info_dom = siomart.utils.ge('smWifiInfo')
        if sm_wifi_info_dom != null
          siomart.utils.ge('smWifiInfo').style.display = 'block'

        siomart.init_navigation()
        cbca_grid.init()

      setTimeout grid_init_cb, grid_init_timoeut
      siomart.set_window_class()

    if data.action == 'producerAds'
      screensContainer = siomart.utils.ge 'sioMartNodeOffersRoot'
      screensContainer.innerHTML += data.html
      screensContainer.style.display = 'block'

      cb = () ->
        siomart.utils.addClass screensContainer, 'sio-mart__node-offers-root_in'

      setTimeout cb, 10

      siomart.utils.ge('smCategoriesScreen').style.display = 'none'
      siomart.node_offers_popup.init()
      siomart.navigation_layer.close()

    if data.action == 'findAds' || data.action == 'searchAds'
      grid_container_dom = siomart.utils.ge 'sioMartIndexGrid'

      grid_container_dom.innerHTML = data.html
      document.getElementById('sioMartIndexOffers').scrollTop = '0';
      cbca_grid.init()
      siomart.init_shop_links()

      if data.action == 'searchAds'
        siomart.navigation_layer.close true
      else
        siomart.navigation_layer.close()

    siomart.utils.ge('smLoading').style.display = 'none'

  close_node_offers_popup : ( event ) ->
    siomart.utils.removeClass siomart.utils.ge('sioMartNodeOffersRoot'), 'sio-mart__node-offers-root_in'

    cb = () ->
      siomart.utils.re 'sioMartNodeOffers'
      siomart.utils.ge('sioMartNodeOffersRoot').style.display = 'none'

      delete siomart.shop_load_locked
      console.log 'unlock'

    setTimeout cb, 400

    delete siomart.node_offers_popup.requested_ad_id
    delete siomart.node_offers_popup.active_block_index

    if event
      event.preventDefault()

  node_offers_popup :

    nav_pointer_size : 14
    scroll_or_move : undefined

    show_block_by_index : ( block_index, direction ) ->

      if typeof this.sm_blocks == 'undefined'
        return false

      if vendor_prefix.js == 'Webkit'
        siomart.node_offers_popup._block_container.style['-webkit-transform'] = 'translate3d(-' + cbca_grid.ww*block_index + 'px, 0px, 0px)'
      else
        siomart.node_offers_popup._block_container.style['transform'] = 'translate3d(-' + cbca_grid.ww*block_index + 'px, 0px, 0px)'

      siomart.node_offers_popup._block_container.setAttribute 'data-x-offset', -cbca_grid.ww*block_index

      if block_index == this.active_block_index
        return false

      this.active_block_index = block_index

      if direction == '+'
        siomart.utils.ge('sioMartNodeOffers_' + ( block_index + 1 ) ).style.visibility = 'visible';

        if block_index >= 2
          siomart.utils.ge('sioMartNodeOffers_' + ( block_index - 2 ) ).style.visibility = 'hidden';

      if direction == '-'
        if block_index >= 1
          siomart.utils.ge('sioMartNodeOffers_' + ( block_index - 1 ) ).style.visibility = 'visible';

        siomart.utils.ge('sioMartNodeOffers_' + ( block_index + 2 ) ).style.visibility = 'hidden';

    next_block : () ->
      console.log 'next_block'

      if typeof this.active_block_index == 'undefined'
        return false

      next_index = this.active_block_index + 1

      if next_index == this.sm_blocks.length
        next_index = next_index-1
      this.show_block_by_index next_index, '+'

    prev_block : () ->

      if typeof this.active_block_index == 'undefined'
        return false

      prev_index = this.active_block_index - 1
      if prev_index < 0
        prev_index = 0

      this.show_block_by_index prev_index, '-'

    fit : () ->

      if typeof this.sm_blocks == 'undefined'
        return false

      for _b in this.sm_blocks
        _block_width = _b.getAttribute 'data-width'

        if cbca_grid.ww >= 660
          siomart.utils.addClass _b, 'double-size'
        else
          siomart.utils.removeClass _b, 'double-size'

        console.log _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode
        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.width = cbca_grid.ww + 'px'

        _b.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 'px'

        if cbca_grid.ww >= 660
          _b.parentNode.parentNode.parentNode.style.width = _block_width*2 + 11*2 + 'px'
        else
          _b.parentNode.parentNode.parentNode.style.width = _block_width + 'px'

      this._block_container.style.width = this.sm_blocks.length * cbca_grid.ww + 'px'

    ###############################
    ## События для обработки свайпа
    ###############################

    touchstart_event : ( event ) ->

      ex = event.touches[0].pageX
      ey = event.touches[0].pageY

      this.tstart_x = ex
      this.tstart_y = ey

      this.last_x = ex

      siomart.utils.removeClass this._block_container, 'sio-mart-node-offers-window__root-container_animated'

    touchmove_event : ( event ) ->

      ex = event.touches[0].pageX
      ey = event.touches[0].pageY

      delta_x = this.tstart_x - ex
      delta_y = this.tstart_y - ey

      if typeof siomart.node_offers_popup.scroll_or_move == 'undefined' && !( delta_x == 0 && delta_y == 0 )

        if Math.abs( delta_y ) > Math.abs( delta_x )
          siomart.node_offers_popup.scroll_or_move = 'scroll'
        else
          siomart.node_offers_popup.scroll_or_move = 'move'

      if siomart.node_offers_popup.scroll_or_move == 'scroll'
        return false
      else
        event.preventDefault()

      c_x_offset = siomart.node_offers_popup._block_container.getAttribute 'data-x-offset'
      c_x_offset = parseInt c_x_offset

      console.log 'touchmove'

      if vendor_prefix.js == 'Webkit'
        siomart.node_offers_popup._block_container.style['-webkit-transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'
      else
        siomart.node_offers_popup._block_container.style['transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'

      this.x_delta_direction = this.last_x - ex

      this.last_x = ex

    touchend_event : ( event ) ->
      console.log 'touchstart'
      siomart.utils.addClass this._block_container, 'sio-mart-node-offers-window__root-container_animated'

      delete siomart.node_offers_popup.tstart_x
      delete siomart.node_offers_popup.tstart_y

      if this.x_delta_direction > 0
        cb = () ->
          siomart.node_offers_popup.next_block()
      else
        cb = () ->
          siomart.node_offers_popup.prev_block()

      if this.scroll_or_move == 'move'
        setTimeout cb, 1

      delete siomart.node_offers_popup.scroll_or_move


    init : () ->

      this.root
      this._block_container = siomart.utils.ge('sioMartNodeOffersBlockContainer')
      this._container = siomart.utils.ge('sioMartNodeOffers')
      
      this.sm_blocks = sm_blocks = siomart.utils.ge_class this._container, 'sm-block'

      this.fit()
      i = 0

      this.active_block_index = 0

      siomart.utils.addClass this._block_container, 'sio-mart-node-offers-window__root-container_animated'

      ## События
      _e = if siomart.utils.is_touch_device() then 'touchend' else 'click'

      ## Кнопка возврата на главный экран
      siomart.utils.add_single_listener siomart.utils.ge('closeNodeOffersPopupButton'), _e, siomart.close_node_offers_popup

      siomart.utils.add_single_listener siomart.utils.ge('sioMartHomeButton'), _e, siomart.close_node_offers_popup

      ## События для свайпа
      siomart.utils.add_single_listener this._block_container, 'touchstart', ( event ) ->
        siomart.node_offers_popup.touchstart_event event

      siomart.utils.add_single_listener this._block_container, 'touchmove', ( event ) ->
        siomart.node_offers_popup.touchmove_event event

      siomart.utils.add_single_listener this._block_container, 'touchend', ( event ) ->
        siomart.node_offers_popup.touchend_event event

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
    localStorage.setItem('is_closed_by_used', true)
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

    if typeof siomart.shop_load_locked != 'undefined'
      return false

    siomart.shop_load_locked = true

    console.log 'load_for_shop_id : ' + shop_id + ', ad_id : ' + ad_id

    url = '/market/ads?a.shopId=' + shop_id + '&a.firstAdId=' + ad_id + '&a.size=50&a.rcvr=' + siomart.config.mart_id

    siomart.node_offers_popup.requested_ad_id = ad_id
    siomart.request.perform url

  ## Загрузить все офферы для магазина
  load_for_cat_id : ( cat_id ) ->
    if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
      return false

    url = '/market/ads?a.catId=' + cat_id + '&a.rcvr=' + siomart.config.mart_id
    siomart.request.perform url

  ########################################
  ## картинка приветствия торгового центра
  ########################################
  welcome_ad :
    hide_timeout : 3000
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
      if this.img_dom == null
        return false

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
    this.utils.add_single_listener this.utils.ge('sioMartHomeButton'), _event, siomart.load_index_ads

    this.utils.add_single_listener this.utils.ge('smSearchIcon'), _event, () ->
      siomart.utils.ge('smSearchField').focus()

    this.init_shop_links()
    this.init_categories_links()

  init_shop_links : () ->
    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'
    blocks_w_actions = siomart.utils.ge_class document, 'js-shop-link'
    for _b in blocks_w_actions
      cb = ( b ) ->
        producer_id = b.getAttribute 'data-producer-id'
        ad_id = b.getAttribute 'data-ad-id'
        siomart.utils.add_single_listener b, _event, () ->
          siomart.load_for_shop_id producer_id, ad_id
      cb _b

  init_categories_links : () ->
    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'
    blocks_w_actions = siomart.utils.ge_class document, 'js-cat-link'
    for _b in blocks_w_actions
      cb = ( b ) ->
        cat_id = b.getAttribute 'data-cat-id'

        _cat_class_match_regexp = new RegExp( 'disabled' ,"g")
        if !siomart.utils.is_array( _b.className.match( _cat_class_match_regexp ) )
          siomart.utils.add_single_listener b, _event, () ->
            siomart.load_for_cat_id cat_id
      cb _b

  set_window_class : () ->
    _window_class = ''

    if cbca_grid.ww <= 980
      _window_class = 'sm-w-980'

    if cbca_grid.ww <= 800
      _window_class = 'sm-w-800'

    if cbca_grid.ww <= 660
      _window_class = 'sm-w-400'

    siomart.utils.ge('sioMartLayout').className = _window_class

  ###########################
  ## Инициализация Sio.Market
  ###########################
  init : () ->

    siomart.config.mart_id = window.siomart_id
    siomart.config.host = window.siomart_host

    ## загрузка cbca_grid
    this.load_deps()
    this.utils.set_vendor_prefix()

    ## Нарисовать разметку
    this.draw_layout()

    ## Забиндить оконные события
    this.bind_window_events()

    this.load_mart_index_page()

window.siomart = siomart
siomart.init()
