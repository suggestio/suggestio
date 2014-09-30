$ document
.scroll () ->

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
  targetScrollTop = $target.offset().top

  if targetScrollTop > HEADER_HEIGHT
    scrollTop = targetScrollTop - HEADER_HEIGHT

  $ 'body, html'
  .animate(
    scrollTop: scrollTop,
    800
  )


$ document
.on 'click', '.js-tab', (e) ->
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


$ document
.on 'click', '.js-slide-btn', (e) ->
  $this = $ this
  selector = $this.attr 'data-slide'
  $slideElement = $ selector

  $slideElement.slideDown()
  slideToElement selector

$ document
.on 'click', '.js-slide-tab', (e) ->
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


$ document
.on 'click', '.js-slide-to', (e) ->
  e.preventDefault()
  $this = $ this
  targetSelector = $this.attr 'href'
  slideToElement targetSelector

$doc = $ document

$doc.ready ()->

  $ '#slider'
  .bxSlider(
    auto: true,
    pager: true,
    infiniteLoop: false,
    hideControlOnEnd: false
  )