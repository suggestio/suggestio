market =
  market_image_form_upload : ( data ) ->

    $('#promoOfferForm input[name=image_key]').val data.image_key
    $('#promoOfferForm .image-preview').show().attr "src", data.image_link


  init_upload_forms : () ->
    window.market_image_form_upload = market.market_image_form_upload

    $('.file-upload-form').bind "submit", () ->

      js_callback_input = jQuery "#formJsCallback", this
      js_callback_input = js_callback_input.val()

      form_data = new FormData()
      for input in jQuery('input', this)
        if input.type == 'file'
          form_data.append input.name, input.files[0]

      request_params =
        url : $(this).attr "action"
        method : 'post'
        data : form_data
        contentType: false
        processData: false
        success : ( resp_data ) ->
          window[js_callback_input](resp_data)

      $.ajax request_params

      return false


  init: () ->
    $(document).ready () ->
      market.init_upload_forms()

market.init()
window.market=market