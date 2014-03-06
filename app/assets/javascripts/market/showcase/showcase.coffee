showcase =

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

  fit_image : () ->

    photo = document.getElementById 'poster-photo'

    image_w = parseInt photo.getAttribute "data-width"
    image_h = parseInt photo.getAttribute "data-height"

    if image_w / image_h < this.ww / this.wh
      nw = this.ww
      nh = nw * image_h / image_w
    else
      nh = this.wh
      nw = nh * image_w / image_h

    photo.style.width = nw + 'px'
    photo.style.height = nh + 'px'
    photo.style.marginLeft = - nw / 2 + 'px'
    photo.style.marginTop = - nh / 2 + 'px'

  init : () ->
    this.set_window_size()
    this.fit_image()

    $(window).bind "resize", () ->
      sio_showcase.set_window_size()
      sio_showcase.fit_image()

window.sio_showcase = showcase
showcase.init()