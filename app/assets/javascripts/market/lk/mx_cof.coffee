
$(document).ready ->
  cbca.emptyPhoto = '/assets/images/market/lk/empty-image.gif'

  cbca.popup = new CbcaPopup()
  cbca.search = new CbcaSearch()

  cbca.statusBar = StatusBar
  cbca.statusBar.init()

  cbca.shop = CbcaShop


  cbca.editAdPage = EditAdPage
  cbca.editAdPage.init()

  cbca.common = new CbcaCommon()
  cbca.shop.init()
  cbca.editAdPage.updatePreview()

##СКРЫТЬ ВЫБРАННОЕ ФОТО##
$(document).on 'click', '.input-wrap .close', (event)->
  event.preventDefault()

  $this =$(this)
  $wrap = $this.closest('.input-wrap')
  $wrap.find('.image-key').val('')
  $wrap.find('.image-preview').attr('src', cbca.emptyPhoto)
  cbca.editAdPage.updatePreview()

##ПРЕВЬЮ РЕКЛАМНОЙ КАРТОЧКИ##
$(document).on 'click', '.device', ->
  $this = $(this)

  if(!$this.hasClass('selected'))
    $('.device.selected').removeClass('selected')
    $this.addClass('selected')

    $('#previewContainer')
    .width($this.attr('data-width'))
    .height($this.attr('data-height'))
    .closest('table').attr('class', 'ad-preview ' + $this.attr('data-class'))

    market.resize_preview_photos()


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

$(document).on 'click', '.popup .close', (event)->
  event.preventDefault()
  cbca.popup.hidePopup()

$(document).on 'click', '#overlay', ->
    cbca.popup.hidePopup()

$(document).on 'click', '.popup .cancel', (event)->
    event.preventDefault()
    cbca.popup.hidePopup()

CbcaPopup = () ->

  showOverlay: ->
    $('#popupsContainer, #overlay').show()

  hideOverlay: ->
    $('#popupsContainer, #overlay').hide()

  showPopup: (popup) ->
    this.showOverlay()
    $popup = $(popup)
    $popup.show()
    popupHeight = $popup.height()
    popupsContainerHeight = $('#popupsContainer').height()

    $('body').addClass('ovh')
    if(popupHeight > popupsContainerHeight)
      $('#overlay').height(popupHeight)
    else
      $('#overlay').height(popupsContainerHeight)

    $popup.find('.border-line-vertical').each () ->
      $this = $(this)
      $parent = $this.parent()

      $this.height($parent.height() - 10)

  hidePopup: (popup) ->
    popup = '.popup' || popup
    this.hideOverlay()
    $(popup).hide()
    $('#overlay, #overlayData').hide()
    $('body').removeClass 'ovh'


