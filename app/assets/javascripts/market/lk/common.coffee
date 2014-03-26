market =
  init_images_upload : () ->

    $('.w-async-image-upload').bind "change", () ->

      relatedFieldId = $(this).attr "data-related-field-id"
      form_data = new FormData()

      if $(this)[0].type == 'file'
        form_data.append $(this)[0].name, $(this)[0].files[0]

      request_params =
        url : $(this).attr "data-action"
        method : 'post'
        data : form_data
        contentType: false
        processData: false
        success : ( resp_data ) ->
          $('#' + relatedFieldId + ' .image-key').val(resp_data.image_key).trigger('change')
          $('#' + relatedFieldId + ' .image-preview').show().attr "src", resp_data.image_link

      $.ajax request_params

      return false

  init: () ->
    $(document).ready () ->
      market.init_images_upload()

market.init()
window.market=market