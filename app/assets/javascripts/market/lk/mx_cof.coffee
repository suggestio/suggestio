$(document).ready ->
  cbca.emptyPhoto = '/assets/images/market/lk/empty-image.gif'

  cbca.popup = new CbcaPopup()
  cbca.search = new CbcaSearch()

  cbca.statusBar = StatusBar
  cbca.statusBar.init()

  cbca.shop = CbcaShop
  cbca.shop.init()

  cbca.editAdPage = EditAdPage
  cbca.editAdPage.init()
  cbca.editAdPage.updatePreview()


  cbca.common = new CbcaCommon()


  if (typeof tinymce != 'undefined')
    tinymce.init(
      selector:'textarea.tiny-mce',
      ode: "textareas",
      menubar : false,
      statusbar : false,
      content_css: "/assets/javascripts/lib/tinymce/style-formats.css",
      toolbar: "undo redo | styleselect",
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
      ],
      setup: (editor)->
        editor.on 'keyup', (e)->
          clearTimeout(upd)
          updTextarea = ()->
            $('#ad_offer_text_value').val(editor.getContent()).trigger('change')
          upd = setTimeout(updTextarea, 500)
    )



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


$(document).on 'click', '.create-ad .color-list .color', ->
  $this = $(this)
  $wrap = $this.closest('.item')
  $wrap.find('.one-checkbox').trigger('click').get(0).checked = true

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
    $('#preview')
    .width($this.attr('data-width'))
    .height($this.attr('data-height'))
    .closest('table').attr('class', 'ad-preview ' + $this.attr('data-class'))


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


    $(document).on 'click', '#create-your-market-btn', (e)->
      e.preventDefault()

      $('#hello-message').hide()
      $('#create-your-market').show()


    $(document).on 'click', '.ads-list .tc-edit', (e)->
      e.preventDefault()

      $this = $(this)
      $.ajax(
        url: $this.attr('href')
        success: (data) ->
          $('#disable-ad').remove()
          $('.body-wrap').append(data)
          cbca.popup.showPopup('#disable-ad')
        error: (error) ->
          console.log(error)
      )

    $(document).on 'click', '#disable-ad .blue-but-small', ()->
      if($('#disable-ad .hide-content').css('display') == 'none')
        $('#disable-ad .hide-content').show()
        return false
      else
        $form = $(this).closest('form')
        if(!$.trim($form.find('textarea').val()))
          $form.find('.input').addClass('error')
          return false
        $.ajax(
          url: $form.attr('action')
          type: 'POST'
          data: $form.serialize()
          success: (data) ->
            cbca.popup.hidePopup('#disable-ad')
          error: (error) ->
            console.log(error)
        )

    $('input[type = "checkbox"]').each ()->
      $this = $(this)
      if($this.attr('data-checked') == 'checked')
        this.checked = true
      else
        $this.removeAttr('checked')


    $('.slide-content').each ()->
      $this = $(this)

      if($this.find('.err-msg').size())
        $this.slideDown()


  self.init()

