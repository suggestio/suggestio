define ["IPublishMessageBuilder"], (IPublishMessageBuilder) ->

  class VkPublishMessageBuilder extends IPublishMessageBuilder
    url = null
    message = null

    setUrl: (_url) ->
      url = _url
      return @

    setMessage: (_message) ->
      message = _message
      return @

    saveWallPhoto: () ->
      savedPhoto = JSON.parse(@ctx._picture.saved)

      callback = (data) =>
        post =
          attachments: data.response[0].id

        @wallPost(post)

      params =
        user_id: @ctx.user_id
        server: savedPhoto.server
        photo: savedPhoto.photo
        hash: savedPhoto.hash

      VK.Api.call "photos.saveWallPhoto", params, callback

    wallPost: (post) ->

      callback = (data) ->
        console.log data

      console.log post

      VK.Api.call "wall.post", post, callback

    execute: (onSuccess, onError) ->

      @saveWallPhoto()