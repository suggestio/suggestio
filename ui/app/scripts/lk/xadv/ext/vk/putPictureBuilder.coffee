define ["putPictureBuilder"], (PutPictureBuilder) ->

  class VkPutPictureBuilder extends PutPictureBuilder

    execute: (onSucess, onError) ->
      console.log "vk put picture execute"
      params =
        user_id: @ctx.userId

      callback = (data) ->
        console.log data

        if data.error && onError?
          onError data.error

      VK.Api.call "photos.saveWallPhoto", params, callback