#########################################################
## Страница создания/редактирования рекламной карточки ##
#########################################################
EditAdPage =

  updatePreview: () ->
    $form = $('#promoOfferForm')
    if($form.size())
      action = $form.find('#preview-action').val()
      $.ajax(
        type: 'POST'
        url: action
        data: $form.serialize()
        success: (data)->
          $('#preview').html(data)
        error: (error)->
          console.log(error)
      )

  init: () ->
    #################
    ## Старая цена ##
    #################
    if($('#ad_offer_oldPrice_value').val())
      $('#old-price-status').attr('data-checked', 'checked')
      $('.create-ad .old-price').show()

    $(document).on 'click', '#old-price-status', ->
      if(!$('#ad_offer_oldPrice_value').val())
        oldPrice = $('#priceValueInput').val()
        $('#ad_offer_oldPrice_value').val(oldPrice)
      $('.create-ad .old-price').toggle()
      cbca.editAdPage.updatePreview()

    ############
    ## Превью ##
    ############
    $(document).on 'change', '#promoOfferForm input', ()->
      cbca.editAdPage.updatePreview()

    $(document).on 'change', '#promoOfferForm textarea', ()->
      cbca.editAdPage.updatePreview()

    $(document).on 'keyup', '#promoOfferForm input', ()->
      clearTimeout(updatePreview)
      selfUpdatePreview = ()->
        cbca.editAdPage.updatePreview()
      updatePreview = setTimeout(selfUpdatePreview, 500)

    ########################################
    ## Положение элементов на iphone/ipad ##
    ########################################
    $(document).on 'click', '.select-iphone .iphone-block', ->
      $this = $(this)
      if(!$this.hasClass 'act')
        $('.iphone-block.act').removeClass 'act'
        $this.addClass 'act'
        $('#textAlign-phone').val($this.attr('data-value')).trigger('change')

    $(document).on 'click', '.select-ipad .ipad-block', ->
        $this = $(this)
        dataGroup = $this.attr 'data-group'

        if(!$this.hasClass 'act')
          $('.ipad-block.act[data-group = "'+dataGroup+'"]').removeClass 'act'
          $this.addClass 'act'
          $('#'+dataGroup).val($this.attr('data-value')).trigger('change')

    ##########################
    ## Переключение вкладок ##
    ##########################
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
        cbca.editAdPage.updatePreview()


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
      lvlEnabled = this.checked

      jsRoutes.controllers.MarketAd.updateShowLevelSubmit($this.attr('data-adid')).ajax(
        type: 'POST'
        data:
          'levelId': $this.attr('data-level')
          'levelEnabled': lvlEnabled
        success: (data) ->
          if(lvlEnabled)
            $this.closest('.item').removeClass('disabled')
          else
            check = true
            $this.closest('.item').find('input[type = "checkbox"]').each ()->
              if(this.checked)
                check = false
            if(check)
              $this.closest('.item').toggleClass('disabled', true)
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

      $.ajax(
        type: 'POST'
        dataType: 'JSON'
        url: $this.attr('action')
        data: $this.serialize()
        success: (data) ->
          if(!data.isEnabled)
            cbca.popup.hidePopup('#disable-shop')
            $('#disable-shop').remove()
            $('*[data-shop = "'+data.shopId+'"]').removeClass('enabled')
      )

    ###################################################
    ## Включение/выключение первой страницы магазина ##
    ###################################################
    $(document).on 'click', '#first-page-triger .enable-but', ->
      $shop = $(this).closest('.triger-wrap')
      if(!$shop.hasClass('enabled'))
        $shop.addClass('enabled')
        jsRoutes.controllers.MarketMartLk.setShopTopLevelAvailable($shop.attr('data-shop')).ajax(
          type: 'POST'
          data:
            'isEnabled': true
        )

    $(document).on 'click', '#first-page-triger .disable-but', ->
      $shop = $(this).closest('.triger-wrap')
      if($shop.hasClass('enabled'))
        $shop.removeClass('enabled')
        jsRoutes.controllers.MarketMartLk.setShopTopLevelAvailable($shop.attr('data-shop')).ajax(
          type: 'POST'
          data:
            'isEnabled': false
        )

#################
## Уведомления ##
#################
StatusBar =

  close: ($bar) ->
    if($bar.data('open'))
      $bar.data('open', false)
      $bar.slideUp()

  show: ($bar) ->
    if(!$bar.data('open'))
      $bar.data('open', true)
      $bar.slideDown()

  init: ()->

    $('.status-bar').each ()->
      _this = $(this)

      StatusBar.show _this
      close_cb = () ->
        StatusBar.close _this

      setTimeout close_cb, 5000

    $(document).on 'click', '.status-bar', ->
      StatusBar.close($(this))
