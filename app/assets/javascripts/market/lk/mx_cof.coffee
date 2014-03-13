$(document).ready ->
  if(document.getElementById('old-price-status') != null)
    document.getElementById('old-price-status').checked = false

  cbca.popup = new CbcaPopup();



$(document).on 'click', '.select-iphone .iphone-block', ->
  $this = $(this)
  if(!$this.hasClass 'act')
    $('.iphone-block.act').removeClass 'act'
    $this.addClass 'act'
    $('#textAlign-phone').val($this.attr('data-value'))

$(document).on 'click', '.select-ipad .ipad-block', ->
    $this = $(this)
    dataGroup = $this.attr 'data-group'

    if(!$this.hasClass 'act')
      $('.ipad-block.act[data-group = "'+dataGroup+'"]').removeClass 'act'
      $this.addClass 'act'
      $('#'+dataGroup).val($this.attr('data-value'))

$(document).on 'click', '.block .tab', ->
  $this = $(this)
  $wrap = $this.closest '.block'
  index = $wrap.find('.tabs .tab').index(this)

  if(!$this.hasClass 'act')
    $wrap.find('.tabs .act').removeClass 'act'
    $this.addClass 'act'
    $wrap.find('.tab-content').hide()
    $wrap.find('.tab-content').eq(index).show()
    $('#ad-mode').val($this.attr('data-mode'))

$(document).on 'click', '.create-ad .one-checkbox', ->
  $this = $(this)
  dataName = $this.attr('data-name')
  dataFor = $this.attr('data-for')
  value = $this.attr('data-value')

  $('.create-ad .one-checkbox[data-name = "'+dataName+'"]').filter(':checked').not(this).removeAttr('checked')
  this.checked = 'checked'
  $('#'+dataFor).val(value)

$(document).on 'click', '#old-price-status', ->
  $('.create-ad .old-price').toggle()

$(document).on 'click', '.create-ad .color-list .color', ->
  $this = $(this)
  $wrap = $this.closest('.item')

  $wrap.find('.one-checkbox').trigger('click')

$(document).on 'click', '.create-ad .mask-list .item', ->
  $this = $(this)

  $this.find('.one-checkbox').trigger('click' )

##popup##
$(document).on 'click', '.popup-but', ->
  $this = $(this)

  cbca.popup.showPopup($this.attr('href'))

$(document).on 'click', '.popup .close', (e)->
  e.preventDefault()
  cbca.popup.hidePopup()

$(document).on 'click', '#overlay', ->
    cbca.popup.hidePopup()

$(document).on 'click', '.popup .cancel', (e)->
    e.preventDefault()
    cbca.popup.hidePopup()

CbcaPopup = () ->

  showOverlay: ->
    $('#overlay').show()

  hideOverlay: ->
    $('#overlay').hide()

  showPopup: (popup) ->
    this.showOverlay()
    $popup = $(popup)
    $popup.show()
    marginTop = 0 - parseInt($popup.height()/2) + $(window).scrollTop()
    $popup.css('margin-top', marginTop)


  hidePopup: (popup) ->
    popup = '.popup' || popup
    this.hideOverlay()
    $(popup).hide()