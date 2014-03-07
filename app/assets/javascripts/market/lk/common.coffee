market =
  init_upload_forms : () ->

    $('.file-upload-form').bind "submit", () ->
      related_form_id_input = jQuery ".related-form-id", this
      related_form_id = related_form_id_input.val()

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
          $('#' + related_form_id + ' input[name=image_key]').val resp_data.image_key
          $('#' + related_form_id + ' .image-preview').show().attr "src", resp_data.image_link

      $.ajax request_params

      return false

  init: () ->
    $(document).ready () ->
      market.init_upload_forms()

market.init()
window.market=market