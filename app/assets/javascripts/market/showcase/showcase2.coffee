cbca_grid =
  cell_size : 140
  cell_padding : 20
  top_offset : 20
  bottom_offset : 20
  max_allowed_cell_width : 4

  blocks : []
  spacers : []

  set_window_size : () ->
    ww = wh = 0
    if typeof window.innerWidth == 'number'
      ww = window.innerWidth
      wh = window.innerHeight
    else if document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight )
      ww = document.documentElement.clientWidth
      wh = document.documentElement.clientHeight
    else if document.body && ( document.body.clientWidth || document.body.clientHeight )
      ww = document.body.clientWidthb
      wh = document.body.clientHeight

    this.ww = ww
    this.wh = wh

  set_container_size : () ->
    this.set_window_size()

    no_of_cells = Math.floor( ( this.ww - this.cell_padding ) / ( this.cell_size + this.cell_padding) )

    if no_of_cells < 2
      no_of_cells = 2

    if no_of_cells > 8
      no_of_cells = 8

    if no_of_cells == 3
      no_of_cells = 3

    if no_of_cells == 5
      no_of_cells = 4

    if no_of_cells == 7
      no_of_cells = 6

    cw = no_of_cells * ( this.cell_size + this.cell_padding) - this.cell_padding

    this.max_allowed_cell_width = no_of_cells

    if typeof this.layout_dom != 'undefined'
      this.layout_dom.style.width = cw + 'px'
      this.layout_dom.style.height = cbca_grid.wh + 'px'
      this.layout_dom.style.opacity = 1

  ##############
  ## Fetch block
  ##############
  fetch_block : (block_max_w, tmp_block, i) ->

    # если элементов в grid.blocks нет – возвращаем null
    if this.blocks.length == 0
      return null

    if typeof( tmp_block ) == "undefined"
      tmp_block = this.blocks
      i = 0

    b = tmp_block[i]

    if typeof b == "undefined"
      return null

    w_cell_width = Math.floor ( b.width + this.cell_padding ) / ( this.cell_size + this.cell_padding )
    w_cell_width_opened = Math.floor ( b.opened_width + this.cell_padding ) / ( this.cell_size + this.cell_padding )

    if w_cell_width <= block_max_w
      if w_cell_width > this.max_allowed_cell_width || w_cell_width_opened > this.max_allowed_cell_width
        tmp_block.splice i,1

        _elt = siomart.utils.ge 'elt' + b.id

        _elt.style.opacity = 0

        for p in [vendor_prefix.css + 'transform', 'transform']
          _elt.style[p] = 'translate3d(-1000px, 0px,0)'

        return this.fetch_block(block_max_w, tmp_block, i+1 )
      else
        tmp_block.splice(i,1)
        this.blocks = tmp_block

        return b
    else
      if w_cell_width > this.max_allowed_cell_width || w_cell_width_opened > this.max_allowed_cell_width

        _elt = siomart.utils.ge 'elt' + b.id

        _elt.style.opacity = 0
        for p in [vendor_prefix.css + 'transform', 'transform']
          _elt.style[p] = 'translate3d(-1000px, 0px,0)'
        tmp_block.splice(i,1)

      return this.fetch_block(block_max_w, tmp_block, i+1 )

  fetch_spacer : (block_max_w, tmp_block, i) ->

    if this.spacers.length == 0
      return null

    if typeof( tmp_spacer ) == "undefined"
      tmp_spacers = this.spacers
      i = 0

    b = tmp_spacers[i]
    tmp_spacers.splice(i,1)
    this.spacers = tmp_spacers
    b.block.style.display = 'block'
    return b

  ######################
  ## Get Max block width
  ######################
  get_max_block_width : ( columns_used_space, cline, cur_column, columns ) ->

    m_w = 1

    for i in [cur_column..columns]

      if typeof( columns_used_space[i] ) != 'undefined'
        if columns_used_space[i].used_height == cline
          m_w++
        else
          m_w--
          return m_w
      else
        m_w--
        return m_w

  get_max_block_width : ( columns_used_space, cline, cur_column, columns ) ->
    m_w = 1

    for i in [cur_column..columns]

      if typeof( columns_used_space[i] ) != 'undefined'
        if columns_used_space[i].used_height == cline
          m_w++
        else
          m_w--
          return m_w
      else
        m_w--
        return m_w

  init_single_block : ( block_id ) ->

    _elt = siomart.utils.ge 'elt' + block_id

    siomart.utils.addClass _elt, 'animated-block'
    _elt.style.opacity = 1

  deactivate_block : ( block_id, target_opacity ) ->

    block = siomart.utils.ge block_id

    block.style.opacity = target_opacity

    if siomart.utils.is_array block.className.match /active-block/g
      siomart.utils.removeClass block, 'active-block'
      block_js_class = block.getAttribute 'data-js-class'

      bs = siomart.utils.ge_class block, '.block-source'
      cb2 = () ->
        bs.style['visibility'] = 'hidden'
        siomart.utils.removeClass block, 'no-bg'
        siomart.utils.removeClass block, 'hover'

      cbca['block_desource' + block_id] = setTimeout cb2, 300

      if typeof block_js_class != 'undefined'
        close_action = window[block_js_class].close_action

        if typeof close_action != 'undefined'
          close_action()

  is_only_spacers : () ->
    for b in this.blocks
      if b.class != 'sm-b-spacer'
        return false

    return true

  max_used_height : ( columns_used_space ) ->
    max_h = 0

    for c, v of columns_used_space
      if v.used_height > max_h
        max_h = v.used_height

    return max_h

  ################################
  ## Найти новые блоки на странице
  ################################
  load_blocks : ( is_add ) ->

    is_add = is_add || false

    cbca_grid.blocks = []
    cbca_grid.spacers = []
    cbca_grid.m_spacers = []

    if is_add == true
      i = cbca_grid.blocks_index
    else
      cbca_grid.all_blocks = []
      i = 0

    console.log 'cbca_grid.all_blocks'
    console.log cbca_grid.all_blocks

    ## TODO : make selector configurable

    for elt in siomart.utils.ge_class document, 'sm-block'

      if elt.id == ''
        _this = elt

        _this.setAttribute 'id', 'elt' + i

        height = parseInt _this.getAttribute 'data-height'
        width = parseInt _this.getAttribute 'data-width'

        opened_height = parseInt _this.getAttribute 'data-opened-height'
        opened_width = parseInt _this.getAttribute 'data-opened-width'

        _class = _this.className
        _search_string = _this.getAttribute 'data-search-string'
        _is_moveable = _this.getAttribute 'data-is-moveable' || 'false'

        block =
          'id' : i
          'width' : width
          'height' : height
          'opened_width' : opened_width
          'opened_height' : opened_height
          'class' : _class
          'block' : _this
          '_is_moveable' : _is_moveable

        i++
        cbca_grid.blocks.push block
        cbca_grid.m_blocks = cbca_grid.blocks.slice(0)
        cbca_grid.all_blocks.push block

    ## Загрузить спейсеры
    #for i in siomart.utils.ge_class document, 'sm-b-spacer'
    for k in [1..30]

      _spacer_attributes =
        'class' : 'sm-b-spacer sm-b-spacer-' + k
        'data-width' : 140
        'data-height' : 140

      _spacer = siomart.utils.ce 'div', _spacer_attributes
      _this = _spacer

      siomart.utils.ge('sioMartIndexGrid').appendChild _spacer

      _this.setAttribute 'id', 'elt' + i

      height = 140
      width = 140

      opened_height = 140
      opened_width = 140

      _class = _this.className
      _search_string = _this.getAttribute 'data-search-string'
      _is_moveable = _this.getAttribute 'data-is-moveable' || 'false'

      block =
        'id' : i
        'width' : width
        'height' : height
        'opened_width' : opened_width
        'opened_height' : opened_height
        'class' : _class
        'block' : _this
        '_is_moveable' : _is_moveable

      i++
      cbca_grid.spacers.push block
      cbca_grid.m_spacers = cbca_grid.spacers.slice(0)

    cbca_grid.blocks_index = i

  init : ( is_add ) ->

    this.blocks_container = document.getElementById 'sioMartIndexGrid'
    this.layout_dom = document.getElementById 'sioMartIndexGridLayout'

    this.set_container_size()
    this.load_blocks( is_add )

    this.build( is_add )

  resize : () ->

    this.set_container_size()

    if typeof cbca_grid.blocks == 'undefined'
      return false

    cbca_grid.m_blocks = cbca_grid.all_blocks.slice(0)
    cbca_grid.blocks = cbca_grid.m_blocks

    this.build()

  build : ( is_add ) ->

    is_add = is_add || false

    for elt in siomart.utils.ge_class document, 'blocks-container'
      elt.style.display = 'block'

    if is_add == false
      for elt in siomart.utils.ge_class document, 'sm-b-spacer'
        elt.style.display = 'none'

    blocks_length = cbca_grid.blocks.length

    # setting up left and top
    left_pointer = left_pointer_base = 0
    top_pointer = 0

    # Определяем ширину окна
    window_width = this.ww

    # Определеяем сколько колонок влезет в экран колонок
    columns = Math.floor( ( window_width - this.cell_padding ) / ( this.cell_size + this.cell_padding) )

    if columns < 2
      columns = 2

    if columns > 8
      columns = 8

    if columns == 3
      columns = 3

    if columns == 5
      columns = 4

    if columns == 7
      columns = 6

    # Ставим указатели строки и колонки
    cline = 0
    pline = 0
    cur_column = 0

    if is_add == false
      # Генерим объект с инфой об использованном месте
      columns_used_space = {}
      for c in [0..columns-1]
        columns_used_space[c] =
          used_height : 0
    else
      columns_used_space = cbca_grid.columns_used_space

    is_break = false

    ## Генерим поле
    for i in [0..1000]

      pline = cline

      if cur_column >= Math.floor columns
        cur_column = 0
        cline++
        left_pointer = left_pointer_base

      top = top_pointer + cline * ( this.cell_size + this.cell_padding ) + this.top_offset
      left = left_pointer

      if this.is_only_spacers() == true
        is_break = true
        break

      if cline > pline && this.is_only_spacers() == true && cline == this.max_used_height columns_used_space
        is_break = true
        break

      if is_break == false
        if columns_used_space[cur_column].used_height == cline

          # есть место хотя бы для одного блока с минимальной шириной
          # высяним блок с какой шириной может влезть
          block_max_w = this.get_max_block_width columns_used_space, cline, cur_column, columns

          b = this.fetch_block block_max_w

          if b == null
            if this.blocks.length > 0
              b = this.fetch_spacer block_max_w
            else
              break

          w_cell_width = Math.floor ( b.width + this.cell_padding ) / ( this.cell_size + this.cell_padding )
          w_cell_height = Math.floor ( b.height + this.cell_padding ) / ( this.cell_size + this.cell_padding )

          for ci in [cur_column..cur_column+w_cell_width-1]
            if typeof( columns_used_space[cur_column] ) != 'undefined'
              columns_used_space[cur_column].used_height += w_cell_height
              cur_column++

          id = b.id

          _pelt = document.getElementById('elt' + id)

          ## temp
          _pelt.style.opacity = 1
          _pelt.style.display = 'block'

          if _pelt != null

            for p in [vendor_prefix.css + 'transform', 'transform']
              style_string = 'translate3d(' + left + 'px, ' + top + 'px,0)'
              _pelt.style[p] = style_string

          left_pointer += b.width + this.cell_padding
          pline = cline

        else
          cur_column++
          left_pointer += this.cell_size + this.cell_padding

    for b in this.blocks
      bid = b.id
      b_elt = siomart.utils.ge('elt' + bid )

      if b_elt != null
        b_elt.style.opacity = 0

    cbca_grid.columns_used_space = columns_used_space

    ## Вычислим максимальную высоту внутри колонки
    max_h = this.max_used_height columns_used_space

    real_h = ( this.cell_size + this.cell_padding) * max_h + this.bottom_offset

    this.blocks_container.style.height = parseInt( real_h + this.top_offset ) + 'px'

