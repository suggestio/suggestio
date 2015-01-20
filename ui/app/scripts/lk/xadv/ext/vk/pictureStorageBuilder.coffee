define ["pictureStorageBuilder"], (PictureStorageBuilder) ->

  class VkPictureStorageBuilder extends PictureStorageBuilder

    execute: (onSuccess, onError) ->
      console.log "vk picture storage execute"

      params =
        user_id: @ctx.userId

      callback = (data) =>
        if data.error
          onError data.error

        @ctx =
          _picture:
            size:
              width: 600
              height: 500
            upload:
              mode: "s2s"
              url: data.response.upload_url
              partName: "photo"

        console.log @ctx
        onSuccess @ws, @ctx

      VK.Api.call "photos.getWallUploadServer", params, callback



