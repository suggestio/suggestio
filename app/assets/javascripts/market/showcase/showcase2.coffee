cbca_grid =
  cell_size : 140
  cell_padding : 20
  top_offset : 70
  bottom_offset : 20
  max_allowed_cell_width : 4
  left_offset : 0
  right_offset : 0
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
    this.count_columns()

    cw = this.columns * ( this.cell_size + this.cell_padding) - this.cell_padding
    cm = 0
    this.max_allowed_cell_width = this.columns

    this.cw = cw

    if this.left_offset > 0
      margin = this.left_offset*( this.cell_size + this.cell_padding) - this.cell_padding
      cw = cw - margin
      cm = margin

    if this.right_offset > 0
      margin = this.right_offset*( this.cell_size + this.cell_padding) - this.cell_padding
      cw = cw - margin
      cm = -margin

    if typeof this.layout_dom != 'undefined'
      this.layout_dom.style.width = cw + 'px'
      this.layout_dom.style.left = cm/2 + 'px'
      this.layout_dom.style.opacity = 1

      sm.utils.ge('smGridAdsLoader').style.width = cw + 'px'

    this.columns = this.columns - this.left_offset - this.right_offset

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

        _elt = sm.utils.ge 'elt' + b.id

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

        _elt = sm.utils.ge 'elt' + b.id

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

    _elt = sm.utils.ge 'elt' + block_id

    sm.utils.addClass _elt, 'animated-block'
    _elt.style.opacity = 1

  deactivate_block : ( block_id, target_opacity ) ->

    block = sm.utils.ge block_id

    block.style.opacity = target_opacity

    if sm.utils.is_array block.className.match /active-block/g
      sm.utils.removeClass block, 'active-block'
      block_js_class = block.getAttribute 'data-js-class'

      bs = sm.utils.ge_class block, '.block-source'
      cb2 = () ->
        bs.style['visibility'] = 'hidden'
        sm.utils.removeClass block, 'no-bg'
        sm.utils.removeClass block, 'hover'

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

    if is_add == true
      i = cbca_grid.blocks_index
    else
      cbca_grid.all_blocks = []
      cbca_grid.spacers = []
      cbca_grid.m_spacers = []
      i = 0

    ## TODO : make selector configurable

    for elt in sm.utils.ge_class document, 'sm-block'

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
    #for i in sm.utils.ge_class document, 'sm-b-spacer'
    if is_add == false
      for k in [1..300]

        _spacer_attributes =
          'class' : 'sm-b-spacer sm-b-spacer-' + k
          'data-width' : 140
          'data-height' : 140

        _spacer = sm.utils.ce 'div', _spacer_attributes
        _this = _spacer

        sm.utils.ge('smGridAdsContainer').appendChild _spacer

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

    if typeof cbca_grid.all_blocks != 'undefined'
      cbca_grid.m_blocks = cbca_grid.all_blocks.slice(0)
      cbca_grid.blocks = cbca_grid.m_blocks

      this.build()

  count_columns : () ->
    # Определеяем сколько колонок влезет в экран колонок
    this.columns = Math.floor( ( this.ww - this.cell_padding ) / ( this.cell_size + this.cell_padding) )

    if this.columns % 2 == 1
      this.columns--

    if this.columns < 2
      this.columns = 2

    if this.columns > 8
      this.columns = 8

  rebuild : () ->
    cbca_grid.set_container_size()

    if typeof cbca_grid.all_blocks == 'undefined' || cbca_grid.all_blocks.length == 0
      return false

    cbca_grid.m_blocks = cbca_grid.all_blocks.slice(0)
    cbca_grid.blocks = cbca_grid.m_blocks
    cbca_grid.build()

  build : ( is_add ) ->

    is_add = is_add || false

    for elt in sm.utils.ge_class document, 'blocks-container'
      elt.style.display = 'block'

    if is_add == false
      for elt in sm.utils.ge_class document, 'sm-b-spacer'
        elt.style.display = 'none'

    blocks_length = cbca_grid.blocks.length

    # setting up left and top
    left_pointer = left_pointer_base = 0
    top_pointer = 0

    # Определяем ширину окна
    window_width = this.ww

    # Ставим указатели строки и колонки
    cline = 0
    pline = 0
    cur_column = 0

    if is_add == false
      # Генерим объект с инфой об использованном месте
      columns_used_space = {}
      for c in [0..this.columns-1]
        columns_used_space[c] =
          used_height : 0
    else
      columns_used_space = cbca_grid.columns_used_space

    is_break = false

    ## Генерим поле
    for i in [0..1000]

      pline = cline

      if cur_column >= Math.floor this.columns
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
          block_max_w = this.get_max_block_width columns_used_space, cline, cur_column, this.columns

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

          if _pelt != null

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
      b_elt = sm.utils.ge('elt' + bid )

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
sm =
  config :
    whitelisted_domains : ['suggest.io', 'localhost:9000', '192.168.199.148:9000']
    index_action : window.siomart_index
    sm_layout_class : 'sm-showcase'
    ontouchmove_offer_change_delta : 80
    welcome_ad_hide_timeout : 2000
    ads_per_load : 30
    producer_ads_per_load : 2
    sio_hostnames : ["suggest.io", "localhost", "192.168.199.*"]

  geo :
    loaded : false
    location_requested : false
    nodes_loaded : false
    active_layer : null
    screen_offset : 129
    layers_count : 2
    layer_dom_height : 44
    requested_node_id : undefined
    position_callback_timeout : 10000

    position_callback : ( gp_obj ) ->
      console.log 'position_callback'
      sm.geo.geo_position_obj = gp_obj
      sm.geo.load_nodes_and_reload_with_mart_id()

    position_callback_fallback : () ->
      console.log 'position_callback_fallback'
      if typeof sm.geo.geo_position_obj == 'undefined'
        sm.geo.load_for_node_id()

    open_layer : ( index ) ->

      sm.geo.layers_count = parseInt( sm.utils.ge('geoNodesListContainer').getAttribute('data-layers-count') )

      if sm.geo.active_layer != null
        sm.utils.removeClass sm.utils.ge('geoLayer' + sm.geo.active_layer), '__active'
        sm.utils.addClass sm.utils.ge('geoLayerNodes' + sm.geo.active_layer), '__hidden'

      if sm.geo.active_layer != index
        sm.geo.active_layer = index

        layer_nodes_dom = sm.utils.ge('geoLayerNodes' + sm.geo.active_layer)
        layer_nodes_dom_wrapper = sm.utils.ge('geoLayerNodes' + sm.geo.active_layer + 'Wrapper')
        layer_nodes_dom_content = sm.utils.ge('geoLayerNodes' + sm.geo.active_layer + 'Content')

        sm.utils.addClass sm.utils.ge('geoLayer' + sm.geo.active_layer), '__active'
        sm.utils.removeClass layer_nodes_dom, '__hidden'

        layer_nodes_dom_height = layer_nodes_dom.offsetHeight
        max_height = ( cbca_grid.wh - sm.geo.screen_offset - parseInt( sm.geo.layers_count + 1 ) * sm.geo.layer_dom_height )

        if layer_nodes_dom_height > max_height
          layer_nodes_dom.style.height = max_height + 'px'
          layer_nodes_dom_wrapper.style.height = max_height + 'px'
          layer_nodes_dom_content.style.minHeight = max_height + 1 + 'px'
        else
          layer_nodes_dom_wrapper.style.height = layer_nodes_dom_height + 'px'
          layer_nodes_dom_content.style.minHeight = layer_nodes_dom_height + 1 + 'px'

      else
        sm.geo.active_layer = null

    get_current_position : () ->
      if this.location_requested == true
        return false

      if sm.utils.ge('smGeoLocationButtonIcon') != null
        sm.utils.ge('smGeoLocationButtonIcon').style.display = 'none'
        sm.utils.ge('smGeoLocationButtonSpinner').style.display = 'block'

      sm.geo.location_requested = true

      if typeof navigator.geolocation != 'undefined'
        if sm.utils.is_webkit() == true
          navigator.geolocation.getCurrentPosition sm.geo.position_callback, sm.geo.position_callback_fallback
        else
          sm.geo.position_callback_fallback()
          navigator.geolocation.getCurrentPosition sm.geo.position_callback

      else
        sm.geo.position_callback_fallback()

    init_events : () ->
      _geo_nodes_search_dom = sm.utils.ge('smGeoSearchField')
      sm.utils.add_single_listener _geo_nodes_search_dom, 'keyup', ( e ) ->
        sm.log sm.geo.search.queue_request this.value

    load_for_node_id : ( node_id ) ->
      sm.warn 'load_for_node_id'
      cs = sm.geo.cur_state
      sm.states.add_state
        mart_id : node_id

    adjust : () ->

      geo_screen = sm.utils.ge('smGeoNodes')
      geo_screen_wrapper = sm.utils.ge('smGeoNodesWrapper')
      geo_screen_content = sm.utils.ge('smGeoNodesContent')

      geo_screen.style.height = cbca_grid.wh - sm.geo.screen_offset
      geo_screen_wrapper.style.height = cbca_grid.wh - sm.geo.screen_offset
      geo_screen_content.style.minHeight = cbca_grid.wh - sm.geo.screen_offset + 1

    #############################
    ## Открыть экран с гео добром
    #############################
    open_screen : () ->

      gs = sm.utils.ge('smGeoScreen')

      if gs.style.display == 'block'
        return false

      sm.utils.ge('smGeoScreen').style.display = 'block'
      sm.utils.ge('smRootProducerHeaderButtons').style.display = 'none'

      sm.rebuild_grid()

      sm.log 'open screen'

      siomart.utils.ge('smGeoScreenButton').style.display = 'none'
      sm.geo.load_nodes()

    close_screen : () ->

      gs = sm.utils.ge('smGeoScreen')

      if gs == null || gs.style.display == '' || gs.style.display == 'none'
        return false

      gs.style.display = 'none'
      sm.utils.ge('smRootProducerHeaderButtons').style.display = 'block'
      cbca_grid.left_offset = 0
      cbca_grid.rebuild()

      siomart.utils.ge('smGeoScreenButton').style.display = 'block'

    request_query_param : () ->

      if window.with_geo == false
        return ""

      if typeof sm.geo.geo_position_obj == 'undefined'
        "a.geo=ip"
      else
        "a.geo=" + this.geo_position_obj.coords.latitude + "," + this.geo_position_obj.coords.longitude

    load_nodes_and_reload_with_mart_id : () ->

      if sm.utils.ge('smGeoLocationButtonIcon') != null
        sm.utils.ge('smGeoLocationButtonIcon').style.display = 'block'
        sm.utils.ge('smGeoLocationButtonSpinner').style.display = 'none'

      cs = sm.states.cur_state()
      node_query_param = if typeof cs != 'undefined' && cs.mart_id then '&a.cai=' + cs.mart_id else ''
      nodesw = '&a.nodesw=true'

      url = '/market/nodes/search?' + this.request_query_param() + node_query_param + nodesw
      sm.request.perform url

    load_nodes : () ->

      cs = sm.states.cur_state()
      node_query_param = if cs.mart_id then '&a.cai=' + cs.mart_id else ''

      nodesw = ''

      url = '/market/nodes/search?' + this.request_query_param() + node_query_param + nodesw
      sm.request.perform url

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

      sm.welcome_ad.fit sm.welcome_ad._bg_img_dom
      sm.welcome_ad.fit sm.welcome_ad._fg_img_dom, true
      window.scrollTo(0,0)

      if typeof sm.window_resize_timer != 'undefined'
        clearTimeout sm.window_resize_timer

      grid_resize = () ->

        document.getElementById('smGridAdsWrapper').scrollTop = '0';

        cbca_grid.resize()
        sm.set_window_class()

        sm.focused_ads.fit()
        sm.focused_ads.show_ad_by_index sm.focused_ads.active_ad_index

        sm.grid_ads.adjust_dom()

        sm.geo.adjust()

      sm.window_resize_timer = setTimeout grid_resize, 300

    this.utils.add_single_listener window, 'resize', resize_cb

  styles :

    style_dom : null

    init : () ->

      style_tags = sm.utils.ge_tag('code', true)

      css = ''

      for s in style_tags
        if s.getAttribute( 'data-rendered' ) == null
          s.setAttribute( 'data-rendered', true )
          css = css.concat( s.innerHTML )

      style_dom = document.createElement('style')
      style_dom.type = "text/css"
      sm.utils.ge_tag('head')[0].appendChild(style_dom)
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
      if state != null
        sm.states.goto state.state_index

    push : ( data, title, path ) ->
      history.pushState data, title, this.base_path + '#' + path

    init : () ->
      this.base_path = window.location.pathname

      if !this.is_supported()
        return false

      sm.utils.add_single_listener window, 'popstate', ( event ) ->
        sm.history.navigate event.state

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
    return false
    console[fun](message)

  utils :
    elts_cache : {}
    is_firefox : () ->
      navigator.userAgent.toLowerCase().indexOf('firefox') > -1
    is_webkit : () ->
      if typeof document.documentElement.style['WebkitAppearance'] == 'undefined'
        return false
      else
        return true
    is_touch_device : () ->
      if document.ontouchstart != null
        false
      else
        if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          false
        else
          true

    is_sio_host : () ->
      for hn in sm.config.sio_hostnames
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
        if typeof child.className != 'undefined'
          _className = if typeof child.className.baseVal != 'undefined' then child.className.baseVal else child.className
          if typeof _className != 'undefined'
            if sm.utils.is_array _className.match _class_match_regexp
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

      if element==null || typeof element == 'undefined'
        return 0

      if !element.className
        element.className = ''
      else
        newClassName = element.className.replace(value,'').replace(/\s{2,}/g, ' ')
        element.className = newClassName

    ############################
    ## добавить класс для объекта
    ############################
    addClass : (elm, className) ->

      if document.documentElement.classList
        addClass = (elm, className) ->
          elm.classList.add className
      else
        addClass = (elm, className) ->
          if !elm
            return false

          if !sm.utils.containsClass(elm, className)
            elm.className += (elm.className ? " " : "") + className

      addClass elm, className

    containsClass : (elm, className) ->

      if document.documentElement.classList && elm.classList
        containsClass = (elm, className) ->
          return elm.classList.contains className
      else
        if !elm || !elm.className
          return false
        containsClass = (elm, className) ->
          if typeof elm.className != 'string'
            return false
          re = new RegExp('(^|\\s)' + className + '(\\s|$)');
          return elm.className.match re

      return containsClass elm, className

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

    rgb_to_hsl : ( rgb ) ->

      r1 = rgb[0] / 255
      g1 = rgb[1] / 255
      b1 = rgb[2] / 255

      maxColor = Math.max(r1, g1, b1)
      minColor = Math.min(r1, g1, b1)

      L = (maxColor + minColor) / 2
      S = 0
      H = 0

      unless maxColor is minColor

        #Calculate S:
        if L < 0.5
          S = (maxColor - minColor) / (maxColor + minColor)
        else
          S = (maxColor - minColor) / (2.0 - maxColor - minColor)

        #Calculate H:
        if r1 is maxColor
          H = (g1 - b1) / (maxColor - minColor)
        else if g1 is maxColor
          H = 2.0 + (b1 - r1) / (maxColor - minColor)
        else
          H = 4.0 + (r1 - g1) / (maxColor - minColor)


      L = L * 100
      S = S * 100
      H = H * 60

      H += 360  if H < 0

      result = [
        H
        S
        L
      ]
      result

  rebuild_grid : ( do_rebuild ) ->

    do_rebuild = do_rebuild || true

    if cbca_grid.columns > 2 || ( cbca_grid.left_offset != 0 || cbca_grid.right_offset != 0 )
      sm_geo_screen = sm.utils.ge('smGeoScreen')

      if sm_geo_screen != null
        if sm_geo_screen.style.display == "" || sm_geo_screen.style.display == "none"
          cbca_grid.left_offset = 0
        else
          sm_geo_screen.style.width = 280 + Math.round((cbca_grid.ww - parseInt(cbca_grid.cw)) / 2)
          cbca_grid.left_offset = 2
      else
        cbca_grid.left_offset = 0
    else
      cbca_grid.left_offset = 0

    if cbca_grid.columns > 2 || ( cbca_grid.left_offset != 0 || cbca_grid.right_offset != 0 )
      sm_cat_screen = sm.utils.ge('smCategoriesScreen')
      if sm_cat_screen != null
        if sm_cat_screen.style.display == "" || sm_cat_screen.style.display == "none"
          cbca_grid.right_offset = 0
        else
          sm_cat_screen.style.width = 300 + Math.round((cbca_grid.ww - parseInt(cbca_grid.cw)) / 2)
          cbca_grid.right_offset = 2
      else
        cbca_grid.right_offset = 0
    else
      cbca_grid.right_offset = 0

    sm.warn 'cbca_grid.left_offset : ' + cbca_grid.left_offset
    sm.warn 'cbca_grid.right_offset : ' + cbca_grid.right_offset

    if do_rebuild == true
      cbca_grid.rebuild()

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

          if sm.utils.containsClass target, lookup_criteria
            return target

      return sm.events.target_lookup target.parentNode, lookup_method, lookup_criteria

    touchmove_lock_delta : 0
    is_touch_locked : false

    document_touchmove : ( event ) ->

      cx = event.touches[0].pageX
      cy = event.touches[0].pageY

      sm.events.document_touch_x_delta = sm.events.document_touch_x - cx
      sm.events.document_touch_y_delta = sm.events.document_touch_y - cy

      if Math.abs( sm.events.document_touch_x_delta ) > sm.events.touchmove_lock_delta || Math.abs( sm.events.document_touch_y_delta ) > sm.events.touchmove_lock_delta || typeof sm.events.document_touch_x == 'undefined'
        sm.events.is_touch_locked = true

      if typeof sm.events.document_touch_x == 'undefined'
        sm.events.document_touch_x = cx
        sm.events.document_touch_y = cy

    document_touchend : ( event ) ->

      cb = () ->
        sm.events.is_touch_locked = false
        delete sm.events.document_touch_x
        delete sm.events.document_touch_y

      setTimeout cb, 100

    document_touchcancel : ( event ) ->
      sm.events.is_touch_locked = false
      delete sm.events.document_touch_x
      delete sm.events.document_touch_y

    #############################
    ## Обработка click / touchend
    #############################
    document_click : ( event ) ->

      #if sm.events.is_touch_locked
      #  return false

      ## Обработка событий для открытия / закрытия экрана выхода
      if sm.events.target_lookup( event.target, 'id', 'smExitButton' ) != null
        sm.utils.ge('smCloseScreen').style.display = 'block'
        return false

      if sm.events.target_lookup( event.target, 'id', 'smExitCloseScreenButton' ) != null
        sm.utils.ge('smCloseScreen').style.display = 'none'
        return false

      if sm.events.target_lookup( event.target, 'id', 'smCloseScreenContainer' ) != null
        event.preventDefault()
        return false

      if sm.events.target_lookup( event.target, 'id', 'smCloseScreen' ) != null
        sm.utils.ge('smCloseScreen').style.display = 'none'
        return false

      #########################################################
      ## Элементы, отвечающие за изменение состояния geo screen
      #########################################################

      ## Кнопка для определения текущей геопозиции, напрямую на влияет на состояние выдачи
      if sm.events.target_lookup( event.target, 'id', 'smGeoLocationButton' ) != null
        if sm.events.is_touch_locked
          return false

        sm.geo.location_requested = false
        sm.geo.location_node = undefined
        setTimeout sm.geo.get_current_position, 200
        return false

      ## Кнопка закрытия экрана geo
      if sm.events.target_lookup( event.target, 'id', 'smGeoScreenCloseButton' ) != null
        sm.states.transform_state { geo_screen : { is_opened : false } }
        return false

      ## Логотип-кнока
      if ( sm.events.target_lookup( event.target, 'className', 'sm-producer-header_txt-logo' ) != null ) || ( sm.events.target_lookup( event.target, 'id', 'smGeoScreenButton' ) != null )
        cs = sm.states.cur_state()
        sm.states.requested_geo_id = cs.mart_id
        geogoBack = document.getElementById('smRootProducerHeader').getAttribute 'data-gl-go-back'

        if geogoBack == "false"
          sm.states.gb_mart_id = cs.mart_id
        else
          if cs.mart_id == sm.geo.location_node._id
            sm.states.gb_mart_id = cs.mart_id

        if typeof sm.geo.location_node == 'undefined' || ( typeof sm.geo.location_node == 'object' && cs.mart_id == sm.geo.location_node._id ) || geogoBack == "false"
          sm.states.transform_state { geo_screen : { is_opened : true } }
        else
          sm.states.add_state
            #mart_id : sm.geo.location_node._id
            mart_id : sm.states.gb_mart_id
            with_welcome_ad : false
            geo_screen :
              is_opened : true
        return false

      ## Юзер нажал на ноду в списке
      geo_node_target = sm.events.target_lookup( event.target, 'className', 'js-geo-node' )
      if geo_node_target != null

        if sm.events.is_touch_locked
          return false

        node_id = geo_node_target.getAttribute 'data-id'

        sm.states.add_state {mart_id : node_id}

        return false

      ##########################################################################################
      ## Элементы, отвечающие за изменение состояния выдачи в зависимости от выбранной категорий
      ##########################################################################################

      if sm.events.target_lookup( event.target, 'id', 'smIndexButton' ) != null || sm.events.target_lookup( event.target, 'id', 'smCategoriesIndexButton' ) != null
        sm.states.transform_state
          cat_id : undefined
          cat_class : undefined
          cat_screen :
            is_opened : false
        return false

      shop_link_target = sm.events.target_lookup( event.target, 'className', 'js-shop-link' )
      if shop_link_target != null

        if sm.events.is_touch_locked
          return false

        producer_id = shop_link_target.getAttribute 'data-producer-id'
        ad_id = shop_link_target.getAttribute 'data-ad-id'

        sm.load_for_shop_id producer_id, ad_id

        return false

      ## Кнопка открытия экрана с выдачей категории
      cat_link_target = sm.events.target_lookup( event.target, 'className', 'js-cat-link' )
      if cat_link_target != null

        if sm.events.is_touch_locked
          return false

        cat_id = cat_link_target.getAttribute 'data-cat-id'
        cat_class = cat_link_target.getAttribute 'data-cat-class'

        _cat_class_match_regexp = new RegExp( 'disabled' ,"g")
        if !sm.utils.is_array( cat_link_target.className.match( _cat_class_match_regexp ) )

          if cbca_grid.ww <= 400
            ns =
              cat_screen :
                is_opened :false
              cat_id : cat_id
              cat_class : cat_class
          else
            ns =
              cat_id : cat_id
              cat_class : cat_class

          sm.states.transform_state ns

      #######################
      ## Работа с категориями
      #######################
      ## Открыть экран с категориями
      if sm.events.target_lookup( event.target, 'id', 'smNavigationLayerButton' ) != null
        sm.states.transform_state { cat_screen : {is_opened : true }, geo_screen : {is_opened : false }}
        return false

      ## Переключить таб
      if sm.events.target_lookup( event.target, 'id', 'smCategoriesTab' ) != null
        sm.navigation_layer.show_tab 'smCategories'

      if sm.events.target_lookup( event.target, 'id', 'smShopsTab' ) != null
        sm.navigation_layer.show_tab 'smShops'

      ## Кнопка закрытия экрана с категориями
      if sm.events.target_lookup( event.target, 'id', 'smCategoriesScreenCloseButton' ) != null
        sm.states.transform_state { cat_screen : {is_opened : false }}
        return false

      ##############
      ## focused_ads
      ##############
      if sm.events.target_lookup( event.target, 'id', 'closeFocusedAdsButton' ) != null

        cs = sm.states.cur_state()
        sm.states.transform_state
          cat_id : cs.cat_id
          cat_class : cs.cat_class
          fads :
            is_opened : false
        return false

      target = sm.events.target_lookup( event.target, 'className', 'geo-nodes-list_layer' )
      if target != null
        if sm.events.is_touch_locked
          return false
        index = target.getAttribute 'data-index'
        sm.geo.open_layer( index )


    #############################
    ## Обработка keyup
    #############################
    document_keyup : ( event ) ->
      #esc
      if event.keyCode == 27
        sm.focused_ads.close()

      #left arrow
      if event.keyCode == 37
        sm.focused_ads.prev_ad()

      #right arrow
      if event.keyCode == 39
        sm.focused_ads.next_ad()


    document_keyup_event : ( event ) ->

      if !event
        return false

      ## Exc button
      if event.keyCode == 27
        sm.close_focused_ads()

      if event.keyCode == 39
        sm.focused_ads.next_ad()

      if event.keyCode == 37
        sm.focused_ads.prev_ad()


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
        sm.search.error_message 'короткий запрос, минимум 3 символа'
        return false

      sm.states.transform_state
        search_request : request

    queue_request : ( request ) ->

      if typeof sm.search.search_timer != 'undefined'
        clearTimeout sm.search.search_timer

      sm.search.error_message '', true

      search_cb = () ->
        sm.search.perform request.toLowerCase()

      sm.search.search_timer = setTimeout search_cb, sm.search.request_delay

    onfocus : () ->
      this.is_active = true
      sm.utils.addClass sm.utils.ge('smSearch'), '__active'

    onblur : () ->
      if this.is_active == false
        return false
      if sm.utils.ge('smSearchField').value == ''
        sm.search.exit()

    exit : () ->
      this.error_message '', true
      this.is_active = false
      sm.utils.removeClass sm.utils.ge('smSearch'), '__active'

      sm.utils.ge('smSearchField').value = ''

      if sm.search.found_count > 0
        sm.grid_ads.load_index_ads()

    error_message : ( message, is_hide ) ->

      if this.is_active == false
        return false

      is_hide = is_hide || false

      em_dom = sm.utils.ge('smSearchEm')

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
        sm.utils.ge('smGridAdsLoader').style.display = 'block'
      hide : () ->
        sm.utils.ge('smGridAdsLoader').style.display = 'none'

    attach_events : () ->
      ## Забиндить событие на скроллинг
      sm.utils.add_single_listener sm.utils.ge('smGridAdsWrapper'), 'scroll', () ->
        wrapper_scroll_top = sm.utils.ge('smGridAdsWrapper').scrollTop
        content_height = sm.utils.ge('smGridAdsContent').offsetHeight

        scroll_pixels_to_go = ( content_height - cbca_grid.wh ) - wrapper_scroll_top

        if scroll_pixels_to_go < sm.grid_ads.load_more_scroll_delta
          sm.grid_ads.load_more()

    adjust_dom : () ->

       grid_ads = sm.utils.ge('smGridAds')
       grid_ads_wrapper = sm.utils.ge('smGridAdsWrapper')
       grid_ads_content = sm.utils.ge('smGridAdsContent')

       es = sm.utils.ge('smCloseScreen')
       es_wrapper = sm.utils.ge('smCloseScreenWrapper')
       es_content = sm.utils.ge('smCloseScreenContent')

       grid_ads.style.height = es.style.height = cbca_grid.wh
       grid_ads_wrapper.style.height = es_wrapper.style.height = cbca_grid.wh
       grid_ads_content.style.minHeight = es_content.style.minHeight = cbca_grid.wh + 1

    load_more : () ->
      if this.is_load_more_requested == true || this.is_fully_loaded == true
        return false
      this.is_load_more_requested = true

      sm.request.perform this.c_url + '&a.size=' + sm.config.ads_per_load + '&a.offset=' + this.loaded

    load_index_ads : () ->

      grd_c = sm.utils.ge('smGridAdsContainer')
      url = grd_c.getAttribute 'data-index-offers-action'

      document.getElementById('smGridAdsWrapper').scrollTop = '0'

      sm.grid_ads.is_fully_loaded = false
      sm.grid_ads.is_load_more_requested = false

      sm.grid_ads.loaded = 0

      if typeof sm.grid_ads.multiplier == 'undefined'
        sm.grid_ads.multiplier = 100000000000
      else
        sm.grid_ads.multiplier = sm.grid_ads.multiplier / 10

      url = url.replace '&a.geo=ip', ''
      sm.grid_ads.c_url = url + '&a.gen=' + Math.floor((Math.random() * sm.grid_ads.multiplier) + (Math.random() * 100000) ) + '&' + sm.geo.request_query_param()

      sm.request.perform sm.grid_ads.c_url + '&a.size=' + sm.config.ads_per_load

  #####################################################
  ## Добавить в DOM необходимую разметку для Sio.Market
  #####################################################

  draw_layout : () ->

    if sm.utils.ge('sioMartRoot') != null
      sm.utils.re('sioMartRoot')

    sm.geo.active_layer = null

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
      sm.log "НЕ УДАЛОСЬ ВЫПОЛНИТЬ ЗАПРОС"

    ## Выполнить запрос по указанному url
    perform : ( url ) ->

      timeout_cb = () ->
        sm.request.on_request_error()

      sm.request.request_timeout_timer = setTimeout timeout_cb, sm.request.request_timeout

      js_request_attrs =
        type : 'text/javascript'
        src : sm.config.host + url
      js_request = sm.utils.ce "script", js_request_attrs
      sm.utils.ge_tag("head")[0].appendChild js_request

  ##################################################
  ## Получить результаты по последнему отправленному
  ## зпросу и передать их в нужный callback
  ##################################################
  receive_response : ( data ) ->

    sm.log 'receive_response : got some data'
    sm.warn data

    if typeof sm.request.request_timeout_timer != 'undefined'
      clearTimeout sm.request.request_timeout_timer

    ## Пришла пустота — уведомить юзера
    if data.html == ''
      sm.log "КАРТОЧЕК НЕ НАЙДЕНО, ПОПРОБУЙТЕ ДРУГОЙ ЗАПРОС"
      return false

    ## Инициализация глагне
    if data.action == 'showcaseIndex'
      this.response_callbacks.showcase_index data

    if data.action == 'producerAds'
      this.response_callbacks.producer_ads data

    if data.action == 'findAds' || data.action == 'searchAds'
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

      sm.log 'showcase_index()'

      ## Нарисовать разметку
      sm.draw_layout()

      ## Забиндить оконные события
      sm.bind_window_events()

      window.scrollTo 0,0

      sm.utils.ge('sioMartRoot').style.display = 'block'

      container = sm.utils.ge 'sioMartLayout'
      container.innerHTML = data.html
      sm.utils.ge_tag('body')[0].style.overflow = 'hidden'

      cs = sm.states.cur_state()
      if data.curr_adn_id != null && typeof cs.mart_id == 'undefined'
        sm.states.update_state
          mart_id : data.curr_adn_id

      sm.grid_ads.adjust_dom()
      sm.geo.adjust()
      sm.geo.init_events()

      setTimeout sm.grid_ads.attach_events, 200

      ## Инициализация welcome_ad
      ## если возвращается false — значит картинки нет и
      ## нет смысла тянуть с дальнейшей инициализацией

      if window.with_geo == true
        sm.utils.ge('smExitButton').style.display = 'none'
        sm.utils.ge('smGeoScreenButton').style.display = 'block'

      if sm.welcome_ad.init() == false
        grid_init_timeout = 1
      else
        grid_init_timeout = sm.welcome_ad.hide_timeout - 100

      grid_init_cb = () ->
        document.body.style.backgroundColor = '#ffffff'
        sm.utils.ge('sioMartRoot').style.backgroundColor = "#ffffff"

        sm_wifi_info_dom = sm.utils.ge('smWifiInfo')
        if sm_wifi_info_dom != null
          sm.utils.ge('smWifiInfo').style.display = 'block'

        sm.init_navigation()
        sm.grid_ads.load_index_ads()

      setTimeout grid_init_cb, grid_init_timeout
      sm.set_window_class()
      sm.is_market_loaded = true

      if typeof sm.states.requested_state != 'undefined'
        sm.states.process_state_2 sm.states.requested_state


    ##########################################################
    ## Отобразить рекламные карточки для указанного продьюсера
    ##########################################################
    producer_ads : ( data ) ->

      siomart.utils.ge('fsLoaded').style.display = 'none'

      if sm.focused_ads.load_more_ads_requested == true
        sm.focused_ads.render_more data.blocks
      else
        screensContainer = sm.utils.ge 'smFocusedAds'
        screensContainer = sm.utils.replaceHTMLandShow screensContainer, data.html

        if typeof data.blocks == 'undefined'
          sm.focused_ads.ads = []
        else
          sm.focused_ads.ads = data.blocks
        sm.focused_ads.init()

    ##############################
    ## Отобразить карточки в сетке
    ##############################
    find_ads : ( data ) ->

      grid_container_dom = sm.utils.ge 'smGridAdsContainer'

      html = ''

      for index, elt of data.blocks
        if typeof elt == 'string'
          html += elt

      if typeof data.blocks != 'undefined'

        if data.action == 'searchAds'
          sm.search.found_count++

        if data.blocks.length < sm.config.ads_per_load
          sm.grid_ads.is_fully_loaded = true
          sm.grid_ads.loader.hide()
        else
          sm.grid_ads.loader.show()

        sm.grid_ads.loaded += data.blocks.length

        sm.rebuild_grid( false )
        if sm.grid_ads.is_load_more_requested == false
          grid_container_dom.innerHTML = html
          cbca_grid.init()
        else
          grid_container_dom.innerHTML += html
          cbca_grid.init(is_add = true)

        sm.styles.init()

      else

        if data.action == 'searchAds'
          sm.search.error_message 'ничего не найдено!'

        sm.grid_ads.is_fully_loaded = true
        sm.grid_ads.loader.hide()

      sm.grid_ads.is_load_more_requested = false

    find_nodes : ( data ) ->

      smGeoLabel = sm.utils.ge('smGeoLocationLabel')

      if sm.geo.location_requested == true

        sm.warn 'sm.geo.location_node : ' + sm.geo.location_node

        if typeof sm.geo.location_node == 'undefined'

          sm.geo.location_node = data.first_node

          if smGeoLabel != null
            smGeoLabel.innerHTML = data.first_node.name
          sm.geo.load_for_node_id data.first_node._id
          sm.geo.loaded = false

      if typeof sm.geo.location_node != 'undefined'
        if smGeoLabel != null
          smGeoLabel.innerHTML = sm.geo.location_node.name

      smGeoNodesDom = sm.utils.ge('smGeoNodesContent')
      if smGeoNodesDom != null
        smGeoNodesDom.innerHTML = data.nodes

      gls = sm.utils.ge_class document, 'js-gnlayer'
      sm.geo.layers_count = gls.length

      cs = sm.states.cur_state()
      if typeof sm.states.requested_geo_id != 'undefined'
        node_dom = sm.utils.ge_class document, 'gn-' + sm.states.requested_geo_id
        sm.states.requested_geo_id = undefined
        first_node = node_dom[0]

        if typeof first_node != 'undefined'
          layer_id = first_node.parentNode.id
          layer_id = layer_id.replace 'geoLayerNodes', ''
          layer_id = layer_id.replace 'Content', ''

          sm.geo.open_layer layer_id

      if typeof sm.geo.geo_position_obj == 'undefined' && sm.geo.location_requested == false
        sm.geo.get_current_position()
        return false

  ############################################
  ## Объект для работы с карточками продьюсера
  ############################################
  focused_ads :
    is_active : false
    load_more_ads_requested : false

    load_more_ads : () ->
      if this.is_fully_loaded == true
        return false
      sm.request.perform this.curl + '&h=' + false + '&a.offset=' + this.ads.length
      this.load_more_ads_requested = true

    scroll_or_move : undefined

    show_ad_by_index : ( ad_index, direction ) ->

      if typeof this.ads == 'undefined'
        return false

      if typeof this.sm_blocks == 'undefined'
        return false

      if vendor_prefix.js == 'Webkit'
        sm.focused_ads.ads_container_dom.style['-webkit-transform'] = 'translate3d(-' + cbca_grid.ww*ad_index + 'px, 0px, 0px)'
      else
        sm.focused_ads.ads_container_dom.style['transform'] = 'translate3d(-' + cbca_grid.ww*ad_index + 'px, 0px, 0px)'

      sm.focused_ads.ads_container_dom.setAttribute 'data-x-offset', -cbca_grid.ww*ad_index

      if ad_index == this.active_ad_index
        return false

      this.active_ad_index = ad_index

      if this.active_ad_index > this.ads.length
        cb = () ->
          sm.focused_ads.load_more_ads()

        setTimeout cb, 400

      if direction == '+'
        ad_c_el = sm.utils.ge('focusedAd' + ( ad_index + 1 ) )
        if ad_c_el != null
          ad_c_el.style.visibility = 'visible';

        if ad_index >= 2
          sm.utils.ge('focusedAd' + ( ad_index - 2 ) ).style.visibility = 'hidden';

      if direction == '-'
        if ad_index >= 1
          sm.utils.ge('focusedAd' + ( ad_index - 1 ) ).style.visibility = 'visible';

        fel = sm.utils.ge('focusedAd' + ( ad_index + 2 ) )
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

      sm.utils.removeClass this.ads_container_dom, '__animated'

    touchmove_event : ( event ) ->
      ex = event.touches[0].pageX
      ey = event.touches[0].pageY

      delta_x = this.tstart_x - ex
      delta_y = this.tstart_y - ey

      if typeof sm.focused_ads.scroll_or_move == 'undefined' && !( delta_x == 0 && delta_y == 0 )
        if Math.abs( delta_y ) > Math.abs( delta_x )
          sm.focused_ads.scroll_or_move = 'scroll'
        else
          sm.focused_ads.scroll_or_move = 'move'

      if sm.focused_ads.scroll_or_move == 'scroll'
        return false
      else
        event.preventDefault()

      c_x_offset = sm.focused_ads.ads_container_dom.getAttribute 'data-x-offset'
      c_x_offset = parseInt c_x_offset

      if vendor_prefix.js == 'Webkit'
        sm.focused_ads.ads_container_dom.style['-webkit-transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'
      else
        sm.focused_ads.ads_container_dom.style['transform'] = 'translate3d(' + parseInt( c_x_offset - delta_x ) + 'px, 0px, 0px)'

      this.x_delta_direction = this.last_x - ex

      this.last_x = ex

    touchend_event : ( event ) ->

      sm.utils.addClass this.ads_container_dom, '__animated'

      delete sm.focused_ads.tstart_x
      delete sm.focused_ads.tstart_y

      if this.x_delta_direction > 0
        cb = () ->
          sm.focused_ads.next_ad()
      else
        cb = () ->
          sm.focused_ads.prev_ad()

      if this.scroll_or_move == 'move'
        setTimeout cb, 1

      delete sm.focused_ads.scroll_or_move

    touchcancel_event : ( event ) ->

      delete sm.focused_ads.tstart_x
      delete sm.focused_ads.tstart_y
      delete sm.focused_ads.scroll_or_move

    render_more : ( more_ads ) ->

      this.load_more_ads_requested = false
      if typeof more_ads == 'undefined'
        return false

      html = ''
      for i, v of more_ads
        html += more_ads[i]
        this.ads.push more_ads[i]

      sm.utils.ge('smads' + this.ads_receiver_index).innerHTML = html
      this.ads_rendered = this.ads.length + 1

      this.check_if_fully_loaded()
      this.render_ads_receiver()

      this.sm_blocks = sm_blocks = sm.utils.ge_class this._container, 'sm-block'
      this.fit()
      sm.styles.init()

    check_if_fully_loaded : () ->

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

      loader_dom = sm.utils.ge('smFocusedAdsLoaderContent')
      if loader_dom != null
        if cbca_grid.ww >= 660
          loader_dom.className = 'sm-block sm-focused-ads-loader'
        else
          loader_dom.className = 'sm-block sm-focused-ads-loader __small'

      for _b in this.sm_blocks

        _block_width = _b.getAttribute 'data-width'

        if cbca_grid.ww >= 660
          sm.utils.addClass _b, 'double-size'
          _block_width = _block_width*2
          padding = 0
        else
          sm.utils.removeClass _b, 'double-size'
          padding = 0
          _block_width = 300

        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.width = cbca_grid.ww + 'px'

        _b.parentNode.parentNode.parentNode.style.width = parseInt( _block_width ) + padding + 'px'

        sm.utils.addClass _b, '__rel'

        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.width = cbca_grid.ww + 'px'
        _b.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 'px'
        _b.parentNode.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 'px'
        _b.parentNode.parentNode.parentNode.parentNode.style.height = cbca_grid.wh + 1 + 'px'

      this.ads_container_dom.style.width = this.sm_blocks.length * cbca_grid.ww + 'px'

    render_ads_receiver : () ->

      if this.is_fully_loaded == true
        return false

      this.ads_receiver_index++
      attrs =
        'id' : 'smads' + this.ads_receiver_index

      _blocks_receiver_dom = sm.utils.ce 'span', attrs
      _blocks_receiver_dom.innerHTML = this.loader_dom
      this.ads_container_dom.appendChild _blocks_receiver_dom


    ## Закрыть
    close : () ->

      if typeof sm.focused_ads.ads_container_dom == 'undefined'
        return false

      this.is_active = false
      animation_cb = () ->
        sm.utils.removeClass sm.focused_ads._container, 'fs-animated-end'
      setTimeout animation_cb, 3

      cb = () ->
        sm.utils.ge('smFocusedAds').style.display = 'none'
        sm.focused_ads.ads_container_dom.innerHTML = ''

      setTimeout cb, 200

      delete sm.focused_ads.active_ad_index
      delete sm.shop_load_locked

    cursor :
      offset : -2
      init : () ->
        if sm.utils.is_touch_device()
          return false
        cursor_dom = sm.utils.ge 'smFocusedAdsArrowLabel'

        sm.utils.add_single_listener sm.focused_ads.ads_container_dom, 'mousemove', ( event ) ->
          sm.focused_ads.cursor.move event.clientX, event.clientY

        sm.utils.add_single_listener sm.focused_ads._container, 'click', ( event ) ->

          direction = sm.utils.ge('smFocusedAdsArrowLabel').getAttribute 'data-direction'

          if direction == "right"
            sm.focused_ads.next_ad()
          if direction == "left"
            sm.focused_ads.prev_ad()

      move : ( x, y ) ->
        cursor_dom = sm.utils.ge 'smFocusedAdsArrowLabel'
        cursor_dom.style.left = x - sm.focused_ads.cursor.offset + 'px'
        cursor_dom.style.top = y - sm.focused_ads.cursor.offset + 'px'

        if x > cbca_grid.ww / 2
          cursor_dom.className = 'sm-focused-ads_arrow-label fad-arrow-label __right-arrow'
          cursor_dom.setAttribute 'data-direction', 'right'
        else
          cursor_dom.className = 'sm-focused-ads_arrow-label fad-arrow-label __left-arrow'
          cursor_dom.setAttribute 'data-direction', 'left'

    ############################
    ## Инициализация focused_ads
    ############################
    init : () ->
      this.is_active = true
      this.ads_container_dom = sm.utils.ge('smFocusedAdsContainer')

      this.ads_receiver_index = 0
      this.is_fully_loaded = false
      this.render_ads_receiver()
      this.loader_dom = '<div id="focusedAdLoader" class="sm-flex sm-overflow-scrolling focused-ad">' + sm.utils.ge('focusedAdLoader').innerHTML + '</div>'
      sm.utils.re('focusedAdLoader')

      ## общее число карточек у продьюсера
      this.ads_count = this.ads_container_dom.getAttribute 'data-ads-count'
      this.ads_rendered = this.ads.length + 1

      html = ''
      for i, v of this.ads
        html += this.ads[i]

      sm.utils.ge('smads' + this.ads_receiver_index).innerHTML = html
      this.check_if_fully_loaded()
      this.render_ads_receiver()

      this._container = sm.utils.ge('smFocusedAds')

      ## Ввести экран с анимацией
      sm.utils.addClass this._container, 'fs-animated-start'

      animation_cb = () ->
        sm.utils.addClass sm.focused_ads._container, 'fs-animated-end'
        sm.utils.addClass sm.focused_ads._container, 'transition-animated'

      setTimeout animation_cb, 20

      ## События
      _e = if sm.utils.is_touch_device() then 'touchend' else 'click'

      ## События для свайпа
      sm.utils.add_single_listener this._container, 'touchstart', ( event ) ->
        sm.focused_ads.touchstart_event event

      sm.utils.add_single_listener this._container, 'touchmove', ( event ) ->
        sm.focused_ads.touchmove_event event

      sm.utils.add_single_listener this._container, 'touchcancel', ( event ) ->
        sm.focused_ads.touchcancel_event event

      sm.utils.add_single_listener this._container, 'touchend', ( event ) ->
        sm.focused_ads.touchend_event event

      ## События для стрелки
      this.cursor.init()

      this.sm_blocks = sm_blocks = sm.utils.ge_class this._container, 'sm-block'
      this.fit()

      sm.styles.init()
      this.active_ad_index = 0

  ##################################################
  ## Показать / скрыть экран с категориями и поиском
  ##################################################
  navigation_layer :

    tabs : ["smCategories", "smShops"]

    adjust : () ->

      if sm.utils.ge('smNavLayerTabs') == null
        offset = 100
      else
        offset = 150

      for k, t of this.tabs
        if sm.utils.ge(t) == null
          return false
        sm.utils.ge(t).style.height = cbca_grid.wh - offset
        sm.utils.ge(t + 'Wrapper').style.height = cbca_grid.wh - offset
        sm.utils.ge(t + 'Content').style.height = cbca_grid.wh - ( offset - 1 )

    open : () ->
      cs = sm.utils.ge('smCategoriesScreen')

      if cs.style.display == "block"
        return false

      this.adjust()

      ## Скрыть кнопки хидера главного экрана
      sm.utils.addClass sm.utils.ge('smRootProducerHeader'), '__w-index-icon'

      cs.style.display = 'block'
      sm.rebuild_grid()

    reset_tabs : () ->
      this.show_tab this.tabs[0]

    show_tab : ( tab ) ->
      for k, t of this.tabs
        tab_content_dom = sm.utils.ge(t)
        tab_dom = sm.utils.ge(t + 'Tab')

        if tab_content_dom == null
          return false

        if tab == t
          tab_content_dom.style.display = 'block'
          sm.utils.removeClass tab_dom, '__inactive'
        else
          tab_content_dom.style.display = 'none'
          sm.utils.addClass tab_dom, '__inactive'

    close : ( all_except_search ) ->

      cs = sm.utils.ge('smCategoriesScreen')

      if cs == null || cs.style.display == "" || cs.style.display == "none"
        return false

      cbca_grid.right_offset = 0
      cbca_grid.rebuild()

      sm.utils.removeClass sm.utils.ge('smRootProducerHeader'), '__w-index-icon'

      if all_except_search == true
        sm.utils.addClass sm.utils.ge('smCategoriesScreen'), '__search-mode'
      else
        sm.utils.removeClass sm.utils.ge('smCategoriesScreen'), '__search-mode'
        this.reset_tabs()

        sm_cat_screen_dom = sm.utils.ge('smCategoriesScreen')
        if sm_cat_screen_dom != null
          sm_cat_screen_dom.style.display = 'none'
        sm.utils.ge('smRootProducerHeaderButtons').style.display = 'block'

  #########################################
  ## Показать / скрыть экран со списком магазинов
  #########################################
  open_shopList_screen : ( event ) ->
    sm.utils.ge('smShopListScreen').style.display = 'block'
    event.preventDefault()
    return false

  close_shopList_screen : ( event ) ->
    sm.utils.ge('smShopListScreen').style.display = 'none'
    event.preventDefault()
    return false

  ######################################################
  ## Открыть экран с предупреждением о выходе из маркета
  ######################################################
  open_close_screen : ( event ) ->
    sm.utils.ge('smCloseScreen').style.display = 'block'
    event.preventDefault()
    return false

  exit_close_screen : ( event ) ->
    sm.utils.ge('smCloseScreen').style.display = 'none'
    event.preventDefault()
    return false

  ###############################
  ## Скрыть / показать sio.market
  ###############################
  close_mart : ( event ) ->
    #sm.utils.ge('sioMartRoot').style.display = 'none'
    #sm.utils.ge('smCloseScreen').style.display = 'none'

    #sm.utils.ge_tag('body')[0].style.overflow = 'auto'

    event.preventDefault()
    return false

  open_mart : ( event ) ->
    if this.is_market_loaded != true
      sm.load_mart()
    event.preventDefault()
    return false

  index_navigation :
    hide : () ->
      _dom = sm.utils.ge 'smIndexNavigation'
      sm.utils.addClass _dom, 'hidden'
    show : () ->
      _dom = sm.utils.ge 'smIndexNavigation'
      sm.utils.removeClass _dom, 'hidden'

  load_for_shop_id : ( shop_id, ad_id ) ->

    if sm.utils.is_touch_device() && sm.events.is_touch_locked
      return false

    if typeof sm.shop_load_locked != 'undefined'
      return false

    siomart.utils.ge('fsLoaded').style.display = 'block'
    sm.shop_load_locked = true

    cs = sm.states.cur_state()

    sm.states.transform_state
      cat_id : cs.cat_id
      cat_class : cs.cat_class
      fads :
        is_opened : true
        producer_id : shop_id
        ad_id : ad_id

  do_load_for_shop_id : ( shop_id, ad_id ) ->

    cs = sm.states.cur_state()
    a_rcvr = '&a.rcvr=' + cs.mart_id

    url = '/market/fads?a.shopId=' + shop_id + '&a.gen=' + Math.floor((Math.random() * 100000000000) + 1) + '&a.size=' + sm.config.producer_ads_per_load + a_rcvr + '&a.firstAdId=' + ad_id + '&' + sm.geo.request_query_param()

    sm.focused_ads.curl = url

    sm.focused_ads.requested_ad_id = ad_id
    sm.request.perform url

  ## Загрузить все офферы для магазина
  load_for_cat_id : ( cat_id, cat_class ) ->

    sm.grid_ads.is_load_more_requested = false
    sm.grid_ads.is_fully_loaded = false

    document.getElementById('smGridAdsWrapper').scrollTop = '0'

    cs = sm.states.cur_state()

    sm.utils.ge('smRootProducerHeader').className = 'sm-producer-header abs __w-global-cat ' + '__' + cat_class

    a_rcvr = if sm.config.mart_id == '' then '' else '&a.rcvr=' + cs.mart_id
    url = '/market/ads?a.catId=' + cat_id + a_rcvr  + '&' + sm.geo.request_query_param()
    sm.request.perform url

  ########################################
  ## картинка приветствия торгового центра
  ########################################
  welcome_ad :
    hide_timeout : 1700
    fadeout_transition_time : 700

    fit : ( image_dom, is_divided ) ->

      is_divided = is_divided || false

      if image_dom == null
        return false
      image_w = parseInt image_dom.getAttribute "data-width"
      image_h = parseInt image_dom.getAttribute "data-height"

      if is_divided == true
        nw = image_w/2
        nh = image_h/2
      else
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

      _dom = sm.utils.ge 'smWelcomeAd'

      sm.utils.addClass _dom, '__animated'
      sm.utils.addClass _dom, '__fade-out'

      dn_cb = () ->
        _dom.style.display = 'none'
      setTimeout dn_cb, sm.welcome_ad.fadeout_transition_time

    init : () ->

      cs = sm.states.cur_state()

      this._dom = sm.utils.ge 'smWelcomeAd'

      if typeof cs.with_welcome_ad != 'undefined' && cs.with_welcome_ad == false

        ## Все скрыть и вернуть false
        this._dom.style.display = 'none'
        return false

      this._bg_img_dom = _bg_img_dom = siomart.utils.ge 'smWelcomeAdBgImage'
      this._fg_img_dom = _fg_img_dom = siomart.utils.ge 'smWelcomeAdfgImage'

      this.fit _bg_img_dom
      this.fit _fg_img_dom, true

      setTimeout sm.welcome_ad.hide, this.hide_timeout

  ##################################################
  ## Забиндить события на навигационные кнопари
  ## Вызывается только при инициализации marketIndex
  ##################################################
  init_navigation : () ->

    sm.utils.add_single_listener window, 'touchmove', sm.events.document_touchmove
    sm.utils.add_single_listener window, 'touchend', sm.events.document_touchend
    sm.utils.add_single_listener window, 'touchcancel', sm.events.document_touchcancel

    _event = if sm.utils.is_touch_device() then 'touchend' else 'click'

    sm.utils.add_single_listener document, _event, sm.events.document_click
    sm.utils.add_single_listener document, 'keyup', sm.events.document_keyup

    ## Поиск
    _search_dom = sm.utils.ge('smSearchField')
    sm.utils.add_single_listener _search_dom, 'keyup', ( e ) ->
      sm.search.queue_request this.value

    sm.utils.add_single_listener _search_dom, 'focus', ( e ) ->
      sm.search.onfocus()

    sm.utils.add_single_listener _search_dom, 'blur', ( e ) ->
      sm.search.onblur()

  set_window_class : () ->
    _window_class = ''

    if cbca_grid.ww <= 980
      _window_class = 'sm-w-980'

    if cbca_grid.ww <= 800
      _window_class = 'sm-w-800'

    if cbca_grid.ww <= 660
      _window_class = 'sm-w-400'

    sm.utils.ge('sioMartLayout').className = _window_class

  ###################
  ## Состояния выдачи
  ###################
  states :
    list : []
    requested_state : undefined
    cur_state_index : -1
    prev_state : undefined
    ds :
      url : '/'
      mart_id : undefined
      with_welcome_ad : true
      cat_id : undefined
      cat_class : undefined
      cat_screen :
        is_opened : false
      geo_screen :
        is_opened : false
      fads :
        is_opened : false
      search_request : undefined

    cur_state : () ->
      if this.cur_state_index == -1
        sm.warn 'no state with index -1'
        return undefined
      this.list[this.cur_state_index]

    add_state : ( ns ) ->

      if typeof ns.url == 'undefined' then ns.url = this.ds.url
      if typeof ns.mart_id == 'undefined' then ns.mart_id = this.ds.mart_id
      if typeof ns.with_welcome_ad == 'undefined' then ns.with_welcome_ad = this.ds.with_welcome_ad
      if typeof ns.cat_id == 'undefined' then ns.cat_id = this.ds.cat_id
      if typeof ns.cat_class == 'undefined' then ns.cat_class = this.ds.cat_class
      if typeof ns.cat_screen == 'undefined' then ns.cat_screen = this.ds.cat_screen
      if typeof ns.geo_screen == 'undefined' then ns.geo_screen = this.ds.geo_screen
      if typeof ns.fads == 'undefined' then ns.fads = this.ds.fads
      if typeof ns.search_request == 'undefined' then ns.search_request = this.ds.search_request

      this.push ns

    update_state : ( sup ) -> #state_update_params
      cs = sm.states.cur_state()

      if typeof sup.mart_id != 'undefined' then cs.mart_id = sup.mart_id
      this.list[this.list.length-1] = cs

    transform_state : ( stp ) -> #state_transform_params
      cs = sm.states.cur_state()
      ns = {}

      ns.search_request = if typeof stp.search_request != 'undefined' then stp.search_request else undefined
      ns.geo_screen = if typeof stp.geo_screen != 'undefined' then stp.geo_screen else cs.geo_screen
      ns.cat_screen = if typeof stp.cat_screen != 'undefined' then stp.cat_screen else cs.cat_screen
      ns.fads = if typeof stp.fads != 'undefined' then stp.fads else cs.fads

      ns.cat_id = stp.cat_id
      ns.cat_class = stp.cat_class

      ns.mart_id = cs.mart_id

      this.push ns

    push : ( state ) ->
      this.process_state state

      this.list = this.list.slice 0, this.cur_state_index+1
      this.list.push state

      ## state index
      this.cur_state_index = this.list.length - 1
      sm.history.push {state_index : this.cur_state_index}, 'Suggest.io', '/p' + this.cur_state_index

    goto : ( state_index ) ->
      if state_index == -1
        sio.warn 'no state with index -1'
        return false

      state = this.list[state_index]
      this.process_state state
      this.cur_state_index = state_index

    process_state : ( state ) ->

      cs = this.cur_state()

      ## 1. проверить, соответствует ли текущий mart_id в состояниях
      if typeof cs == 'undefined' || cs.mart_id == undefined || cs.mart_id != state.mart_id
        sm.log 'process_state : switch nodes'
        sm.load_mart state
        this.requested_state = state
      else
        this.process_state_2 state

    process_state_2 : ( state ) ->

      cs = this.cur_state()

      sm.warn 'process_state_2 invoked'
      sm.warn state
      this.requested_state = undefined

      ## 1. Экран с гео добром
      if state.geo_screen.is_opened == true
        sm.geo.open_screen()

      if state.geo_screen.is_opened == false
        sm.geo.close_screen()

      ## 2. Карточки по категориям
      if typeof state.cat_id != 'undefined' && sm.global_cat_id != state.cat_id
        sm.load_for_cat_id state.cat_id, state.cat_class

      sm.global_cat_id = state.cat_id

      if typeof state.cat_id == 'undefined' && typeof cs.cat_id != 'undefined'
        sm.utils.removeClass sm.utils.ge('smRootProducerHeader'), '__w-global-cat'
        sm.utils.removeClass sm.utils.ge('smRootProducerHeader'), '__w-index-icon'
        sm.grid_ads.load_index_ads()

      ## 3. Экран с категориями
      if state.cat_screen.is_opened == true
        sm.navigation_layer.open()

      if state.cat_screen.is_opened == false
        sm.navigation_layer.close()

      ## 4. Focused ads
      if typeof state.fads != 'undefined' && state.fads.is_opened == true
        sm.do_load_for_shop_id state.fads.producer_id, state.fads.ad_id
      else
        sm.focused_ads.close()

      if cbca_grid.ww <= 400
        if state.geo_screen.is_opened == true || state.cat_screen.is_opened == true || ( typeof state.fads != 'undefined' && state.fads.is_opened == true )
          sm.utils.addClass sm.utils.ge('smGridAds'), '__blurred'
        else
          sm.utils.removeClass sm.utils.ge('smGridAds'), '__blurred'

      ## 5. Search
      if typeof state.search_request != 'undefined'
        a_rcvr = '&a.rcvr=' + state.mart_id
        url = '/market/ads?a.q=' + state.search_request + a_rcvr + '&' + sm.geo.request_query_param()
        sm.request.perform url

  ############################
  ## Функции для инициализации
  ############################

  ######################################
  ## Загрузить индексную страницу для ТЦ
  ######################################
  load_mart : ( state ) ->
    ## ? здесь ли это должно быть?
    this.define_per_load_values()

    ww_param = if cbca_grid.ww then 'a.screen=' + cbca_grid.ww + 'x' + cbca_grid.wh else ''
    index_action = if typeof state.mart_id != 'undefined' then '/market/index/' + state.mart_id  + '?' + ww_param else '/market/geo/index' + '?' + ww_param

    sm.log 'about to call index_action : ' + index_action
    this.request.perform index_action

  define_per_load_values : () ->

    cbca_grid.set_window_size()

    if cbca_grid.ww <= 980
      sm.config.ads_per_load = 20

    if cbca_grid.ww <= 800
      sm.config.ads_per_load = 10

    if cbca_grid.ww <= 660
      sm.config.ads_per_load = 5

  ###########################
  ## Инициализация Sio.Market
  ###########################
  init : () ->

    sm.config.host = window.siomart_host
    this.utils.set_vendor_prefix()
    this.history.init()

    sm_id = window.siomart_id || undefined

    sm.warn 'initial mart_id : ' + sm_id

    ## Если еще не запрашивали координаты у юзера
    if sm.geo.location_requested == false
      sm.geo.get_current_position()


window.sm = window.siomart = sm
sm.init()