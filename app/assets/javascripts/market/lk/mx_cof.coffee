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
        editor.on 'keyup', (event)->
          clearTimeout(upd)
          updTextarea = ()->
            $('#ad_offer_text_value').val(editor.getContent()).trigger('change')
          upd = setTimeout(updTextarea, 500)
    )





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


    $(document).on 'click', '.ads-list .tc-edit', (event)->
      event.preventDefault()

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

      $this.height($parent.height())


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
      	    _this.find('input').val hex
      	    _this.css
      	      'background-color' : '#' + hex
      cb( $(this) )

  ## Главная страница ЛК торгового центра
  mart :
    init : () ->
      market.init_colorpickers()

      $('#installScriptButton').bind 'click', () ->
        $('#installScriptPopup, #overlay').show()

        _dom = $('#installScriptPopup')
        ish = _dom.height()

        params =
          'margin-top' : - ish / 2

        _dom.css params

        return false


  ################################
  ## Класс для работы с картинками
  ################################
  img :

    init_upload : () ->

      $('.w-async-image-upload').bind "change", () ->

        relatedFieldId = $(this).attr 'data-related-field-id'
        form_data = new FormData()

        console.log relatedFieldId

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
          img_key = $(this).attr 'data-image-key'
          width = $(this).attr 'data-width'
          height = $(this).attr 'data-height'

          $.ajax
            url : '/img/crop/' + img_key + '?width=' + width + '&height=' + height
            success : ( data ) ->
              $('#overlay, #overlayData').show()
              $('#overlayData').html data

              market.img.crop.init()

          return false

      init : () ->
        crop_tool_dom = $('#imgCropTool')
        crop_tool_container_dom = jQuery('.js-crop-container', crop_tool_dom)
        crop_tool_img_dom = jQuery('img', crop_tool_dom)

        width = parseInt crop_tool_dom.attr 'data-width'
        height = parseInt crop_tool_dom.attr 'data-height'

        img_width = parseInt crop_tool_img_dom.attr 'data-width'
        img_height = parseInt crop_tool_img_dom.attr 'data-height'

        crop_tool_container_dom.css
          'width' : width + 'px'
          'height' : height + 'px'

        crop_tool_dom.css
          'width' : width + 'px'

        ## отресайзить картинку по нужной стороне

        wbh = width/height
        img_wbh = img_width/img_height

        if wbh < img_wbh
          img_new_width = width
          img_new_height = img_height * img_new_width / img_width
        else
          img_new_height = height
          img_new_width = img_new_height * img_width / img_height

        crop_tool_img_dom.css
          'width' : img_new_width + 'px'
          'height' : img_new_height + 'px'

        crop_tool_img_dom.draggable()
        
  ##############################
  ## Редактор рекламной карточки
  ##############################
  ad_form :

    preview_request_delay : 300

    request_block_preview : () ->
      action = $('.js-ad-block-preview-action').val()

      $.ajax
        url : action
        method : 'post'
        data : $('#promoOfferForm').serialize()
        success : ( data ) ->
          $('#adFormBlockPreview').html data

    queue_block_preview_request : ( request_delay ) ->

      request_delay = request_delay || this.preview_request_delay

      if typeof this.block_preview_request_timer != 'undefined'
        clearTimeout this.block_preview_request_timer

      this.block_preview_request_timer = setTimeout market.ad_form.request_block_preview, request_delay

    init_block_editor : () ->
      market.img.init_upload()
      market.img.crop.init_triggers()
      
      $('.js-int-only-input').bind 'keyup', () ->


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

    init : () ->
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
    this.ad_form.init()
    $(document).ready () ->
      market.img.init_upload()
      market.resize_preview_photos()
      market.mart.init()

market.init()
window.market=market
