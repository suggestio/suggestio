$(document).ready ->
  if(document.getElementById('old-price-status') != null)
    document.getElementById('old-price-status').checked = false

  cbca.popup = new CbcaPopup()
  cbca.search = new CbcaSearch()
  cbca.common = new CbcaCommon()



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


##СКРЫТЬ ВЫБРАННОЕ ФОТО##
$(document).on 'click', '.input-wrap .close', (e)->
  e.preventDefault()

  $this =$(this)
  $wrap = $this.closest('.input-wrap')
  $wrap.find('.image-key').val('')
  $wrap.find('.image-preview').attr('src', '')

##ПРЕВЬЮ РЕКЛАМНОЙ КАРТОЧКИ##
$(document).on 'click', '.device', ->
  $this = $(this)

  if(!$this.hasClass('selected'))
    $('.device.selected').removeClass('selected')
    $this.addClass('selected')


##ФОТО ТОВАРА##
$(document).on 'change', '#product-photo', ->
  value = $(this).val()
  console.log(value)
  $('#upload-product-photo').find('input[type = "file"]').val(value)
  ##$('#upload-product-photo').find('form').trigger('submit')##


##ПОПАПЫ##
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


##поисковая строка##
CbcaSearch = () ->

  self = this

  self.search = (martId, searchString) ->
    jsRoutes.controllers.MarketMartLk.searchShops(martId).ajax(
      type: "POST",
      data:
        'q': searchString
      success: (data) ->
        if(data.trim().length)
          $('#search-results').html(data.trim())
        else
          $('#search-results').html('')
      error: (error) ->
        console.log(error)
    )


  self.init = () ->
    $(document).on 'keyup', '#searchShop', ->
      $this = $(this)
      if($this.val().trim())
        $('#shop-list').hide()
        self.search($this.attr('data-mart-id'), $this.val())
      else
        $('#shop-list').show()

  self.init()

##CbcaInputStyle##
CbcaCommon = () ->

  self = this

  self.init = () ->
    $(document).on 'focus', '.input-wrap input, .input-wrap textarea', ->
      $(this).closest('.input-wrap').toggleClass('focus', true)

    $(document).on 'blur', '.input-wrap input, .input-wrap textarea', ->
      $(this).closest('.input-wrap').removeClass('focus')

    $(document).on 'click', '#shop-list .disable-but', ->
      $(this).closest('.item').removeClass('enabled')

    $(document).on 'click', '#shop-list .enable-but', ->
      $(this).closest('.item').addClass('enabled')

    $(document).on 'click', '.big-triger .disable-but', ->
      $(this).closest('.big-triger').removeClass('enabled')

    $(document).on 'click', '.big-triger .enable-but', ->
      $(this).closest('.big-triger').addClass('enabled')

    $(document).on 'click', '.submit', ->
      $this = $(this)
      formId = $this.attr('data-for')

      $('#'+formId).trigger('submit')

  self.init()