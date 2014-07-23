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
    top_pointer = 60

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