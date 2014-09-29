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
  slideSelector = $this.attr 'data-slide'
  $slideElement = $ slideSelector

  $slideElement.slideToggle()


$ document
.on 'click', '.js-slide-to', (e) ->
  e.preventDefault()
  $this = $ this
  targetSelector = $this.attr 'href'
  $target = $ targetSelector
  targetScrollTop = $target.offset().top

  $ 'body, html'
  .animate(
    scrollTop: targetScrollTop,
    800
  )
