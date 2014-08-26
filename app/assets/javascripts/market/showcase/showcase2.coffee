cbca_grid =
  cell_size : 140
  cell_padding : 20
  top_offset : 70
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
    if is_add == false
      for k in [1..30]

        _spacer_attributes =
          'class' : 'sm-b-spacer sm-b-spacer-' + k
          'data-width' : 140
          'data-height' : 140

        _spacer = siomart.utils.ce 'div', _spacer_attributes
        _this = _spacer

        siomart.utils.ge('smGridAdsContainer').appendChild _spacer

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

    this.blocks_container = document.getElementById 'smGridAdsContainer'
    this.layout_dom = document.getElementById 'smGridAdsContainer'

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

          console.log b

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
    sm_layout_class : 'sm-showcase'
    ontouchmove_offer_change_delta : 80
    welcome_ad_hide_timeout : 2000
    ads_per_load : 30
    producer_ads_per_load : 5
    sio_hostnames : ["suggest.io", "localhost", "192.168.199.*"]

  geo :
    location_requested : false
    nodes_loaded : false
    search :
      min_request_length : 2
      search_delay : 500
      queue_request : ( request ) ->

        if request.length < this.min_request_length
          return false

        if typeof this.timer != 'undefined'
          clearTimeout this.timer

        cb = () ->
          siomart.geo.search.do request

        this.timer = setTimeout cb, this.search_delay

      do : ( request ) ->
        console.log 'request : ' + request

    position_callback : ( gp_obj ) ->
      siomart.geo.geo_position_obj = gp_obj
      siomart.geo.load_nodes( true )

    get_current_position : () ->
      this.location_requested = true
      navigator.geolocation.getCurrentPosition siomart.geo.position_callback

    init_events : () ->
      _geo_nodes_search_dom = siomart.utils.ge('smGeoNodesSearchInput')
      siomart.utils.add_single_listener _geo_nodes_search_dom, 'keyup', ( e ) ->
        console.log siomart.geo.search.queue_request this.value

    load_for_node_id : ( node_id ) ->

      siomart.config.index_action = '/market/index/' + node_id
      siomart.config.mart_id = node_id

      siomart.load_mart()

    adjust : () ->
      geo_screen = siomart.utils.ge('smGeoNodes')
      geo_screen_wrapper = siomart.utils.ge('smGeoNodesWrapper')
      geo_screen_content = siomart.utils.ge('smGeoNodesContent')

      geo_screen.style.height = cbca_grid.wh - 100
      geo_screen_wrapper.style.height = cbca_grid.wh - 100
      geo_screen_content.style.minHeight = cbca_grid.wh - 100 + 1


    request_query_param : () ->

      if window.with_geo == false
        return ""

      if typeof siomart.geo.geo_position_obj == 'undefined'
        "a.geo=ip"
      else
        "a.geo=" + this.geo_position_obj.coords.latitude + "," + this.geo_position_obj.coords.longitude

    load_nodes : ( refresh ) ->
      refresh = refresh || false

      if refresh == false && siomart.geo.nodes_loaded == true
        siomart.response_callbacks.find_nodes siomart.geo.nodes_data_cached
        return false
      console.log 'load nodes'
      url = '/market/nodes/search?' + this.request_query_param()
      siomart.request.perform url

    init : () ->
      if window.with_geo == true
        this.get_current_position()
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

        document.getElementById('smGridAdsWrapper').scrollTop = '0';

        cbca_grid.resize()
        siomart.set_window_class()

        siomart.focused_ads.fit()
        siomart.focused_ads.show_ad_by_index siomart.focused_ads.active_ad_index

        siomart.grid_ads.adjust_dom()

      siomart.window_resize_timer = setTimeout grid_resize, 300

    this.utils.add_single_listener window, 'resize', resize_cb

  styles :

    style_dom : null

    init : () ->

      style_tags = siomart.utils.ge_tag('code', true)

      css = ''

      for s in style_tags
        if s.getAttribute( 'data-rendered' ) == null
          s.setAttribute( 'data-rendered', true )
          css = css.concat( s.innerHTML )

      style_dom = document.createElement('style')
      style_dom.type = "text/css"
      siomart.utils.ge_tag('head')[0].appendChild(style_dom)
      this.style_dom = style_dom

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

      if element == null
        return false

      for _i, _c of element.classList
        if _c == value
          return false

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

  ###########################
  ## Единая обработка событий
  ###########################
  events :

    target_lookup : ( target, lookup_method, lookup_criteria ) ->

      if target == null
        return null

      if lookup_method == 'id'
        if target.id == lookup_criteria
          return target

      if lookup_method == 'className'

        if typeof target.className != 'undefined'
          regexp = new RegExp( lookup_criteria ,"g")

          for cn in target.classList
            if cn == lookup_criteria
              return target

      return siomart.events.target_lookup target.parentNode, lookup_method, lookup_criteria

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

    #############################
    ## Обработка click / touchend
    #############################
    document_click : ( event ) ->

      #if siomart.events.is_touch_locked
      #  return false

      ## Обработка событий для открытия / закрытия экрана выхода
      if siomart.events.target_lookup( event.target, 'id', 'smExitButton' ) != null
        siomart.utils.ge('smCloseScreen').style.display = 'block'
        siomart.utils.ge('smGridAds').style.webkitFilter = "blur(5px)"
        return false

      if siomart.events.target_lookup( event.target, 'id', 'smExitCloseScreenButton' ) != null
        siomart.utils.ge('smCloseScreen').style.display = 'none'
        siomart.utils.ge('smGridAds').style.webkitFilter = ""
        return false

      if siomart.events.target_lookup( event.target, 'id', 'smCloseScreenContainer' ) != null
        event.preventDefault()
        return false

      if siomart.events.target_lookup( event.target, 'id', 'smCloseScreen' ) != null
        siomart.utils.ge('smCloseScreen').style.display = 'none'
        siomart.utils.ge('smGridAds').style.webkitFilter = ""
        return false

      ## гео добро
      if siomart.events.target_lookup( event.target, 'id', 'smGeoScreenButton' ) != null
        siomart.geo.load_nodes()
        siomart.utils.ge('smGeoScreen').style.display = 'block'
        return false

      if siomart.events.target_lookup( event.target, 'id', 'smGeoLocationButton' ) != null
        siomart.geo.get_current_position()
        return false

      geo_node_target = siomart.events.target_lookup( event.target, 'className', 'js-geo-node' )
      if geo_node_target != null
        node_id = geo_node_target.getAttribute 'data-id'
        siomart.geo.load_for_node_id node_id

        return false

      if siomart.events.target_lookup( event.target, 'id', 'smIndexButton' ) != null
        siomart.utils.removeClass siomart.utils.ge('smRootProducerHeader'), '__w-global-cat'
        siomart.grid_ads.load_index_ads()
        return false

      shop_link_target = siomart.events.target_lookup( event.target, 'className', 'js-shop-link' )
      if shop_link_target != null

        if siomart.events.is_touch_locked
          return false

        producer_id = shop_link_target.getAttribute 'data-producer-id'
        ad_id = shop_link_target.getAttribute 'data-ad-id'

        siomart.load_for_shop_id producer_id, ad_id

        return false

      cat_link_target = siomart.events.target_lookup( event.target, 'className', 'js-cat-link' )
      if cat_link_target != null

        if siomart.events.is_touch_locked
          return false

        cat_id = cat_link_target.getAttribute 'data-cat-id'
        cat_class = cat_link_target.getAttribute 'data-cat-class'

        _cat_class_match_regexp = new RegExp( 'disabled' ,"g")
        if !siomart.utils.is_array( cat_link_target.className.match( _cat_class_match_regexp ) )
          siomart.load_for_cat_id cat_id, true, cat_class

      #######################
      ## Работа с категориями
      #######################
      if siomart.events.target_lookup( event.target, 'id', 'smNavigationLayerButton' ) != null
        siomart.navigation_layer.open()
        return false

      if siomart.events.target_lookup( event.target, 'id', 'smCategoriesTab' ) != null
        siomart.navigation_layer.show_tab 'smCategories'

      if siomart.events.target_lookup( event.target, 'id', 'smShopsTab' ) != null
        siomart.navigation_layer.show_tab 'smShops'

      if siomart.events.target_lookup( event.target, 'id', 'smCategoriesScreenCloseButton' ) != null
        siomart.navigation_layer.close()
        siomart.search.exit()

      ##############
      ## focused_ads
      ##############
      if siomart.events.target_lookup( event.target, 'id', 'closeFocusedAdsButton' ) != null
        siomart.focused_ads.close()

    document_keyup_event : ( event ) ->

      if !event
        return false

      ## Exc button
      if event.keyCode == 27
        siomart.close_focused_ads()
        siomart.navigation_layer.back()

      if event.keyCode == 39
        siomart.focused_ads.next_ad()

      if event.keyCode == 37
        siomart.focused_ads.prev_ad()


  ########
  ## Поиск
  ########

  search :
    found_count : 0
    request_delay : 600
    is_active : false
    perform : ( request ) ->

      if request == ''
        return false

      if request.length < 3
        siomart.search.error_message 'короткий запрос, минимум 3 символа'
        return false

      url = '/market/ads?a.q=' + request + '&a.rcvr=' + siomart.config.mart_id + '&' + siomart.geo.request_query_param()
      siomart.request.perform url

    queue_request : ( request ) ->

      if typeof siomart.search.search_timer != 'undefined'
        clearTimeout siomart.search.search_timer

      siomart.search.error_message '', true

      search_cb = () ->
        siomart.search.perform request.toLowerCase()

      siomart.search.search_timer = setTimeout search_cb, siomart.search.request_delay

    onfocus : () ->
      this.is_active = true
      siomart.utils.addClass siomart.utils.ge('smSearch'), '__active'

    onblur : () ->
      if this.is_active == false
        return false
      if siomart.utils.ge('smSearchField').value == ''
        siomart.search.exit()

    exit : () ->
      this.error_message '', true
      this.is_active = false
      siomart.utils.removeClass siomart.utils.ge('smSearch'), '__active'

      siomart.utils.ge('smSearchField').value = ''

      if siomart.search.found_count > 0
        siomart.grid_ads.load_index_ads()

    error_message : ( message, is_hide ) ->

      if this.is_active == false
        return false

      is_hide = is_hide || false

      em_dom = siomart.utils.ge('smSearchEm')

      if is_hide
        em_dom.style.display = 'none'
      else
        em_dom.style.display = 'block'

      em_dom.innerHTML = message


  ## Карточки ноды верхнего уровня
  grid_ads :

    load_more_scroll_delta : 100

    is_fully_loaded : false
    is_load_more_requested : false
    c_url : null

    loader :
      show : () ->
        siomart.utils.ge('smGridAdsLoader').style.display = 'block'
      hide : () ->
        siomart.utils.ge('smGridAdsLoader').style.display = 'none'

    attach_events : () ->
      ## Забиндить событие на скроллинг
      siomart.utils.add_single_listener siomart.utils.ge('smGridAdsWrapper'), 'scroll', () ->
        wrapper_scroll_top = siomart.utils.ge('smGridAdsWrapper').scrollTop
        content_height = siomart.utils.ge('smGridAdsContent').offsetHeight

        scroll_pixels_to_go = ( content_height - cbca_grid.wh ) - wrapper_scroll_top

        if scroll_pixels_to_go < siomart.grid_ads.load_more_scroll_delta
          siomart.grid_ads.load_more()

    adjust_dom : () ->

       grid_ads = siomart.utils.ge('smGridAds')
       grid_ads_wrapper = siomart.utils.ge('smGridAdsWrapper')
       grid_ads_content = siomart.utils.ge('smGridAdsContent')

       es = siomart.utils.ge('smCloseScreen')
       es_wrapper = siomart.utils.ge('smCloseScreenWrapper')
       es_content = siomart.utils.ge('smCloseScreenContent')

       grid_ads.style.height = es.style.height = cbca_grid.wh
       grid_ads_wrapper.style.height = es_wrapper.style.height = cbca_grid.wh
       grid_ads_content.style.minHeight = es_content.style.minHeight = cbca_grid.wh + 1

    load_more : () ->

      if this.is_load_more_requested == true || this.is_fully_loaded == true
        return false
      console.log 'load more'
      console.log 'loaded : ' + this.loaded

      this.is_load_more_requested = true

      console.log this.c_url

      siomart.request.perform this.c_url + '&a.size=' + siomart.config.ads_per_load + '&a.offset=' + this.loaded

    load_index_ads : () ->
      grd_c = siomart.utils.ge('smGridAdsContainer')
      url = grd_c.getAttribute 'data-index-offers-action'

      document.getElementById('smGridAdsWrapper').scrollTop = '0'

      siomart.grid_ads.is_fully_loaded = false
      siomart.grid_ads.is_load_more_requested = false

      siomart.grid_ads.loaded = 0

      if typeof siomart.grid_ads.multiplier == 'undefined'
        siomart.grid_ads.multiplier = 100000000000
      else
        siomart.grid_ads.multiplier = siomart.grid_ads.multiplier / 10

      siomart.grid_ads.c_url = url + '&a.gen=' + Math.floor((Math.random() * siomart.grid_ads.multiplier) + (Math.random() * 100000) ) + '&' + siomart.geo.request_query_param()

      console.log siomart.grid_ads.c_url

      siomart.request.perform siomart.grid_ads.c_url + '&a.size=' + siomart.config.ads_per_load

  #####################################################
  ## Добавить в DOM необходимую разметку для Sio.Market
  #####################################################

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
      console.log "НЕ УДАЛОСЬ ВЫПОЛНИТЬ ЗАПРОС"

    ## Выполнить запрос по указанному url
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

    siomart.log 'receive_response : got some data'
    siomart.warn data

    if typeof siomart.request.request_timeout_timer != 'undefined'
      clearTimeout siomart.request.request_timeout_timer

    ## Пришла пустота — уведомить юзера
    if data.html == ''
      siomart.log "КАРТОЧЕК НЕ НАЙДЕНО, ПОПРОБУЙТЕ ДРУГОЙ ЗАПРОС"
      return false

    ## Инициализация глагне
    if data.action == 'showcaseIndex'
      this.response_callbacks.showcase_index data

    if data.action == 'producerAds'
      this.response_callbacks.producer_ads data

    if data.action == 'findAds' || data.action == 'searchAds'

      if data.action == 'searchAds'
        cbca_grid.top_offset = 120
      else
        cbca_grid.top_offset = 70

      this.response_callbacks.find_ads data

    if data.action == 'findNodes'
      this.response_callbacks.find_nodes data


  #############################################
  ## Коллбеки для обработки результатов запроса
  #############################################
  response_callbacks :

    ###########################################
    ## Загрузка шаблона главной страницы выдачи
    ###########################################

    showcase_index : ( data ) ->

      siomart.log 'showcase_index()'

      ## Нарисовать разметку
      siomart.draw_layout()

      ## Забиндить оконные события
      siomart.bind_window_events()


      ## Инициализировать history api
      ## this.history.init()

      window.scrollTo 0,0

      siomart.utils.ge('sioMartRoot').style.display = 'block'

      container = siomart.utils.ge 'sioMartLayout'
      container.innerHTML = data.html
      siomart.utils.ge_tag('body')[0].style.overflow = 'hidden'

      siomart.grid_ads.adjust_dom()
      siomart.geo.adjust()
      siomart.geo.init_events()

      setTimeout siomart.grid_ads.attach_events, 200

      ## Инициализация welcome_ad
      ## если возвращается false — значит картинки нет и
      ## нет смысла тянуть с дальнейшей инициализацией

      if window.with_geo == true
        siomart.utils.ge('smExitButton').style.display = 'none'
        siomart.utils.ge('smGeoScreenButton').style.display = 'block'

      if siomart.welcome_ad.init() == false
        grid_init_timeout = 1
      else
        grid_init_timeout = siomart.welcome_ad.hide_timeout - 100

      grid_init_cb = () ->
        document.body.style.backgroundColor = '#ffffff'
        siomart.utils.ge('sioMartRoot').style.backgroundColor = "#ffffff"

        sm_wifi_info_dom = siomart.utils.ge('smWifiInfo')
        if sm_wifi_info_dom != null
          siomart.utils.ge('smWifiInfo').style.display = 'block'

        siomart.init_navigation()
        siomart.grid_ads.load_index_ads()


      setTimeout grid_init_cb, grid_init_timeout
      siomart.set_window_class()
      siomart.is_market_loaded = true

    ##########################################################
    ## Отобразить рекламные карточки для указанного продьюсера
    ##########################################################
    producer_ads : ( data ) ->

      if siomart.focused_ads.load_more_ads_requested == true
        siomart.focused_ads.render_more data.blocks
      else
        screensContainer = siomart.utils.ge 'smFocusedAds'
        screensContainer = siomart.utils.replaceHTMLandShow screensContainer, data.html

        if typeof data.blocks == 'undefined'
          siomart.focused_ads.ads = []
        else
          siomart.focused_ads.ads = data.blocks
        siomart.focused_ads.init()

        console.log 'producerAds : ready'

    ##############################
    ## Отобразить карточки в сетке
    ##############################
    find_ads : ( data ) ->

      grid_container_dom = siomart.utils.ge 'smGridAdsContainer'

      html = ''

      for index, elt of data.blocks
        if typeof elt == 'string'
          html += elt

      if typeof data.blocks != 'undefined'

        if data.action == 'searchAds'
          siomart.search.found_count++

        if data.blocks.length < siomart.config.ads_per_load
          siomart.grid_ads.is_fully_loaded = true
          siomart.grid_ads.loader.hide()
        else
          siomart.grid_ads.loader.show()

        siomart.grid_ads.loaded += data.blocks.length

        console.warn 'siomart.grid_ads.is_load_more_requested : ' + siomart.grid_ads.is_load_more_requested

        if siomart.grid_ads.is_load_more_requested == false
          grid_container_dom.innerHTML = html

          cbca_grid.init()
        else
          grid_container_dom.innerHTML += html
          cbca_grid.init(is_add = true)

        siomart.styles.init()

        if data.action == 'searchAds'
          siomart.navigation_layer.close true
        else
          siomart.navigation_layer.close()
      else

        if data.action == 'searchAds'
          siomart.search.error_message 'ничего не найдено!'

        siomart.grid_ads.is_fully_loaded = true
        siomart.grid_ads.loader.hide()

      siomart.grid_ads.is_load_more_requested = false

    find_nodes : ( data ) ->

      if typeof siomart.geo.nodes_data_cached != 'undefined'
        siomart.utils.ge('smGeoLocationButton').innerHTML = siomart.geo.nodes_data_cached.first_node.name

      siomart.geo.nodes_data_cached = data
      siomart.geo.nodes_loaded = true

      if siomart.geo.location_requested == true
        siomart.geo.location_requested = false
        siomart.utils.ge('smGeoLocationButton').innerHTML = data.first_node.name
        siomart.geo.load_for_node_id data.first_node._id

      siomart.utils.ge('smGeoNodesContent').innerHTML = data.nodes

  ############################################
  ## Объект для работы с карточками продьюсера
  ############################################
  focused_ads :
    load_more_ads_requested : false
    
    load_more_ads : () ->
      if this.is_fully_loaded == true
        return false
      siomart.request.perform this.curl + '&h=' + false + '&a.offset=' + this.ads.length
      this.load_more_ads_requested = true
    
    scroll_or_move : undefined
    
    show_ad_by_index : ( ad_index, direction ) ->

      if typeof this.ads == 'undefined'
        return false

      if typeof this.sm_blocks == 'undefined'
        return false

      if vendor_prefix.js == 'Webkit'
        siomart.focused_ads.ads_container_dom.style['-webkit-transform'] = 'translate3d(-' + cbca_grid.ww*ad_index + 'px, 0px, 0px)'
      else
        siomart.focused_ads.ads_container_dom.style['transform'] = 'translate3d(-' + cbca_grid.ww*ad_index + 'px, 0px, 0px)'
      
      siomart.focused_ads.ads_container_dom.setAttribute 'data-x-offset', -cbca_grid.ww*ad_index
      
      if ad_index == this.active_ad_index
        return false

      this.active_ad_index = ad_index
      
      console.log 'this.active_ad_index : ' + this.active_ad_index
      console.log 'this.blocks.length : ' + this.ads.length
      
      if this.active_ad_index > this.ads.length
        this.load_more_ads()
      
      if direction == '+'
        ad_c_el = siomart.utils.ge('focusedAd' + ( ad_index + 1 ) )
        if ad_c_el != null
          ad_c_el.style.visibility = 'visible';

        if ad_index >= 2
          siomart.utils.ge('focusedAd' + ( ad_index - 2 ) ).style.visibility = 'hidden';

      if direction == '-'
        if ad_index >= 1
          siomart.utils.ge('focusedAd' + ( ad_index - 1 ) ).style.visibility = 'visible';

        fel = siomart.utils.ge('focusedAd' + ( ad_index + 2 ) )
        if fel != null
          fel.style.visibility = 'hidden';

    next_ad : () ->
      if typeof this.active_ad_index == 'undefined'
        return false

      next_index = this.active_ad_index + 1

      if next_index == this.sm_blocks.length
        next_index = next_index-1
      this.show_ad_by_index next_index, '+'

    prev_ad : () ->

      if typeof this.active_ad_index == 'undefined'
        return false

      prev_index = this.active_ad_index - 1
      if prev_index < 0
        prev_index = 0

      this.show_ad_by_index prev_index, '-'

    ####################
    ## Обработка событий
    ####################
    touchstart_event : ( event ) ->
      ex = event.touches[0].pageX
      ey = event.touches[0].pageY

      this.tstart_x = ex
      this.tstart_y = ey

      this.last_x = ex

      siomart.utils.removeClass this.ads_container_dom, '__animated'

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

      c_x_offset = siomart.focused_ads.ads_container_dom.getAttribute 'data-x-offset'
      c_x_offset = parseInt c_x_offset

      if vendor_prefix.js == 'Webkit'
        siomart.focused_ads.ads_container_dom.style['-webkit-transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'
      else
        siomart.focused_ads.ads_container_dom.style['transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'

      this.x_delta_direction = this.last_x - ex

      this.last_x = ex

    touchend_event : ( event ) ->
      console.log 'touchend'
      siomart.utils.addClass this.ads_container_dom, '__animated'

      delete siomart.focused_ads.tstart_x
      delete siomart.focused_ads.tstart_y

      if this.x_delta_direction > 0
        cb = () ->
          siomart.focused_ads.next_ad()
      else
        cb = () ->
          siomart.focused_ads.prev_ad()

      if this.scroll_or_move == 'move'
        setTimeout cb, 1

      delete siomart.focused_ads.scroll_or_move

    touchcancel_event : ( event ) ->

      delete siomart.focused_ads.tstart_x
      delete siomart.focused_ads.tstart_y
      delete siomart.focused_ads.scroll_or_move

    render_more : ( more_ads ) ->

      this.load_more_ads_requested = false
      if typeof more_ads == 'undefined'
        return false

      html = ''
      for i, v of more_ads
        html += more_ads[i]
        this.ads.push more_ads[i]

      siomart.utils.ge('ads' + this.ads_receiver_index).innerHTML = html
      this.ads_rendered = this.ads.length + 1

      this.check_if_fully_loaded()
      this.render_ads_receiver()

      this.sm_blocks = sm_blocks = siomart.utils.ge_class this._container, 'sm-block'
      this.fit()
      siomart.styles.init()

    check_if_fully_loaded : () ->

      console.log 'this.ads_count : ' + this.ads_count
      console.log 'this.ads_rendered : ' + this.ads_rendered

      if parseInt( this.ads_count ) == parseInt( this.ads_rendered )
        this.is_fully_loaded = true
      else
        this.is_fully_loaded = false

    #############################################################
    ## Расставить необходимые размеры для различных дом элементов
    #############################################################
    fit : () ->

      if typeof this.sm_blocks == 'undefined'
        return false

      for _b in this.sm_blocks

        _block_width = _b.getAttribute 'data-width'

        if cbca_grid.ww >= 660
          siomart.utils.addClass _b, 'double-size'
          _block_width = _block_width*2
          padding = 0
        else
          siomart.utils.removeClass _b, 'double-size'
          padding = 0
          _block_width = 300

        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.width = cbca_grid.ww + 'px'

        _b.parentNode.parentNode.parentNode.style.width = parseInt( _block_width ) + padding + 'px'

        siomart.utils.addClass _b, '__rel'

        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.width = cbca_grid.ww + 'px'
        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 'px'
        _b.parentNode.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 'px'
        _b.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 1 + 'px'

      this.ads_container_dom.style.width = this.sm_blocks.length * cbca_grid.ww + 'px'

    render_ads_receiver : () ->

      console.warn 'this.is_fully_loaded : ' + this.is_fully_loaded

      if this.is_fully_loaded == true
        return false

      this.ads_receiver_index++
      attrs =
        'id' : 'ads' + this.ads_receiver_index

      _blocks_receiver_dom = siomart.utils.ce 'span', attrs
      _blocks_receiver_dom.innerHTML = this.loader_dom
      this.ads_container_dom.appendChild _blocks_receiver_dom


    ## Закрыть
    close : () ->
      siomart.utils.ge('smGridAds').style.webkitFilter = ""
      animation_cb = () ->
        siomart.utils.removeClass siomart.focused_ads._container, 'fs-animated-end'
      setTimeout animation_cb, 3

      cb = () ->
        siomart.utils.ge('smFocusedAds').style.display = 'none'
        siomart.focused_ads.ads_container_dom.innerHTML = ''

      setTimeout cb, 200

      delete siomart.focused_ads.active_ad_index
      delete siomart.shop_load_locked

    ############################
    ## Инициализация focused_ads
    ############################
    init : () ->
      siomart.utils.ge('smGridAds').style.webkitFilter = "blur(5px)"
      this.ads_container_dom = siomart.utils.ge('smFocusedAdsContainer')

      this.ads_receiver_index = 0
      this.is_fully_loaded = false
      this.render_ads_receiver()
      this.loader_dom = '<div id="focusedAdLoader" class="sm-flex sm-overflow-scrolling focused-ad">' + siomart.utils.ge('focusedAdLoader').innerHTML + '</div>'
      siomart.utils.re('focusedAdLoader')

      ## общее число карточек у продьюсера
      this.ads_count = this.ads_container_dom.getAttribute 'data-ads-count'
      this.ads_rendered = this.ads.length + 1

      html = ''
      for i, v of this.ads
        html += this.ads[i]

      siomart.utils.ge('ads' + this.ads_receiver_index).innerHTML = html
      this.check_if_fully_loaded()
      this.render_ads_receiver()

      this._container = siomart.utils.ge('smFocusedAds')

      ## Ввести экран с анимацией
      siomart.utils.addClass this._container, 'fs-animated-start'

      animation_cb = () ->
        siomart.utils.addClass siomart.focused_ads._container, 'fs-animated-end transition-animated'
      setTimeout animation_cb, 20

      ## События
      _e = if siomart.utils.is_touch_device() then 'touchend' else 'click'

      ## События для свайпа
      siomart.utils.add_single_listener this._container, 'touchstart', ( event ) ->
        siomart.focused_ads.touchstart_event event

      siomart.utils.add_single_listener this._container, 'touchmove', ( event ) ->
        siomart.focused_ads.touchmove_event event

      siomart.utils.add_single_listener this._container, 'touchcancel', ( event ) ->
        siomart.focused_ads.touchcancel_event event

      siomart.utils.add_single_listener this._container, 'touchend', ( event ) ->
        siomart.focused_ads.touchend_event event

      this.sm_blocks = sm_blocks = siomart.utils.ge_class this._container, 'sm-block'
      this.fit()

      this.active_ad_index = 0

  ##################################################
  ## Показать / скрыть экран с категориями и поиском
  ##################################################
  navigation_layer :

    tabs : ["smCategories", "smShops"]

    adjust : () ->

      if siomart.utils.ge('smNavLayerTabs') == null
        offset = 100
      else
        offset = 150

      for k, t of this.tabs
        if siomart.utils.ge(t) == null
          return false
        siomart.utils.ge(t).style.height = cbca_grid.wh - offset
        siomart.utils.ge(t + 'Wrapper').style.height = cbca_grid.wh - offset
        siomart.utils.ge(t + 'Content').style.height = cbca_grid.wh - ( offset - 1 )

    open : ( history_push ) ->

      this.adjust()

      siomart.utils.ge('smGridAds').style.webkitFilter = "blur(5px)"

      ## Скрыть кнопки хидера главного экрана
      siomart.utils.ge('smRootProducerHeaderButtons').style.display = 'none'

      if typeof history_push != 'boolean'
        history_push = true

      siomart.utils.ge('smCategoriesScreen').style.display = 'block'

      if history_push == true
        state_data =
          action : 'open_navigation_layer'
        siomart.history.push state_data, 'SioMarket', '/n/categories'

    reset_tabs : () ->
      this.show_tab this.tabs[0]

    show_tab : ( tab ) ->
      for k, t of this.tabs
        tab_content_dom = siomart.utils.ge(t)
        tab_dom = siomart.utils.ge(t + 'Tab')

        if tab_content_dom == null
          return false

        if tab == t
          tab_content_dom.style.display = 'block'
          siomart.utils.removeClass tab_dom, '__inactive'
        else
          tab_content_dom.style.display = 'none'
          siomart.utils.addClass tab_dom, '__inactive'

    close : ( all_except_search ) ->

      siomart.utils.ge('smGridAds').style.webkitFilter = ""

      if all_except_search == true
        siomart.utils.addClass siomart.utils.ge('smCategoriesScreen'), '__search-mode'
      else
        siomart.utils.removeClass siomart.utils.ge('smCategoriesScreen'), '__search-mode'
        this.reset_tabs()

        sm_cat_screen_dom = siomart.utils.ge('smCategoriesScreen')
        if sm_cat_screen_dom != null
          sm_cat_screen_dom.style.display = 'none'
        siomart.utils.ge('smRootProducerHeaderButtons').style.display = 'block'

    back : () ->
      console.log 'navigation layer back'


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
    a_rcvr = if siomart.config.mart_id == '' then '' else '&a.rcvr=' + siomart.config.mart_id

    url = '/market/fads?a.shopId=' + shop_id + '&a.gen=' + Math.floor((Math.random() * 100000000000) + 1) + '&a.size=' + siomart.config.producer_ads_per_load + a_rcvr + '&a.firstAdId=' + ad_id + '&' + siomart.geo.request_query_param()

    siomart.focused_ads.curl = url

    if history_push == true
      state_data =
        action : 'load_for_shop_id'
        shop_id : shop_id
        ad_id : ad_id
      siomart.history.push state_data, 'SioMarket', '/n/mart/' + shop_id + '/' + ad_id

    siomart.focused_ads.requested_ad_id = ad_id
    siomart.request.perform url

  ## Загрузить все офферы для магазина
  load_for_cat_id : ( cat_id, history_push, cat_class ) ->

    siomart.grid_ads.is_load_more_requested = false
    siomart.grid_ads.is_fully_loaded = false

    document.getElementById('smGridAdsWrapper').scrollTop = '0'

    siomart.utils.ge('smRootProducerHeader').className = 'sm-producer-header abs __w-global-cat ' + '__' + cat_class

    if typeof history_push != 'boolean'
      history_push = true

    if history_push == true
      state_data =
        action : 'load_for_cat_id'
        cat_id : cat_id
      siomart.history.push state_data, 'SioMarket', '/n/cat/' + cat_id

    url = '/market/ads?a.catId=' + cat_id + '&a.rcvr=' + siomart.config.mart_id  + '&' + siomart.geo.request_query_param()
    siomart.request.perform url

  ########################################
  ## картинка приветствия торгового центра
  ########################################
  welcome_ad :
    hide_timeout : 1700
    fadeout_transition_time : 700

    fit : ( image_dom ) ->
      if this.img_dom == null
        return false
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
      if this.img_dom == null
        return false
      siomart.utils.addClass siomart.welcome_ad.img_dom, '__fade-out'

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
    siomart.utils.add_single_listener window, 'touchcancel', siomart.events.document_touchcancel

    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'

    siomart.utils.add_single_listener document, _event, siomart.events.document_click

    ## Поиск
    _search_dom = siomart.utils.ge('smSearchField')
    siomart.utils.add_single_listener _search_dom, 'keyup', ( e ) ->
      siomart.search.queue_request this.value

    siomart.utils.add_single_listener _search_dom, 'focus', ( e ) ->
      siomart.search.onfocus()

    siomart.utils.add_single_listener _search_dom, 'blur', ( e ) ->
      siomart.search.onblur()

  set_window_class : () ->
    _window_class = ''

    if cbca_grid.ww <= 980
      _window_class = 'sm-w-980'

    if cbca_grid.ww <= 800
      _window_class = 'sm-w-800'

    if cbca_grid.ww <= 660
      _window_class = 'sm-w-400'

    siomart.utils.ge('sioMartLayout').className = _window_class

  ############################
  ## Функции для инициализации
  ############################

  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart : () ->
    this.define_per_load_values()

    siomart.log 'about to call index_action : ' + siomart.config.index_action
    this.request.perform siomart.config.index_action

  define_per_load_values : () ->

    cbca_grid.set_window_size()

    if cbca_grid.ww <= 980
      siomart.config.ads_per_load = 20

    if cbca_grid.ww <= 800
      siomart.config.ads_per_load = 10

    if cbca_grid.ww <= 660
      siomart.config.ads_per_load = 5

  ###########################
  ## Инициализация Sio.Market
  ###########################
  init : () ->
    siomart.config.mart_id = window.siomart_id
    siomart.config.host = window.siomart_host

    this.utils.set_vendor_prefix()

    #siomart.geo.init()
    siomart.load_mart()

window.siomart = siomart
siomart.init()