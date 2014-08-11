bookletScrollr = ''
initMaxScroll = 0
afterHideMaxScroll = 0

$ document
.scroll () ->

  $document = $ document
  $header = $ '#header'
  scrollTop = $document.scrollTop()

  if scrollTop > 50
    $header.addClass '__js-dark'
  else
    $header.removeClass '__js-dark'



$ document
.on 'click', '.js-slide-btn', (e) ->
  $this = $ this
  slideSelector = $this.attr 'data-slide'
  $slideElement = $ slideSelector

  $slideElement.slideToggle(
    600
    () ->
      if bookletScrollr
        bookletScrollr.refresh()
  )



$ document
.on 'click', '.js-slide-to', (e) ->
  e.preventDefault()
  $this = $ this
  targetSelector = $this.attr 'href'
  $target = $ targetSelector
  targetScrollTop = $target.offset().top

  if bookletScrollr
    currentScrollTop = bookletScrollr.getScrollTop()
    targetScrollTop = targetScrollTop + currentScrollTop
    bookletScrollr.animateTo targetScrollTop
  else
    $ 'body, html'
    .animate(
      scrollTop: targetScrollTop,
      800
    )


initScrollr = () ->

  $window = $ window
  winWidth = $window.width()

  if winWidth <= 1024

    if bookletScrollr

      bookletScrollr.refresh()

    else
      skrollr.init(
        smoothScrolling: false,
        mobileDeceleration: 0.004,
        render: (data) ->
          $header = $ '#header'
          if data.curTop > 50
            $header.addClass '__js-dark'
          else
            $header.removeClass '__js-dark'
      )

      bookletScrollr = skrollr.get()


$ document
.ready () ->

  initScrollr()

$ window
.resize () ->
  initScrollr()
