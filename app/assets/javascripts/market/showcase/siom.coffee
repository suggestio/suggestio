########################################
## иконка siom для подмешивания на сайты
########################################
siom =
  config :
    sm_trigger_class : 'sio-mart-trigger'
  utils :

    elts_cache : {}

    ce : ( tag, attributes, inhtml ) ->
      ne = document.createElement tag
      for k,v of attributes
        ne.setAttribute k, v
      if( typeof( inhtml ) != 'undefined' )
        ne.innerHTML = inhtml
      ne

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

    ge_tag : ( tag, force_no_cache ) ->
      force_no_cache = force_no_cache || false
      if force_no_cache != true && typeof this.elts_cache[tag] != 'undefined'
        this.elts_cache[tag]
      else
        _elt = document.getElementsByTagName tag
        this.elts_cache[tag] = _elt
        _elt

    is_touch_device : () ->
      if document.ontouchstart != null
        false
      else
        if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          false
        else
          true
    add_single_listener : (elt, eventType, listener) ->
      if elt == null
        return false
      if elt.addEventListener
        elt.addEventListener eventType, listener, false
      else
        if elt.attachEvent
          elt.attachEvent 'on' + eventType, () ->
            listener.apply elt

  perform_request : ( url ) ->
    js_request_attrs =
      type : 'text/javascript'
      src : siom.config.host + url
    js_request = siom.utils.ce 'script', js_request_attrs
    siomart.utils.ge_tag('head')[0].appendChild js_request

  receive_response : ( data ) ->
    siom.config.node_color = data.color
    siom.config.logo_src = data.logo_src

    siom.draw_trigger()

  draw_trigger : () ->
    sm_trigger_attrs =
      class : this.config.sm_trigger_class
      id : 'sioMartTrigger'
      style : '-webkit-border-radius: 50px; -webkit-transition: all 0.5s linear; opacity: 0; cursor: pointer; position: fixed; left: 10px; top: 10px; color: #fff; border: 1px #333 solid; text-align: center; padding: 0px; width: 50px; height: 50px; z-index: 99998; background-color: #' + siom.config.node_color

    sm_trigger = this.utils.ce 'div', sm_trigger_attrs, '<span style="display: table-cell; height: 50px; vertical-align: middle"><img style="max-width: 50px; max-height: 50px;" src=\'' + siom.config.host + siom.config.logo_src + '\'/>' + '</span>'

    _body = this.utils.ge_tag('body')[0]
    _body.appendChild sm_trigger

    _event = if siomart.utils.is_touch_device() then 'touchend' else 'click'

    show_cb = () ->
      siom.utils.ge('sioMartTrigger').style.opacity = 1
    setTimeout show_cb, 100

    this.utils.add_single_listener sm_trigger, _event, () ->
      showcase_url = siomart.config.host + '/market/site/' + siomart.config.mart_id

      newwindow = window.open showcase_url, 'Sio.Market'
      newwindow.focus()

  init : () ->
    siom.config.mart_id = window.siomart_id
    siom.config.host = window.siomart_host

    this.perform_request '/market/node_data/' + window.siomart_id

window.siom=siom
window.siomart=siom
siom.init()