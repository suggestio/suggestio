$doc = $ document
$win = $ window


isTouchDevice = () ->
  if document.ontouchstart != null
    false
  else
    if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
      false
    else
      true

event = if isTouchDevice() then 'touchend' else 'click'

$doc.scroll () ->

  $document = $ document
  $header = $ '#headerSlide'
  scrollTop = $document.scrollTop()

  if scrollTop > 600
    $header.addClass '__visible'
  else
    $header.removeClass '__visible'


slideToElement = (selector)->
  HEADER_HEIGHT = 60
  $target = $ selector
  scrollTop = $target.offset().top

  if $win.width() > 768 && scrollTop > HEADER_HEIGHT
    scrollTop = scrollTop - HEADER_HEIGHT

  $ 'body, html'
  .animate(
    scrollTop: scrollTop,
    800
  )


$doc.on event, '.js-tab', (e) ->
  $this = $ this
  selector = $this.attr 'data-cnt'
  $cnt = $ selector

  if $this.hasClass '__js-act'
    return false

  $ '.js-tab.__js-act'
  .removeClass '__js-act'
  $this.addClass '__js-act'

  $ '.js-tab-cnt:visible'
  .hide()
  $cnt.show()


$doc.on event, '.js-slide-btn', (e) ->
  $this = $ this
  selector = $this.attr 'data-slide'
  $slideElement = $ selector

  $slideElement.slideDown()
  slideToElement selector

$doc.on event, '.js-slide-tab', (e) ->
  $this = $ this
  slideSelector = $this.attr 'data-slide'
  cntSelector = $this.attr 'data-cnt'
  $slideElement = $ slideSelector

  if !$slideElement.is ':visible'
    $slideElement.slideDown()

  $cnt = $ cntSelector

  if !$cnt.is ':visible'
    $slideElement
    .find '.__js-act'
    .removeClass '__js-act'
    $slideElement
    .find ".js-tab[data-cnt = #{cntSelector}]"
    .addClass '__js-act'
    $slideElement
    .find '.js-tab-cnt'
    .hide()
    $cnt.show()

  slideToElement slideSelector


$doc.on event, '.js-slide-to', (e) ->
  e.preventDefault()
  $this = $ this
  targetSelector = $this.attr 'href'
  slideToElement targetSelector


$doc.ready ()->

  sliderControls = true
  windWidth = $win.width()

  if windWidth < 768
    $ 'br'
    .remove()
    sliderControls = false

  if windWidth < 768

    $ '.js-ios-slider'
    .bxSlider(
      auto: false,
      pager: false,
      controls: false,
      infiniteLoop: false,
      hideControlOnEnd: false
    )

    $ '.js-android-slider'
    .bxSlider(
      auto: false,
      pager: false,
      controls: false,
      infiniteLoop: false,
      hideControlOnEnd: false,
      onSliderLoad: ()->
        $ '#phones, #android'
        .hide()
    )
  else
    $ '#phones, #android'
    .hide()

  $ '#slider'
  .bxSlider(
    auto: false,
    pager: true,
    controls: sliderControls,
    infiniteLoop: false,
    hideControlOnEnd: true,
    adaptiveHeight: true
  )