$(document).ready ->
  if(document.getElementById('old-price-status') != null)
    if($('#ad_offer_oldPrice_value').val())
      $('#old-price-status').trigger('click')
      document.getElementById('old-price-status').checked = true
    else
      document.getElementById('old-price-status').checked = false

  cbca.emptyPhoto = '/assets/images/market/lk/empty-image.gif'

  cbca.popup = new CbcaPopup()
  cbca.search = new CbcaSearch()

  cbca.shop = CbcaShop
  cbca.shop.init()
  cbca.common = new CbcaCommon()


  if (typeof tinymce != 'undefined')
    tinymce.init(
      selector:'textarea.tiny-mce',
      ode: "textareas",
      menubar : false,
      plugins: "textcolor",
      statusbar : false,
      content_css: "/assets/javascripts/lib/tinymce/style-formats.css",
      toolbar: "undo redo | styleselect | forecolor",
      style_formats: [
        {
          title: 'Custom format 1'
          inline: 'span'
          classes: "custom-format-1"
        }
        {
          title: 'Custom format 2'
          inline: 'span'
          classes: "custom-format-2"
        }
      ]
    )


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

$(document).on 'click', '.one-checkbox', ->
  $this = $(this)
  dataName = $this.attr('data-name')
  dataFor = $this.attr('data-for')
  value = $this.attr('data-value')


  if(this.checked)
    $('.one-checkbox[data-name = "'+dataName+'"]').filter(':checked').removeAttr('checked')
    this.checked = true
    $('#'+dataFor).val(value)
  else
    $(this).removeAttr('checked')

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
  $wrap.find('.image-preview').attr('src', cbca.emptyPhoto)

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
        $('#search-results').html('')
        $('#shop-list').show()

  self.init()

##Общее оформление##
CbcaCommon = () ->

  self = this

  self.init = () ->
    $(document).on 'focus', '.input-wrap input, .input-wrap textarea', ->
      $(this).closest('.input-wrap').toggleClass('focus', true)

    $(document).on 'blur', '.input-wrap input, .input-wrap textarea', ->
      $(this).closest('.input-wrap').removeClass('focus')


    $(document).on 'click', '.submit', ->
      $this = $(this)
      formId = $this.attr('data-for')

      $('#'+formId).trigger('submit')

    $(document).on 'click', '#first-page-triger .enable-but', ->
      $shop = $(this).closest('.triger-wrap')
      $shop.addClass('enabled')
      jsRoutes.controllers.MarketMartLk.setShopTopLevelAvailable($shop.attr('data-shop')).ajax(
        type: 'POST'
        data:
          'isEnabled': true
      )

    $(document).on 'click', '#first-page-triger .disable-but', ->
      $shop = $(this).closest('.triger-wrap')
      $shop.removeClass('enabled')
      jsRoutes.controllers.MarketMartLk.setShopTopLevelAvailable($shop.attr('data-shop')).ajax(
        type: 'POST'
        data:
          'isEnabled': false
      )



  self.init()

#########################
## Работа с магазинами ##
#########################
CbcaShop =
  disableShop: (shopId) ->
    jsRoutes.controllers.MarketMartLk.shopOnOffForm(shopId).ajax(
      type: "GET",
      success:  (data) ->
        if(data.toString().trim())
          $('.body-wrap').append(data)
          cbca.popup.showPopup('#disable-shop')
      error: (error) ->
        console.log(error)
    )

  enableShop: (shopId) ->
    jsRoutes.controllers.MarketMartLk.shopOnOffSubmit(shopId).ajax(
      type: 'POST'
      dataType: 'JSON'
      data:
        'isEnabled': true
      error: (error) ->
        console.log(error)
    )


  init: () ->

    #########################
    ## Работа с чекбоксами ##
    #########################
    $(document).on 'change', '.ads-list .controls input[type = "checkbox"]', ->
      $this = $(this)

      jsRoutes.controllers.MarketAd.updateShowLevelSubmit($this.attr('data-adid')).ajax(
        type: 'POST'
        data:
          'levelId': $this.attr('data-level')
          'levelEnabled': this.checked
        success: (data) ->
          console.log(data)
        error: (data) ->
          console.log(data)
      )

    ###################################
    ## Включение/выключение магазина ##
    ###################################
    $(document).on 'click', '.renters-list .enable-but', ->
      shopId = $(this).closest('.item').attr('data-shop')
      $shop = $('#shop-list').find('.item[data-shop = "'+shopId+'"]')

      if(!$shop.hasClass('enabled'))
        $shop.addClass('enabled')
        $(this).closest('.item').addClass('enabled')
        cbca.shop.enableShop($shop.attr('data-shop'))


    $(document).on 'click', '.renters-list .disable-but', ->
      shopId = $(this).closest('.item').attr('data-shop')
      $shop = $('#shop-list').find('.item[data-shop = "'+shopId+'"]')

      if($shop.hasClass('enabled'))
        cbca.shop.disableShop($shop.attr('data-shop'))


    $(document).on 'click', '#shop-control .disable-but', ->
      $shop = $(this).closest('.big-triger')
      if($shop.hasClass('enabled'))
        cbca.shop.disableShop($shop.attr('data-shop'))

    $(document).on 'click', '#shop-control .enable-but', ->
      $shop = $(this).closest('.big-triger')

      if(!$shop.hasClass('enabled'))
        $shop.addClass('enabled')
        cbca.shop.enableShop($shop.attr('data-shop'))


    $(document).on 'submit', '#disable-shop form', (e) ->
      e.preventDefault()
      $this = $(this)
      data = $this.serialize()

      $.ajax(
        type: 'POST'
        dataType: 'JSON'
        url: $this.attr('action')
        data: data
        success: (data) ->
          if(!data.isEnabled)
            cbca.popup.hidePopup('#disable-shop')
            $('#disable-shop').remove()
            $('*[data-shop = "'+data.shopId+'"]').removeClass('enabled')
      )
##################
## CbcaShop end ##
##################