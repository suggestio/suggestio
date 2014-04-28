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
      ww = document.body.clientWidth
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
      no_of_cells = 2

    if no_of_cells == 5
      no_of_cells = 4

    if no_of_cells == 7
      no_of_cells = 6

    cw = no_of_cells * ( this.cell_size + this.cell_padding) - this.cell_padding

    this.max_allowed_cell_width = no_of_cells

    this.layout_dom.style.width = cw + 'px'

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
        tmp_block.splice(i,1)
        params = {'opacity':'0'}
        params[vendor_prefix.css + 'transform'] = 'translate3d(-1000px, 0px,0)'
        $('#elt' + b.id ).css params
        return this.fetch_block(block_max_w, tmp_block, i+1 )
      else
        tmp_block.splice(i,1)
        this.blocks = tmp_block

        return b
    else
      if w_cell_width > this.max_allowed_cell_width || w_cell_width_opened > this.max_allowed_cell_width
        params = {'opacity':'0'}
        params[vendor_prefix.css + 'transform'] = 'translate3d(-1000px, 0px,0)'
        $('#elt' + b.id ).css params
        tmp_block.splice(i,1)

      return this.fetch_block(block_max_w, tmp_block, i+1 )

  fetch_spacer : (block_max_w, tmp_block, i) ->

    if this.spacers.length == 0
      return null

    if typeof( tmp_spacer ) == "undefined"
      tmp_spacer = this.spacers
      i = 0

    b = tmp_spacer[i]
    b.block.show()
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

    params =
      'opacity' : '1'

    $('#elt' + block_id).addClass "animated-block"
    $('#elt' + block_id).css params

  deactivate_block : ( block_id, target_opacity ) ->

    block = $('#' + block_id)

    block.css({'opacity':target_opacity})

    if block.hasClass "active-block"
      block.removeClass "active-block"
      block_js_class = block.attr "data-js-class"

      bs = jQuery('.block-source', block)
      cb2 = () ->
        bs.css({'visibility':'hidden'})
        block.removeClass "no-bg"
        block.removeClass "hover"

      cbca['block_desource' + block_id]= setTimeout cb2, 300

      if typeof block_js_class != 'undefined'
        close_action = window[block_js_class].close_action

        if typeof close_action != 'undefined'
          close_action()

  is_only_spacers : () ->

    for b in this.blocks
      if b.class != 'block under-construction'
        return false

    return true

  max_used_height : ( columns_used_space ) ->
    max_h = 0

    for c, v of columns_used_space
      if v.used_height > max_h
        max_h = v.used_height

    return max_h

  slow_down_blocks : ( is_on ) ->

    _slow_class = "slow-motion-block"

    if is_on == 1
      $('.index-block').addClass _slow_class
    else
      $('.index-block').removeClass _slow_class

  ##############################
  ## Find all blocks on the page
  ##############################
  load_blocks : () ->

    i = 0
    ## TODO : make selector configurable
    $('.index-block').each () ->

      $(this).attr "id", "elt" + i

      height = parseInt $(this).attr "data-height"
      width = parseInt $(this).attr "data-width"

      opened_height = parseInt $(this).attr "data-opened-height"
      opened_width = parseInt $(this).attr "data-opened-width"

      _class = $(this).attr "class"
      _search_string = $(this).attr "data-search-string"
      _is_moveable = $(this).attr "data-is-moveable" || "false"

      block =
        'id' : i
        'width' : width
        'height' : height
        'opened_width' : opened_width
        'opened_height' : opened_height
        'class' : _class
        'block' : $(this)
        '_is_moveable' : _is_moveable

      i++
      cbca_grid.blocks.push block
      cbca_grid.m_blocks = cbca_grid.blocks.slice(0)

    ## Загрузить спейсеры
    $('.index-spacer').each () ->
      $(this).attr "id", "elt" + i

      height = parseInt $(this).attr "data-height"
      width = parseInt $(this).attr "data-width"

      opened_height = parseInt $(this).attr "data-opened-height"
      opened_width = parseInt $(this).attr "data-opened-width"

      _class = $(this).attr "class"
      _search_string = $(this).attr "data-search-string"
      _is_moveable = $(this).attr "data-is-moveable" || "false"

      block =
        'id' : i
        'width' : width
        'height' : height
        'opened_width' : opened_width
        'opened_height' : opened_height
        'class' : _class
        'block' : $(this)
        '_is_moveable' : _is_moveable

      i++
      cbca_grid.spacers.push block
      cbca_grid.m_spacers = cbca_grid.spacers.slice(0)

  init : () ->

    this.blocks_container = document.getElementById 'sioMartIndexGrid'
    this.layout_dom = document.getElementById 'sioMartIndexGridLayout'

    this.set_container_size()
    #this.load_blocks()
    #this.build()

  resize : () ->
    this.set_container_size()
    cbca_grid.blocks = cbca_grid.m_blocks
    cbca_grid.m_blocks = cbca_grid.blocks.slice(0)
    this.build()

    $('body').width "100%"

  set_active_block : ( active_block ) ->

    if active_block

      if typeof cbca.active_block != 'undefined'

        elt = $('#' + cbca.active_block)

        elt_id = parseInt cbca.active_block.replace('elt','')

        original_width = elt.data "original-width"
        original_height = elt.data "original-height"

        cbca.blocks[elt_id].width = original_width
        cbca.blocks[elt_id].height =  original_height

        if active_block != cbca.active_block
          elt.css({'width' : original_width + 'px','height' : original_height + 'px','opacity':'1'})

      if active_block == "resize"
        $('.index-block').css({'opacity' : '1'})
        cbca.deactivate_block cbca.active_block, 1
        delete cbca.active_block
        #cbca.push_history_state "/"

      else

        if cbca.active_block == active_block
          return false

        else
          for i in cbca.blocks
            if 'elt' + i.id == active_block

              setTimeout "cbca.scroll_to('" + active_block + "')", 600

              cbca.active_block = active_block

              bn = $('#elt' + i.id).data "block-name"
              if typeof bn != 'undefined'
                cbca.push_history_state "/b/" + bn
              else
                cbca.push_history_state "/"


              $('#elt' + i.id).attr "data-original-width", cbca.blocks[_i].width
              $('#elt' + i.id).attr "data-original-height", cbca.blocks[_i].height

              opened_width = parseInt $('#elt' + i.id).attr "data-opened-width"
              opened_height = parseInt $('#elt' + i.id).attr "data-opened-height"

              cbca.blocks[_i].width = opened_width
              cbca.blocks[_i].height = opened_height

              bl = $('#elt' + i.id)

              bl.addClass "active-block"
              bl.css({'width' : opened_width + 'px','height' : opened_height + 'px','opacity':'1'})

            else
              cbca.deactivate_block 'elt' + i.id, 0.9

  attach_actions : () ->
    if ( browser.iphone || browser.ipad )
      ## Если у нас девайс на базе iOS — использует touch события

      $('.index-block').each () ->

        b = $(this)

        b.bind "touchstart", ( e ) ->
          x = e.originalEvent.touches[0].pageX
          y = e.originalEvent.touches[0].pageY
          cbca.touch_pos.start = {'x' : x, 'y' : y}

        b.bind "touchmove", ( e ) ->
          cbca.touch_moves++
          x = e.originalEvent.touches[0].pageX
          y = e.originalEvent.touches[0].pageY
          cbca.touch_pos.end = {'x' : x, 'y' : y}

        b.bind "touchend", ( e ) ->

          b = $(this)
          id = b.attr "id"

          if cbca.is_locked == true
            return false

          unlock_cb = () ->
            cbca.is_locked = false

          cbca.invoke_block_class_action b, "hover_reset"

          is_moveable = b.attr "data-is-moveable"

          if cbca.touch_moves > 1

            cbca.touch_moves = 0

            if Math.abs( cbca.touch_pos.end.y - cbca.touch_pos.start.y ) > 5
              if typeof is_moveable != 'undefined' && is_moveable == "true"
                cbca.notification.mark_message_as_shown "moveable"
                cbca.notification.show "second-tap"

              return false

          if typeof is_moveable != 'undefined' && is_moveable == "true"
            cbca.notification.show "moveable"
          else
            cbca.notification.show "second-tap"

          id = b.attr "id"

          if !b.hasClass "hover"
            setTimeout unlock_cb, 100

            if typeof cbca.hovered != 'undefined'
              cbca.block_unhover_action cbca.hovered

            cbca.hovered = b
            cbca.block_hover_action b
          else
            setTimeout unlock_cb, 300
            if !b.hasClass "active-block"
              cbca.block_click_action b
            #else
            #  b.removeClass "hover"
            #  cbca.init "resize"

    else
      ## В противном случае — обычные
      $('.index-block').bind "mouseover", () ->
        cbca.block_hover_action $(this)

      $('.index-block').bind "mouseout", () ->
        cbca.block_unhover_action $(this)

      $('.index-block').bind "click", () ->
        if !$(this).hasClass "active-block"
          cbca.block_click_action $(this)

  build : ( active_block ) ->

    $('.blocks-container').show()

    this.set_active_block active_block

    blocks_length = cbca_grid.blocks.length

    # setting up left and top
    left_pointer = left_pointer_base = 0
    top_pointer = 100

    # Определяем ширину окна
    window_width = $(window).width()

    # Определеяем сколько колонок влезет в экран колонок
    columns = Math.floor( ( window_width - this.cell_padding ) / ( this.cell_size + this.cell_padding) )

    if columns < 2
      columns = 2

    if columns > 8
      columns = 8

    if columns == 3
      columns = 2

    if columns == 5
      columns = 4

    if columns == 7
      columns = 6

    # Ставим указатели строки и колонки
    cline = 0
    pline = 0
    cur_column = 0

    # Генерим объект с инфой об использованном месте
    columns_used_space = {}
    for c in [0..columns-1]
      columns_used_space[c] =
        used_height : 0

    ## Генерим поле
    for i in [0..1000]
      pline = cline

      if cur_column >= Math.floor columns
        cur_column = 0
        cline++
        left_pointer = left_pointer_base

      top = cline * ( this.cell_size + this.cell_padding ) + this.top_offset
      left = left_pointer

      if cline > pline && this.is_only_spacers() == true && cline == this.max_used_height columns_used_space
        break

      if columns_used_space[cur_column].used_height == cline

        # есть место хотя бы для одного блока с минимальной шириной
        # высяним блок с какой шириной может влезть
        block_max_w = this.get_max_block_width columns_used_space, cline, cur_column, columns

        b = this.fetch_block block_max_w

        if b == null
          if this.blocks.length > 0
            console.log 'null, got blocks ' + i
            b = this.fetch_spacer block_max_w
          else
            console.log 'null, no more blocks ' + i
            break

        w_cell_width = Math.floor ( b.width + this.cell_padding ) / ( this.cell_size + this.cell_padding )
        w_cell_height = Math.floor ( b.height + this.cell_padding ) / ( this.cell_size + this.cell_padding )

        for ci in [cur_column..cur_column+w_cell_width-1]
          if typeof( columns_used_space[cur_column] ) != 'undefined'
            columns_used_space[cur_column].used_height += w_cell_height
            cur_column++

        id = b.id

        params = {}
        params[vendor_prefix.css + 'transform'] = 'translate3d(' + left + 'px, ' + top + 'px,0)'

        #if !cbca.if_3d_transform_supported()
        #  params['left'] = left + 'px'
        #  params['top'] = top + 'px'

        $('#elt' + id).css params

        if !active_block
          setTimeout "cbca_grid.init_single_block('" + id + "')", 5*i-i*3

        left_pointer += b.width + this.cell_padding
        pline = cline

      else
        cur_column++
        left_pointer += this.cell_size + this.cell_padding

    ## TODO:
    ## тут у нас есть проблема — в некоторых случаях остаются неиспользованные блоки,
    ## надо их как-то скрывать, или что-то другое придумать
    ## console.log cbca.blocks

    for b in this.blocks
      bid = b.id
      params = {'opacity':'0'}
      $('#elt' + bid ).css params

    ## Вычислим максимальную высоту внутри колонки
    max_h = this.max_used_height columns_used_space

    real_h = ( this.cell_size + this.cell_padding) * max_h + this.bottom_offset

    css_params =
      'height' : parseInt( real_h + this.top_offset ) + 'px'

    this.blocks_container.css css_params

    ## Навешиваем события для блоков
    if active_block
      return false

    #this.attach_actions()

window.cbca_grid = cbca_grid