bookletScrollr = ''
initMaxScroll = 0
afterHideMaxScroll = 0

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

  $slideElement.slideToggle(
    600
    () ->
      if $slideElement.is ':visible'
        bookletScrollr.setMaxScrollTop initMaxScroll
      else
        bookletScrollr.setMaxScrollTop afterHideMaxScroll
  )

  console.log bookletScrollr.getMaxScrollTop()

$ document
.ready () ->

  $window = $ window
  winWidth = $window.width()

  if winWidth <= 1024

    skrollr.init(
      smoothScrolling: false,
      mobileDeceleration: 0.004
    )

    bookletScrollr = skrollr.get()
    initMaxScroll = bookletScrollr.getMaxScrollTop()
    console.log bookletScrollr.getMaxScrollTop()


    $ '#equipment_slide-cnt'
    .hide()

    afterHideMaxScroll = bookletScrollr.refresh().getMaxScrollTop()
    console.log bookletScrollr.getMaxScrollTop()

