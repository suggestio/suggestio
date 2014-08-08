$ document
.scroll () ->

  $document = $ document
  $header = $ '#header'
  scrollTop = $document.scrollTop()

  if scrollTop > 500
    $header.addClass '__js-dark'
  else
    $header.removeClass '__js-dark'



$ document
.on 'click', '.js-slide-btn', (e) ->
  $this = $ this
  slideSelector = $this.attr 'data-slide'
  $slideElement = $ slideSelector

  $slideElement.slideToggle()


$ document
.ready () ->

  $window = $ window
  winWidth = $window.width()

  if winWidth <= 1024

    skrollr.init(
      smoothScrolling: false,
      forceHeight: true,
      mobileDeceleration: 0.004
    )