##поисковая строка##
CbcaSearch = () ->

  self = this

  self.search = (martId, searchString) ->
    jsRoutes.controllers.MarketLkAdn.searchSlaves(martId).ajax(
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

    #####################
    ## WIFI FORM start ##
    #####################

    $('.js-quiz__checkbox').removeAttr('disabled')

    $(document).on 'click', '.js-quiz__checkbox', (e)->
      $this = $(this)
      nextSelector = $this.attr('data-next')
      quizElement = $this.closest('.js-quiz__element')
      thisIndex = quizElement.attr('data-index')

      $('.js-quiz__element').not(this).each ()->
        $element = $(this)

        if($element.attr('data-index') > thisIndex)
          $element.hide().find('input').removeAttr('disabled').removeAttr('checked')

        if($element.attr('data-index') == thisIndex)
          $element.find('input').removeAttr('disabled')

      $this.attr('disabled', 'disabled')
      $('.js-quiz__result').hide()
      if(nextSelector == '#text0' || nextSelector == '#text1')
        nextSelector += ',#text3'
      $(nextSelector).show()

    ###################
    ## WIFI FORM end ##
    ###################

    if($('#newPasswordForm').length)
      cbca.popup.showPopup('#newPasswordForm')

    $(document).on 'click', '.js-popup-back', (e)->
      $this = $(this)
      targetPopupId = $this.attr('href')

      $this.closest('.popup').hide()
      $(targetPopupId).show()

    $(document).on 'click', '.js-remove-popup', (e)->
      $popup = $(this).closest('.popup')

      cbca.popup.hidePopup('#'+$popup.attr('id'))
      $('#'+$popup.attr('id')).remove()

    $(document).on 'click', '.js-submit-wrap', (e)->
      $this = $(this)

      $this.closest('form').find('input').removeAttr('disabled')
      $this.find('input').trigger('click')

    $(document).on 'click', '.js-submit-wrap input', (e)->
      e.stopPropagation()

      $(this).closest('form').find('input').removeAttr('disabled')


    ##todo все кнопки ajax/popup зарефакторить к этому обработчику##
    $(document).on 'click', '.js-btn', (e)->
      e.preventDefault()
      $this = $(this)
      href = $this.attr('href')

      if(!href)
        return false

      if(href && href.charAt(0) == '#')
        cbca.popup.showPopup(href)
      else
        $.ajax(
          url: href,
          success: (data)->
            $ajaxData = $(data)
            popupId = $ajaxData.attr('id')
            cbca.popup.hidePopup()
            $('#'+popupId).remove()
            $('#popupsContainer').append(data)
            cbca.popup.showPopup('#'+popupId)
        )

    $(document).on 'submit', '.js-form', (e)->
      e.preventDefault()
      $form = $(this)
      action = $form.attr('action')

      $.ajax(
        type: "POST",
        url: action,
        data: $form.serialize(),
        success: (data)->
          console.log(data)
      )

    $(document).on 'submit', '#recoverPwForm form', (e)->
      e.preventDefault()
      $form = $(this)
      action = $form.attr('action')

      $.ajax(
        type: "POST",
        url: action,
        data: $form.serialize(),
        success: (data)->
          $('#recoverPwForm').find('form').remove()
          $('#recoverPwForm').find('.content').append(data)
        error: (error)->
          $('#recoverPwForm').remove()
          $('#popupsContainer').append(error.responseText)
          cbca.popup.showPopup('#recoverPwForm')
      )

    ## Попапы с ошибками показывать сразу после перезагрузки страницы ##
    $('.popup .lk-error, .popup .error').each ()->
      $this = $(this)
      popupId = $this.closest('.popup').attr('id')

      cbca.popup.showPopup('#'+popupId)

    $(document).on 'click', '#advReqRefuseShow', (e)->
      e.preventDefault()

      $('#advReqRefuse').show()
      $('#advReqAccept').hide()

    $(document).on 'click', '#advReqAcceptShow', (e)->
      e.preventDefault()

      $('#advReqRefuse').hide()
      $('#advReqAccept').show()

    $(document).on 'submit', '#advReqRefuse', (e)->
      $this = $(this)
      $textarea = $this.find('textarea')

      if(!$textarea.val())
        $textarea.closest('.input').addClass('error')
        return false
      else
        return true

    $(document).on 'click', '.js-advertising-requests-item_get-info', (e)->
      e.preventDefault()
      $this = $(this)
      href = $this.attr('href')

      $.ajax(
        url: href,
        success: (data)->
          console.log(data)
          $('#advReqWind').remove()
          $('#popupsContainer').append(data).find('.sm-block').addClass('double-size')

          cbca.popup.showPopup('#advReqWind')
          $('#advReqRefuse').hide()
      )

    $(document).on 'click', '.advs-nodes__node-link_show-popup', (e)->
      e.preventDefault()
      $this = $(this)
      href = $this.attr('href')

      $.ajax(
        url: href,
        success: (data)->
          $('#dailyMmpsWindow').remove()
          $('#popupsContainer').append(data)

          cbca.popup.showPopup('#dailyMmpsWindow')
      )

    $(document).on 'click', '.js-slide-btn', (e)->
      e.preventDefault()
      $this = $(this)
      targetId = $this.attr('href')

      $('#'+targetId).slideToggle()

    $(document).on 'click', '.lk-edit-form__block_title .js-close-btn', (e)->
        e.preventDefault()
        $this = $(this)

        $this.parent().parent().slideUp()

    $(document).on 'click', '.ads-list-block__preview_add-new', ()->
      $this = $(this)

      $this.parent().find('.ads-list-block__link')[0].click()


    $(document).on 'click', '.js-g-slide-toggle', (e)->
      e.preventDefault()
      $this = $(this)
      href = $this.attr('href')

      if(href)
        $.ajax(
          url: href
          success: (data) ->
            $data = $(data).hide()
            $this.closest('.js-slide-wrap').append($data).find('.js-slide-content:first').slideDown()
            $this.attr('href', '')
        )
      else
        $this.closest('.js-slide-wrap').find('.js-slide-content:first').slideToggle()

      $this.toggleClass('open')
      if($this.hasClass('open'))
        $this.html('Свернуть')
      else
        $this.html('Развернуть')



    $(document).on 'focus', '.input-wrap input, .input-wrap textarea', ->
      $(this).closest('.input-wrap').toggleClass('focus', true)

    $(document).on 'blur', '.input-wrap input, .input-wrap textarea', ->
      $(this).closest('.input-wrap').removeClass('focus')


    $(document).on 'click', '.submit', ->
      $this = $(this)
      formId = $this.attr('data-for')

      $('#'+formId).trigger('submit')


    $(document).on 'click', '#create-your-market-btn', (event)->
      event.preventDefault()

      $('#hello-message').hide()
      $('#create-your-market').show()


    $(document).on 'click', '.ads-list .js-tc-edit', (event)->
      event.preventDefault()

      $this = $(this)
      $.ajax(
        url: $this.attr('href')
        success: (data) ->
          $('#disable-ad, #anotherNodes').remove()
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

    ##########################
    ## Работа с checkbox`ов ##
    ##########################
    $('input[type = "checkbox"]').each ()->
      $this = $(this)
      if($this.attr('data-checked') == 'checked')
        this.checked = true
      else
        $this.removeAttr('checked')

    $(document).on 'click', '.create-ad .color-list .color', ->
      $this = $(this)
      $wrap = $this.closest('.item')
      $checkbox = $wrap.find('.one-checkbox')
      dataName = $checkbox.attr('data-name')
      dataFor = $checkbox.attr('data-for')
      value = $checkbox.attr('data-value')
      checkbox = $checkbox.get(0)

      if(!checkbox.checked)
        $('.one-checkbox[data-name = "'+dataName+'"]').filter(':checked').removeAttr('checked')
        checkbox.checked = true
        $('#'+dataFor).val(value)
      else
        $checkbox.removeAttr('checked')

      cbca.editAdPage.updatePreview()

    $(document).on 'click', '.create-ad .mask-list .item', (event)->
      $this = $(this)

      $checkbox = $this.find('.one-checkbox')
      dataName = $checkbox.attr('data-name')
      dataFor = $checkbox.attr('data-for')
      value = $checkbox.attr('data-value')
      checkbox = $checkbox.get(0)

      if(!checkbox.checked)
        $('.one-checkbox[data-name = "'+dataName+'"]').filter(':checked').removeAttr('checked')
        checkbox.checked = true
        $('#'+dataFor).val(value)
      else
        $checkbox.removeAttr('checked')

      cbca.editAdPage.updatePreview()


    ########################################
    ## Набор checkbox`ов, с одним checked ##
    ########################################
    $(document).on 'click', '.one-checkbox', (event)->
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

      event.stopPropagation()


    $('.slide-content').each ()->
      $this = $(this)

      if($this.find('.err-msg').size())
        $this.slideDown()


    $('.nodes .node').each () ->
      $(this).data('dataLoaded', false)

    $(document).on 'click', '.nodes .node .toggle', (e) ->
      e.preventDefault()
      $this = $(this)
      $node = $this.parent()

      if($node.hasClass('open'))
        $node.removeClass('open').next().next().slideUp()
        $this.html('Развернуть')
      else if($node.data('dataLoaded'))
        $node.addClass('open').next().next().slideDown()
        $this.html('Свернуть')
      else
        $.ajax(
          url: $this.attr('href')
          success: (data) ->
            $node.addClass('open').data('dataLoaded', true).next().after('<div class="ads-list small">'+data+'</div>')
            $node.next().next().slideDown('normal', () ->  market.resize_preview_photos())
            $this.html('Свернуть')
          error: (error) ->
            console.log(error)
        )


    $(document).on 'click', '.add-to-another-node', (e) ->
      e.preventDefault()
      $this = $(this)

      $.ajax(
        url:  $this.find('a').attr('href'),
        success: (data) ->
          $('#disable-ad, #anotherNodes').remove()
          $('.body-wrap').append(data)
          cbca.popup.showPopup('#anotherNodes')
        error: (data) ->
          console.log(data)
      )


    $('.border-line-vertical').each () ->
      $this = $(this)
      $parent = $this.parent()

      $this.height($parent.height() - 10)


    $(document).on 'click', '.transactions-history .toggle', (e) ->
      e.preventDefault()
      $this = $(this)
      $parent = $this.parent()

      if($parent.hasClass('open'))
        $parent.removeClass('open').parent().find('.transactions-list').slideUp()
        $this.html('Развернуть')
      else
        $parent.addClass('open').parent().find('.transactions-list').slideDown()
        $this.html('Свернуть')




  self.init()

#########################################################
## Страница создания/редактирования рекламной карточки ##
#########################################################
EditAdPage =

  updatePreview: () ->
    return false

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
    jsRoutes.controllers.MarketLkAdn.nodeOnOffForm(shopId).ajax(
      type: "GET",
      success:  (data) ->
        if(data.toString().trim())
          $('.body-wrap').append(data)
          cbca.popup.showPopup('#disable-shop')
      error: (error) ->
        console.log(error)
    )

  enableShop: (shopId) ->
    jsRoutes.controllers.MarketLkAdn.nodeOnOffSubmit(shopId).ajax(
      type: 'POST'
      dataType: 'JSON'
      data:
        'isEnabled': true
      error: (error) ->
        console.log(error)
    )


  fixActiveAds: (labelSelector, count)->
    if($(labelSelector).find('input[type = "checkbox"]').filter(':checked').size() >= count)
      $(labelSelector).find('input[type = "checkbox"]').not(':checked').each ()->
        $(this).removeAttr('checked').attr('disabled', 'disabled').closest('label').addClass('inactive')
    else
      $(labelSelector).find('input[type = "checkbox"]').not(':checked').each ()->
        $(this).removeAttr('disabled').closest('label').removeClass('inactive')

  checkDisabledAds: ()->
    $('.ads-list .item').each ()->
      check = true
      $this = $(this)
      if($this.find('label').not('.inactive').find('input[type = "checkbox"]').size())
        $this.find('label').not('.inactive').find('input[type = "checkbox"]').each ()->
          if(this.checked)
            check = false
        if(check)
          $this.toggleClass('disabled', true)


  init: () ->

    self = this
    self.martAdsLimit = 0 || parseInt($('#maxMartAds').val(),10)
    self.shopAdsLimit = 0 || parseInt($('#maxShopAds').val(),10)

    ##########################################
    ## Чекбоксы в списке рекламных плакатов ##
    ##########################################
    $(document).on 'change', '.ads-list .ads-list-block__controls input[type = "checkbox"]', ->
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

          cbca.shop.checkDisabledAds()
        error: (data) ->
          console.log(data)
      )

    cbca.shop.checkDisabledAds()

    cbca.shop.fixActiveAds('.shop-catalog', self.shopAdsLimit)

    $(document).on 'change', '.shop-catalog input[type = "checkbox"]', ->
      cbca.shop.fixActiveAds('.shop-catalog', self.shopAdsLimit)


    cbca.shop.fixActiveAds('.martAd-fix', self.martAdsLimit)

    $(document).on 'change', '.martAd-fix input[type = "checkbox"]', ->
      cbca.shop.fixActiveAds('.martAd-fix', self.martAdsLimit)

    ###################################
    ## Включение/выключение магазина ##
    ###################################
    $(document).on 'click', '.nodes-list .enable-but', ->
      shopId = $(this).closest('.item').attr('data-shop')
      $shop = $('#shop-list').find('.item[data-shop = "'+shopId+'"]')

      if(!$shop.hasClass('enabled'))
        $shop.addClass('enabled')
        $(this).closest('.item').addClass('enabled')
        cbca.shop.enableShop($shop.attr('data-shop'))


    $(document).on 'click', '.nodes-list .disable-but', ->
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


    $(document).on 'submit', '#disable-shop form', (event) ->
      event.preventDefault()
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
        jsRoutes.controllers.MarketLkAdn.setSlaveTopLevelAvailable($shop.attr('data-shop')).ajax(
          type: 'POST'
          data:
            'isEnabled': true
        )

    $(document).on 'click', '#first-page-triger .disable-but', ->
      $shop = $(this).closest('.triger-wrap')
      if($shop.hasClass('enabled'))
        $shop.removeClass('enabled')
        jsRoutes.controllers.MarketLkAdn.setSlaveTopLevelAvailable($shop.attr('data-shop')).ajax(
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

######################
## TODO: отрефакторить
######################
market =

  init_colorpickers : () ->
    $('.js-custom-color').each () ->

      cb = ( _this ) ->
        i = Math.random()
        _this.ColorPicker
      	  color: '#0000ff'
      	  onShow: (colpkr) ->
      	    $(colpkr).fadeIn(500)
      	  onHide: (colpkr) ->
      	    $(colpkr).fadeOut(500)
      	  onChange: (hsb, hex, rgb) ->
      	    market.ad_form.queue_block_preview_request()
      	    _this.find('input').val(hex).trigger('change')
      	    _this.css
      	      'background-color' : '#' + hex
      cb( $(this) )

  ## Главная страница ЛК торгового центра
  mart :
    init : () ->
      market.init_colorpickers()

      $('#installScriptButton').bind 'click', () ->

        cbca.popup.showPopup('#installScriptPopup')

        return false


  ################################
  ## Класс для работы с картинками
  ################################
  img :

    init_upload : () ->

      $('.w-async-image-upload').bind "change", () ->

        relatedFieldId = $(this).attr 'data-related-field-id'
        form_data = new FormData()

        is_w_block_preview = $(this).attr 'data-w-block-preview'

        if $(this)[0].type == 'file'
          form_data.append $(this)[0].name, $(this)[0].files[0]

        request_params =
          url : $(this).attr "data-action"
          method : 'post'
          data : form_data
          contentType: false
          processData: false
          success : ( resp_data ) ->

            if typeof is_w_block_preview != 'undefined'
              market.ad_form.queue_block_preview_request()

            $('#' + relatedFieldId + ' .image-key, #' + relatedFieldId + ' .js-image-key').val(resp_data.image_key).trigger('change')
            $('#' + relatedFieldId + ' .image-preview').show().attr "src", resp_data.image_link

        $.ajax request_params

        return false

    crop :
      init_triggers : () ->
        $('.js-img-w-crop').unbind 'click'
        $('.js-img-w-crop').bind 'click', () ->

          img_key = jQuery('input', $(this).parent()).val()
          img_name = jQuery('input', $(this).parent()).attr 'name'

          if img_key == ''
            alert 'сначала нужно загрузить картинку'
            return false

          width = $('.sm-block').attr 'data-width'
          height = $('.sm-block').attr 'data-height'

          marker = $(this).attr 'data-marker'

          $.ajax
            url : '/img/crop/' + img_key + '?width=' + width + '&height=' + height + '&marker=' + marker
            success : ( data ) ->
              $('#overlay, #overlayData').show()
              $('#overlayData').html data

              market.img.crop.init( img_name )

          return false

      save_crop : ( form_dom ) ->

        offset_x = parseInt( $('#imgCropTool img').css('left').replace('px', '') ) || 0
        c_offset_x = this.container_offset_x
        #offset_x = offset_x - parseInt c_offset_x

        offset_y = parseInt( $('#imgCropTool img').css('top').replace('px', '') ) || 0
        c_offset_y = this.container_offset_y
        #offset_y = offset_y - parseInt c_offset_y

        ci = this.crop_tool_img_dom

        sw = parseInt ci.attr 'data-width'
        sh = parseInt ci.attr 'data-height'

        rw = parseInt ci.width()
        rh = parseInt ci.height()

        offset_x = sw * offset_x / rw
        offset_y = sh * offset_y / rh

        console.log 'offset_x : ' + offset_x
        console.log 'offset_y : ' + offset_y

        target_offset = "+" + Math.round( Math.abs(offset_x) ) + "+" + Math.round(Math.abs(offset_y))

        target_size = rw + 'x' + rh

        tw = parseInt this.crop_tool_dom.attr 'data-width'
        th = parseInt this.crop_tool_dom.attr 'data-height'

        resize = rw*2 + 'x' + rh*2

        if sw / sh > tw / th
          ch = sh
          cw = ch * tw / th

        else
          cw = sw
          ch = cw * th / tw

        crop_size = Math.round( cw ) + 'x' + Math.round( ch )

        jQuery('input[name=crop]', form_dom).val( crop_size + target_offset )
        jQuery('input[name=resize]', form_dom).val( resize )

        form_dom1 = $('#imgCropTool form')
        image_name = this.image_name

        $.ajax
          url : form_dom1.attr 'action'
          method : 'post'
          data : form_dom1.serialize()
          success : ( img_data ) ->
            console.log market.img.crop.img_name
            $('input[name=\'' + market.img.crop.img_name + '\']').val img_data.image_key
            market.ad_form.queue_block_preview_request request_delay=10

            $('#overlay, #overlayData').hide()
            $('#overlayData').html ''


      init : (img_name) ->
        this.img_name = img_name
        this.crop_tool_dom = $('#imgCropTool')
        this.crop_tool_container_dom = jQuery('.js-crop-container', this.crop_tool_dom)
        this.crop_tool_container_div_dom = jQuery('div', this.crop_tool_container_dom)
        this.crop_tool_img_dom = jQuery('img', this.crop_tool_dom)

        width = parseInt this.crop_tool_dom.attr 'data-width'
        height = parseInt this.crop_tool_dom.attr 'data-height'

        img_width = parseInt this.crop_tool_img_dom.attr 'data-width'
        img_height = parseInt this.crop_tool_img_dom.attr 'data-height'

        this.crop_tool_container_dom.css
          'width' : width + 'px'
          'height' : height + 'px'


        ## отресайзить картинку по нужной стороне

        wbh = width/height
        img_wbh = img_width/img_height

        if wbh > img_wbh
          img_new_width = width
          img_new_height = img_height * img_new_width / img_width
        else
          img_new_height = height
          img_new_width = img_new_height * img_width / img_height

        container_offset_x = parseInt img_new_width - width
        container_offset_y = parseInt img_new_height - height

        this.crop_tool_img_dom.css
          'width' : img_new_width + 'px'
          'height' : img_new_height + 'px'

        this.crop_tool_container_div_dom.css
          'margin-left' : -container_offset_x + 'px'
          'margin-top' : -container_offset_y + 'px'

        this.container_offset_x = container_offset_x
        this.container_offset_y = container_offset_y

        x1 = this.crop_tool_container_div_dom.offset()['left']
        y1 = this.crop_tool_container_div_dom.offset()['top']

        x2 = x1 + container_offset_x
        y2 = y1 + container_offset_y

        this.crop_tool_img_dom.draggable
          'containment' : [x1,y1,x2,y2]

        ## Забиндить событие на сохранение формы
        $('#imgCropTool form').bind 'submit', () ->
          market.img.crop.save_crop $(this)

          return false

  ################################
  ## Размещение рекламной карточки
  ################################
  adv_form :
    update_price : () ->
      $.ajax
        url : $('#advsPriceUpdateUrl').val()
        method : 'post'
        data : $('#advsFormBlock form').serialize()
        success : ( data ) ->
          $('.js-pre-price').html data

    submit : () ->
      $('#advsFormBlock form').submit()

    init : () ->
      $('.js-datepicker').each () ->
        $(this).datetimepicker
          lang:'ru'
          i18n:
            ru:
              months:['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь']
              dayOfWeek:["Вс", "Пн", "Вт", "Ср","Чт", "Пт", "Сб"]
          timepicker:false
          format:'Y-m-d'

      $('#advsSubmitButton').bind 'click', () ->
        market.adv_form.submit()
      $('#advsFormBlock input').bind 'change', () ->
        cf_id = $(this).attr 'data-connected-field'
        cf = $('#' + cf_id)
        if typeof cf_id != 'undefined'
          if $(this).is(':checked')
            cf.addClass 'advs-nodes__node_active'
            cf.find('.advs-nodes__node-dates').removeClass 'advs-nodes__node-dates_hidden'
            cf.find('.advs-nodes__node-options').removeClass 'advs-nodes__node-options_hidden'
          else
            cf.removeClass 'advs-nodes__node_active'
            cf.find('.advs-nodes__node-dates').addClass 'advs-nodes__node-dates_hidden'
            cf.find('.advs-nodes__node-options').addClass 'advs-nodes__node-options_hidden'

        market.adv_form.update_price()

  ##############################
  ## Редактор рекламной карточки
  ##############################
  ad_form :

    preview_request_delay : 300

    request_block_preview : ( is_with_auto_crop ) ->
      action = $('.js-ad-block-preview-action').val()

      if(action)
        $.ajax
          url : action
          method : 'post'
          data : $('#promoOfferForm').serialize()
          success : ( data ) ->

            if is_with_auto_crop == true
              console.log 'необходим авто кроп'

            $('#adFormBlockPreview').html data
            $('.js-mvbl').draggable
              stop : () ->
                connected_input = $(this).attr 'data-connected-input'
                pos = $(this).position()

                $('input[name=\'' + connected_input + '.coords.x\']').val pos['left']
                $('input[name=\'' + connected_input + '.coords.y\']').val pos['top']


    queue_block_preview_request : ( request_delay, is_with_auto_crop ) ->
      request_delay = request_delay || this.preview_request_delay

      if typeof this.block_preview_request_timer != 'undefined'
        clearTimeout this.block_preview_request_timer

      is_with_auto_crop = is_with_auto_crop || false

      cb = () ->
        market.ad_form.request_block_preview is_with_auto_crop

      this.block_preview_request_timer = setTimeout cb, request_delay

    init_block_editor : () ->
      market.img.init_upload()
      market.img.crop.init_triggers()

      $('.js-align-editor').each () ->
        input = $(this).find 'input'
        buttons = $(this).find '.js-ae-button'

        buttons.bind 'click', () ->
          align_value = $(this).attr 'data-align'
          input.val align_value

          buttons.removeClass 'align-editor__button_active'
          $(this).addClass 'align-editor__button_active'

          market.ad_form.queue_block_preview_request()


      $('.js-custom-font-select, .js-custom-font-family-select').bind 'change', () ->
        market.ad_form.queue_block_preview_request()

      $('.js-input-w-block-preview').bind 'keyup', () ->
        market.ad_form.queue_block_preview_request()

      $('.js-block-height-editor-button').bind 'click', () ->

        _cell_size = 140
        _cell_padding = 20

        _direction = $(this).attr 'data-change-direction'

        _p = $(this).parent()
        _value_dom = _p.find('input')

        _cur_value = parseInt _value_dom.val()
        _min_value = parseInt _value_dom.attr 'data-min-value'
        _max_value = parseInt _value_dom.attr 'data-max-value'

        _cur_cells_value = Math.floor _cur_value / _cell_size

        if _direction == 'decrease'
          _cur_cells_value--
        else
          _cur_cells_value++

        _new_value = _cur_cells_value * _cell_size + ( _cur_cells_value - 1 ) * _cell_padding

        if _new_value > _max_value
          _new_value = _max_value

        if _new_value < _min_value
          _new_value = _min_value

        _value_dom.val _new_value

        console.log _value_dom.val()
        market.ad_form.queue_block_preview_request request_delay=10

    set_descr_editor_bg : () ->
      hex = '#' + $('#ad_descr_bgColor').val()
      $('#ad_descr_text_ifr').contents().find('body').css 'background-color': hex

    init : () ->
      tinymce.init(
        selector:'textarea.js-tinymce',
        width: 615,
        height: 300,
        menubar: false,
        statusbar : false,
        plugins: 'link, textcolor, paste, colorpicker',
        toolbar: ["styleselect | fontsizeselect | alignleft aligncenter alignright | bold italic | colorpicker | link | removeformat" ],
        content_css: '/assets/stylesheets/market/descr.css',
        fontsize_formats: '18px 22px 26px 30px 34px 38px 42px 46px 50px 54px 58px 62px 66px 70px 74px 80px 84px',

        style_formats: [
          {title: 'Favorit Light Cond C Regular', inline: 'span', styles: { 'font-family':'favoritlightcondcregular'}},
          {title: 'Favorit Cond C Bold', inline: 'span', styles: { 'font-family':'favoritcondc-bold-webfont'}},
          {title: 'Helios Thin', inline: 'span', styles: { 'font-family':'heliosthin'}},
          {title: 'Helios Cond Light', inline: 'span', styles: { 'font-family':'helioscondlight-webfont'}},
          {title: 'Helios Ext Black', inline: 'span', styles: { 'font-family':'HeliosExtBlack'}},
          {title: 'PF Din Text Comp Pro Medium', inline: 'span', styles: { 'font-family':'pfdintextcomppro-medium-webfont'}},
          {title: 'Futur Fut C', inline: 'span', styles: { 'font-family':'futurfutc-webfont'}},
          {title: 'Pharmadin Condensed Light', inline: 'span', styles: { 'font-family':'PharmadinCondensedLight'}},
          {title: 'Newspaper Sans', inline: 'span', styles: { 'font-family':'newspsan-webfont'}},
          {title: 'Rex Bold', inline: 'span', styles: { 'font-family':'rex_bold-webfont'}},
          {title: 'Perforama', inline: 'span', styles: { 'font-family':'perforama-webfont'}},
          {title: 'Decor C', inline: 'span', styles: { 'font-family':'decorc-webfont'}},
          {title: 'BlocExt Cond', inline: 'span', styles: { 'font-family':'blocextconc-webfont'}},
          {title: 'Bodon Conc', inline: 'span', styles: { 'font-family':'bodonconc-webfont'}},
          {title: 'Confic', inline: 'span', styles: { 'font-family':'confic-webfont'}}
        ],

        language: 'RU_ru'

        font_size_style_values : '1px,2px',
        setup: (editor) ->
          editor.on 'init', (e) ->
            market.ad_form.set_descr_editor_bg()
      )

      ## Предпросмотр карточки с описанием
      $('.js-ad-preview-button').bind 'click', () ->
        tinyMCE.triggerSave()
        $.ajax
          url : $('.js-ad-block-full-preview-action').val()
          method : 'post'
          data : $('#promoOfferForm').serialize()
          success : ( data ) ->
            $('#popupsContainer').html '<div class="ad-full-preview" id="adFullPreview"><div class="sio-mart-showcase">' + data + '</div></div>'
            $('#adFullPreview .sm-block').addClass 'double-size'
            cbca.popup.showPopup 'adFullPreview'

        return false

      $(document).on 'change', '#ad_descr_bgColor', (e)->
        market.ad_form.set_descr_editor_bg()

      market.img.crop.init_triggers()
      this.request_block_preview()
      this.init_block_editor()

      icons_dom = $('#adFormBlocksList div')

      icons_dom.bind 'click', () ->

        icons_dom.removeClass 'blocks-list-icons__single-icon_active'
        $(this).addClass 'blocks-list-icons__single-icon_active'

        block_id = $(this).attr 'data-block-id'
        block_editor_action = $('#adFormBlocksList .block-editor-action').val()

        $('input[name=\'ad.offer.blockId\']').val block_id

        $.ajax
          url : block_editor_action
          method : 'post'
          data : $('#promoOfferForm').serialize()
          success : ( data ) ->
            $('#adFormBlockEditor').html data
            market.ad_form.init_block_editor()
            market.init_colorpickers()
            market.ad_form.request_block_preview()

  resize_preview_photos : () ->
    $('.preview .poster-photo').each () ->
      $this = $(this)

      image_w = parseInt $this.attr "data-width"
      image_h = parseInt $this.attr "data-height"

      cw = $this.closest('.preview').width()
      ch = $this.closest('.preview').height()

      if image_w / image_h < cw / ch
        nw = cw
        nh = nw * image_h / image_w
      else
        nh = ch
        nw = nh * image_w / image_h

      css_params =
        'width' : nw + 'px'
        'height' : nh + 'px'
        'margin-left' : - nw / 2 + 'px'
        'margin-top' : - nh / 2 + 'px'

      $this.css css_params



  init: () ->

    $(document).bind 'keyup', ( event ) ->
      if event.keyCode == 27
        cbca.popup.hidePopup()

    this.ad_form.init()
    $(document).ready () ->
      market.img.init_upload()
      market.resize_preview_photos()
      market.mart.init()
      market.adv_form.init()

market.init()
window.market=market