window.cbca_grid = cbca_grid

###############################
## Showcase с поддержкой blocks
###############################
siomart =
  config :

    whitelisted_domains : ['suggest.io', 'localhost:9000', '192.168.199.148:9000']
    index_action : window.siomart_index
    sm_layout_class : 'sio-mart-showcase'
    sm_trigger_class : 'sio-mart-trigger'
    ontouchmove_offer_change_delta : 80
    welcome_ad_hide_timeout : 2000
    ads_per_load : 30
    producer_ads_per_load : 5
    sio_hostnames : ["suggest.io", "localhost", "192.168.199.*"]


  getDeviceScale : () ->
    deviceWidth = landscape = Math.abs(window.orientation) == 90

    if landscape
      deviceWidth = Math.max(480, screen.height)
    else
      deviceWidth = screen.width
    return window.innerWidth / deviceWidth

  ## Забиндить оконные события
  bind_window_events : () ->
    resize_cb = () ->
      console.log 'resize'
      siomart.welcome_ad.fit siomart.welcome_ad.img_dom
      window.scrollTo(0,0)
      if typeof siomart.window_resize_timer != 'undefined'
        clearTimeout siomart.window_resize_timer

      grid_resize = () ->

        document.getElementById('sioMartIndexOffers').scrollTop = '0';

        cb = () ->
          siomart.utils.ge('sioMartIndexGrid').style.height = parseInt( siomart.utils.ge('sioMartIndexGrid').style.height) + 10 + 'px'

        setTimeout cb, 70

        cbca_grid.resize()
        siomart.set_window_class()

        siomart.focused_ads.fit()
        siomart.focused_ads.show_block_by_index siomart.focused_ads.active_block_index

      siomart.window_resize_timer = setTimeout grid_resize, 300

    this.utils.add_single_listener window, 'resize', resize_cb

  styles :

    style_dom : null

    init : () ->

      style_tags = siomart.utils.ge_tag('code')
      css = ''

      for s in style_tags
        css = css.concat( s.innerHTML )

      if this.style_dom == null
        style_dom = document.createElement('style')
        style_dom.type = "text/css"
        siomart.utils.ge_tag('head')[0].appendChild(style_dom)
        this.style_dom = style_dom
      else
        this.style_dom.innerHTML = ''
      
      this.style_dom.appendChild(document.createTextNode(css))

  #########################
  ## History Api navigation
  #########################
  history :
    base_path : null
    is_supported : () ->
      !!(window.history && history.pushState);

    navigate : ( state ) ->
      console.log 'navigate to :'
      console.log state

      if typeof siomart.focused_ads.requested_ad_id != 'undefined'
        siomart.close_focused_ads()
        return false

      if state == null
        if siomart.utils.ge('sioMartIndexGrid').innerHTML == ''
          return false
        siomart.navigation_layer.back()
        siomart.grid_ads.load_index_ads()
        return false

      if state.action == 'load_for_shop_id'
        siomart.load_for_shop_id state.shop_id, state.ad_id, false

      if state.action == 'open_navigation_layer'
        siomart.navigation_layer.open( false )

      if state.action == 'load_for_cat_id'
        siomart.load_for_cat_id state.cat_id, false

    push : ( data, title, path ) ->

      #history.pushState data, title, this.base_path + path
      history.pushState data, title, this.base_path

    init : () ->

      this.base_path = window.location.pathname

      if !this.is_supported()
        console.log 'history api not supported'
        return false

      siomart.utils.add_single_listener window, 'popstate', ( event ) ->
        siomart.history.navigate event.state

  ########
  ## Утиль
  ########

  log : ( message ) ->
    this.write_log('log', message)

  warn : ( message ) ->
    this.write_log('warn', message)

  error : ( message ) ->
    this.write_log('error', message)

  write_log : ( fun, message ) ->
    console[fun](message)

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

    is_sio_host : () ->
      for hn in siomart.config.sio_hostnames
        if window.location.hostname.match hn
          return true
      return false

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
        this.elts_cache[tag]
      else
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

    ###################
    ## Заменить элемент
    ###################
    replaceHTMLandShow : ( el, html ) ->
      if typeof el == "string"
        oldEl = document.getElementById(el)
      else
        oldEl = el
      newEl = document.createElement oldEl.nodeName

      newEl.id = oldEl.id
      newEl.className = oldEl.className

      newEl.style.display = 'block'
      newEl.innerHTML = html
      oldEl.parentNode.replaceChild newEl, oldEl

      return newEl

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

    touchmove_lock_delta : 0
    is_touch_locked : false

    document_touchmove : ( event ) ->

      cx = event.touches[0].pageX
      cy = event.touches[0].pageY

      siomart.events.document_touch_x_delta = siomart.events.document_touch_x - cx
      siomart.events.document_touch_y_delta = siomart.events.document_touch_y - cy

      if Math.abs( siomart.events.document_touch_x_delta ) > siomart.events.touchmove_lock_delta || Math.abs( siomart.events.document_touch_y_delta ) > siomart.events.touchmove_lock_delta || typeof siomart.events.document_touch_x == 'undefined'
        siomart.events.is_touch_locked = true

      if typeof siomart.events.document_touch_x == 'undefined'
        siomart.events.document_touch_x = cx
        siomart.events.document_touch_y = cy



    document_touchend : ( event ) ->

      cb = () ->
        siomart.events.is_touch_locked = false
        delete siomart.events.document_touch_x
        delete siomart.events.document_touch_y

      setTimeout cb, 100

    document_touchcancel : ( event ) ->
      siomart.events.is_touch_locked = false
      delete siomart.events.document_touch_x
      delete siomart.events.document_touch_y

    document_keyup_event : ( event ) ->

      if !event
        return false

      ## Exc button
      if event.keyCode == 27
        siomart.close_focused_ads()
        siomart.navigation_layer.back()

      if event.keyCode == 39
        siomart.focused_ads.next_block()

      if event.keyCode == 37
        siomart.focused_ads.prev_block()


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
  grid_ads :
    is_fully_loaded : false
    is_load_more_requested : false
    c_url : null

    load_more : () ->

      if this.is_load_more_requested == true || this.is_fully_loaded == true
        return false
      console.log 'load more'
      console.log 'loaded : ' + this.loaded

      this.is_load_more_requested = true

      console.log this.c_url

      siomart.request.perform this.c_url + '&a.size=' + siomart.config.ads_per_load + '&a.offset=' + this.loaded

    load_index_ads : () ->
      grd_c = siomart.utils.ge('sioMartIndexGrid')
      url = grd_c.getAttribute 'data-index-offers-action'

      siomart.grid_ads.is_fully_loaded = false
      siomart.grid_ads.is_load_more_requested = false
      siomart.utils.ge('gridAdsLoader').style.opacity = 1

      siomart.grid_ads.loaded = 0

      if typeof siomart.grid_ads.multiplier == 'undefined'
        siomart.grid_ads.multiplier = 100000000000
      else
        siomart.grid_ads.multiplier = siomart.grid_ads.multiplier / 10

      siomart.grid_ads.c_url = url + '&a.gen=' + Math.floor((Math.random() * siomart.grid_ads.multiplier) + (Math.random() * 100000) )

      console.log siomart.grid_ads.c_url

      siomart.request.perform siomart.grid_ads.c_url + '&a.size=' + siomart.config.ads_per_load

  #####################################################
  ## Добавить в DOM необходимую разметку для Sio.Market
  #####################################################

  draw_trigger : () ->
    sm_trigger_attrs =
      class : this.config.sm_trigger_class
      id : 'sioMartTrigger'
      style : 'opacity: 0; background-color: #' + siomart.config.node_color

    sm_trigger = this.utils.ce 'div', sm_trigger_attrs, '<span class="trigger-helper"><img src=\'' + siomart.config.host + siomart.config.logo_src + '\'/>' + '</span>'

    _body = this.utils.ge_tag('body')[0]
    _body.appendChild sm_trigger

    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'
    this.utils.add_single_listener sm_trigger, _event, () ->
      showcase_url = siomart.config.host + '/market/site/' + siomart.config.mart_id

      newwindow = window.open showcase_url, 'Sio.Market'
      newwindow.focus()


  draw_layout : () ->


    ## Интерфейс маркета
    sm_layout_attrs =
      class : this.config.sm_layout_class
      id : 'sioMartRoot'
    sm_layout = this.utils.ce "div", sm_layout_attrs, '<div id="sioMartLayout"></div>'
    sm_layout.style.display = 'none'

    _body = this.utils.ge_tag('body')[0]
    _body.appendChild sm_layout

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

    console.log 'receive_response : received data'
    console.warn data

    if typeof siomart.request.request_timeout_timer != 'undefined'
      clearTimeout siomart.request.request_timeout_timer

    ## Пришла пустота — уведомить юзера
    if data.html == ''
      siomart.notifications.show "КАРТОЧЕК НЕ НАЙДЕНО, ПОПРОБУЙТЕ ДРУГОЙ ЗАПРОС"
      if siomart.utils.ge('smLoading') != null
        siomart.utils.ge('smLoading').style.display = 'none'
      return false

    ## Асинхронное получение доп. данных о ноде. Если этот экшн пришел — значит нужно инициализировать триггер
    if data.action == "setData"
      siomart.config.node_color = data.color
      siomart.config.logo_src = data.logo_src

      ## Нарисовать разметку
      this.draw_trigger()

      trigger_cb = () ->
        siomart.utils.ge('sioMartTrigger').style.opacity = '1'
      setTimeout trigger_cb, 100

    ## Инициализация глагне
    if data.action == 'showcaseIndex'
      siomart.log 'showcaseIndex'

      cbca_grid.set_window_size()

      ## Нарисовать разметку
      this.draw_layout()

      ## Забиндить оконные события
      this.bind_window_events()

      ## Инициализировать history api
      ## this.history.init()

      window.scrollTo 0,0

      siomart.utils.ge('sioMartRoot').style.display = 'block'

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
        siomart.grid_ads.load_index_ads()
        siomart.styles.init()

      setTimeout grid_init_cb, grid_init_timoeut
      siomart.set_window_class()
      this.is_market_loaded = true


    if data.action == 'producerAds'

      if siomart.focused_ads.load_more_ads_requested == true
        siomart.focused_ads.render_more data.blocks
      else
        screensContainer = siomart.utils.ge 'sioMartNodeOffersRoot'
        screensContainer = siomart.utils.replaceHTMLandShow screensContainer, data.html

        cb = () ->
          siomart.utils.addClass screensContainer, 'sio-mart__node-offers-root_in'

        setTimeout cb, 30

        siomart.focused_ads.blocks = data.blocks
        siomart.focused_ads.init()
        siomart.navigation_layer.close()
        console.log 'producerAds : ready'

    if data.action == 'findAds' || data.action == 'searchAds'
      grid_container_dom = siomart.utils.ge 'sioMartIndexGrid'

      if typeof data.blocks == 'undefined'
        siomart.grid_ads.is_fully_loaded = true
        siomart.utils.ge('smLoading').style.display = 'none'
        return false

      if data.blocks.length < siomart.config.ads_per_load
        siomart.utils.ge('gridAdsLoader').style.opacity = 0

      html = ''

      for index, elt of data.blocks
        if typeof elt == 'string'
          html += elt

      if typeof data.blocks != 'undefined'
        siomart.grid_ads.loaded += data.blocks.length

        if siomart.grid_ads.is_load_more_requested == false
          grid_container_dom.innerHTML = html
          document.getElementById('sioMartIndexOffers').scrollTop = '0';
          cbca_grid.init()
        else
          grid_container_dom.innerHTML += html
          cbca_grid.init(is_add = true)

        this.utils.add_single_listener this.utils.ge('sioMartIndexOffers'), 'scroll', () ->
          scrollTop = siomart.utils.ge('sioMartIndexOffers').scrollTop
          height = siomart.utils.ge('sioMartIndexOffers').offsetHeight

          if parseInt( height + scrollTop ) > siomart.utils.ge('sioMartIndexGrid').offsetHeight
            siomart.grid_ads.load_more()

        siomart.styles.init()
        siomart.init_shop_links()

        if data.action == 'searchAds'
          siomart.navigation_layer.close true
        else
          siomart.navigation_layer.close()

      siomart.grid_ads.is_load_more_requested = false

    if siomart.utils.ge('smLoading') != null
      siomart.utils.ge('smLoading').style.display = 'none'

  close_focused_ads : ( event ) ->
    siomart.utils.removeClass siomart.utils.ge('sioMartNodeOffersRoot'), 'sio-mart__node-offers-root_in'

    cb = () ->
      siomart.utils.re 'sioMartNodeOffers'
      siomart.utils.ge('sioMartNodeOffersRoot').style.display = 'none'

      delete siomart.shop_load_locked

    setTimeout cb, 200

    delete siomart.focused_ads.requested_ad_id
    delete siomart.focused_ads.active_block_index

    if event
      event.preventDefault()

  focused_ads :

    load_more_ads_requested : false

    load_more_ads : () ->
      siomart.request.perform this.curl + '&h=' + false + '&a.offset=' + this.blocks.length
      this.load_more_ads_requested = true

    scroll_or_move : undefined

    show_block_by_index : ( block_index, direction ) ->

      if typeof this.blocks == 'undefined'
        return false

      if block_index == parseInt( this.blocks.length - 1 )
        this.load_more_ads()

      if typeof this.sm_blocks == 'undefined'
        return false

      if vendor_prefix.js == 'Webkit'
        siomart.focused_ads._block_container.style['-webkit-transform'] = 'translate3d(-' + cbca_grid.ww*block_index + 'px, 0px, 0px)'
      else
        siomart.focused_ads._block_container.style['transform'] = 'translate3d(-' + cbca_grid.ww*block_index + 'px, 0px, 0px)'

      siomart.focused_ads._block_container.setAttribute 'data-x-offset', -cbca_grid.ww*block_index

      if block_index == this.active_block_index
        return false

      this.active_block_index = block_index

      if direction == '+'
        ad_c_el = siomart.utils.ge('ad_c_' + ( block_index + 1 ) )
        if ad_c_el != null
          ad_c_el.style.visibility = 'visible';

        if block_index >= 2
          siomart.utils.ge('ad_c_' + ( block_index - 2 ) ).style.visibility = 'hidden';

      if direction == '-'
        if block_index >= 1
          siomart.utils.ge('ad_c_' + ( block_index - 1 ) ).style.visibility = 'visible';

        fel = siomart.utils.ge('ad_c_' + ( block_index + 2 ) )
        if fel != null
          fel.style.visibility = 'hidden';

    next_block : () ->
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

        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.width = cbca_grid.ww + 'px'

        _b.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 'px'

        if cbca_grid.ww >= 660
          _b.parentNode.parentNode.parentNode.style.width = _block_width*2 + 11*2 + 'px'
        else
          _b.parentNode.parentNode.parentNode.style.width = _block_width + 'px'

      this._block_container.style.width = this.sm_blocks.length * cbca_grid.ww + 'px'

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

      if typeof siomart.focused_ads.scroll_or_move == 'undefined' && !( delta_x == 0 && delta_y == 0 )
        if Math.abs( delta_y ) > Math.abs( delta_x )
          siomart.focused_ads.scroll_or_move = 'scroll'
        else
          siomart.focused_ads.scroll_or_move = 'move'

      console.log siomart.focused_ads.scroll_or_move

      if siomart.focused_ads.scroll_or_move == 'scroll'
        return false
      else
        event.preventDefault()

      c_x_offset = siomart.focused_ads._block_container.getAttribute 'data-x-offset'
      c_x_offset = parseInt c_x_offset

      if vendor_prefix.js == 'Webkit'
        siomart.focused_ads._block_container.style['-webkit-transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'
      else
        siomart.focused_ads._block_container.style['transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'

      this.x_delta_direction = this.last_x - ex

      this.last_x = ex

    touchend_event : ( event ) ->
      console.log 'touchend'
      siomart.utils.addClass this._block_container, 'sio-mart-node-offers-window__root-container_animated'

      delete siomart.focused_ads.tstart_x
      delete siomart.focused_ads.tstart_y

      if this.x_delta_direction > 0
        cb = () ->
          siomart.focused_ads.next_block()
      else
        cb = () ->
          siomart.focused_ads.prev_block()

      if this.scroll_or_move == 'move'
        setTimeout cb, 1

      delete siomart.focused_ads.scroll_or_move

    touchcancel_event : ( event ) ->

      delete siomart.focused_ads.tstart_x
      delete siomart.focused_ads.tstart_y
      delete siomart.focused_ads.scroll_or_move

    render_ad : ( ad ) ->

      console.log this.ads_rendered

      siomart.utils.ge('ad_c_' + this.ads_rendered).innerHTML = ad
      this.ads_rendered++

    render_more : ( more_blocks ) ->
      this.load_more_ads_requested = false
      if typeof more_blocks == 'undefined'
        return false

      for i, v of more_blocks
        this.render_ad more_blocks[i], i
        this.blocks.push more_blocks[i]

      this.sm_blocks = sm_blocks = siomart.utils.ge_class this._container, 'sm-block'
      this.fit()
      siomart.styles.init()

    init : () ->

      this._block_container = siomart.utils.ge('sioMartNodeOffersBlockContainer')
      this.bcInnerHTML = this._block_container.innerHTML
      
      this.ads_count = this._block_container.getAttribute 'data-ads-count'

      this.ads_rendered = 1

      ads_cs = this.bcInnerHTML
      for i in [1..this.ads_count]
        ads_cs += '<div id="ad_c_' + i + '"></div>'

      this._block_container.innerHTML = ads_cs

      for i, v of this.blocks
        this.render_ad this.blocks[i]

      this._container = siomart.utils.ge('sioMartNodeOffers')

      siomart.utils.addClass this._block_container, 'sio-mart-node-offers-window__root-container_animated'

      ## События
      _e = if siomart.utils.is_touch_device() then 'touchend' else 'click'

      ## Кнопка возврата на главный экран
      siomart.utils.add_single_listener siomart.utils.ge('closeNodeOffersPopupButton'), _e, siomart.close_focused_ads

      siomart.utils.add_single_listener siomart.utils.ge('sioMartHomeButton'), _e, siomart.close_focused_ads

      ## События для свайпа
      siomart.utils.add_single_listener this._block_container, 'touchstart', ( event ) ->
        siomart.focused_ads.touchstart_event event

      siomart.utils.add_single_listener this._block_container, 'touchmove', ( event ) ->
        siomart.focused_ads.touchmove_event event

      siomart.utils.add_single_listener this._block_container, 'touchcancel', ( event ) ->
        siomart.focused_ads.touchcancel_event event

      siomart.utils.add_single_listener this._block_container, 'touchend', ( event ) ->
        siomart.focused_ads.touchend_event event

      this.sm_blocks = sm_blocks = siomart.utils.ge_class this._container, 'sm-block'
      this.fit()
      i = 0
      this.active_block_index = 0

  load_mart_data : () ->
    this.request.perform '/market/node_data/' + window.siomart_id

  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart : () ->
    this.define_per_load_values()
    this.request.perform siomart.config.index_action

  ##################################################
  ## Показать / скрыть экран с категориями и поиском
  ##################################################
  navigation_layer :
    open : ( history_push ) ->

      if typeof history_push != 'boolean'
        history_push = true

      sm_cat_screen = siomart.utils.ge('smCategoriesScreen')

      if sm_cat_screen != null
        sm_cat_screen.style.display = 'block'
        siomart.utils.ge('smSearchBar').style.display = 'block'

      console.log history_push

      if history_push == true
        state_data =
          action : 'open_navigation_layer'
        siomart.history.push state_data, 'SioMarket', '/n/categories'

    close : ( all_except_search ) ->
      sm_cat_screen = siomart.utils.ge('smCategoriesScreen')
      console.log sm_cat_screen
      if sm_cat_screen != null
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
    #siomart.utils.ge('sioMartRoot').style.display = 'none'
    #siomart.utils.ge('smCloseScreen').style.display = 'none'

    #siomart.utils.ge_tag('body')[0].style.overflow = 'auto'
    
    event.preventDefault()
    return false

  open_mart : ( event ) ->
    if this.is_market_loaded != true
      siomart.load_mart()
    event.preventDefault()
    return false

  index_navigation :
    hide : () ->
      _dom = siomart.utils.ge 'smIndexNavigation'
      siomart.utils.addClass _dom, 'hidden'
    show : () ->
      _dom = siomart.utils.ge 'smIndexNavigation'
      siomart.utils.removeClass _dom, 'hidden'

  load_for_shop_id : ( shop_id, ad_id, history_push ) ->

    if typeof history_push == 'undefined'
      history_push = true

    if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
      return false

    if typeof siomart.shop_load_locked != 'undefined'
      return false

    siomart.shop_load_locked = true

    url = '/market/fads?a.shopId=' + shop_id + '&a.gen=' + Math.floor((Math.random() * 100000000000) + 1) + '&a.size=' + siomart.config.producer_ads_per_load + '&a.rcvr=' + siomart.config.mart_id

    siomart.focused_ads.curl = url

    if history_push == true
      state_data =
        action : 'load_for_shop_id'
        shop_id : shop_id
        ad_id : ad_id
      siomart.history.push state_data, 'SioMarket', '/n/mart/' + shop_id + '/' + ad_id

    siomart.focused_ads.requested_ad_id = ad_id
    siomart.request.perform url + '&a.firstAdId=' + ad_id

  ## Загрузить все офферы для магазина
  load_for_cat_id : ( cat_id, history_push ) ->
    if typeof history_push != 'boolean'
      history_push = true

    if siomart.utils.is_touch_device() && siomart.events.is_touch_locked
      return false

    if history_push == true
      state_data =
        action : 'load_for_cat_id'
        cat_id : cat_id
      siomart.history.push state_data, 'SioMarket', '/n/cat/' + cat_id

    url = '/market/ads?a.catId=' + cat_id + '&a.rcvr=' + siomart.config.mart_id
    siomart.request.perform url

  ########################################
  ## картинка приветствия торгового центра
  ########################################
  welcome_ad :
    hide_timeout : 1700
    fadeout_transition_time : 700

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

      if siomart.is_market_loaded == true
        this.img_dom.style.display = 'none'
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
    siomart.utils.add_single_listener window, 'touchcancel', siomart.events.document_touchcancel
    
    ## Кнопка выхода
    siomart.utils.add_single_listener document, 'keyup', siomart.events.document_keyup_event

    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'

    siomart.utils.add_single_listener this.utils.ge('smCloseButton'), _event, siomart.open_close_screen

    this.utils.add_single_listener this.utils.ge('smCloseConfirmedButton'), _event, siomart.close_mart
    this.utils.add_single_listener this.utils.ge('smExitCloseScreenButton'), _event, siomart.exit_close_screen

    this.utils.add_single_listener this.utils.ge('smShopListButton'), _event, siomart.open_shopList_screen

    ## поле ввода поискового запроса
    this.utils.add_single_listener this.utils.ge('smSearchField'), 'keyup', () ->
      this.value = this.value.toUpperCase()
      siomart.search.queue_request this.value

    ## Кнопка вызова окна с категориями
    this.utils.add_single_listener this.utils.ge('smCategoriesButton'), _event, siomart.navigation_layer.open
    this.utils.add_single_listener this.utils.ge('smNavigationLayerBackButton'), _event, siomart.navigation_layer.back

    ## Возврат на индекс выдачи
    this.utils.add_single_listener this.utils.ge('rootNodeLogo'), _event, siomart.grid_ads.load_index_ads
    this.utils.add_single_listener this.utils.ge('sioMartHomeButton'), _event, siomart.grid_ads.load_index_ads

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

  define_per_load_values : () ->

    ww = wh = 0
    if typeof window.innerWidth == 'number'
      ww = window.innerWidth
      wh = window.innerHeight
    else if document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight )
      ww = document.documentElement.clientWidth
      wh = document.documentElement.clientHeight
    else if document.body && ( document.body.clientWidth || document.body.clientHeight )
      ww = document.body.clientWidthb
      wh = document.body.clientHeight

    if ww <= 980
      siomart.config.ads_per_load = 20

    if ww <= 800
      siomart.config.ads_per_load = 10

    if ww <= 660
      siomart.config.ads_per_load = 5

    console.log ww
    console.log siomart.config.ads_per_load

  ###########################
  ## Инициализация Sio.Market
  ###########################
  init : () ->
    siomart.config.mart_id = window.siomart_id
    siomart.config.host = window.siomart_host
    siomart.config.index_action = '/market/index/' + siomart.config.mart_id

    this.utils.set_vendor_prefix()

    siomart.load_mart()

window.siomart = siomart
siomart.init()