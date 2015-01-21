define ["IPublishMessageBuilder"], (IPublishMessageBuilder) ->

  class VkPublishMessageBuilder extends IPublishMessageBuilder
    post = new Object()

    setText: (text) ->
      post.message = text
      return @

    setOwnerId: () ->
      REGEXP = /// /(?!.+/)(.+)$ ///
      url = @ctx._target.url

      match = url.match REGEXP

      # TODO добавить обработку ошибок если match[1] undefined
      params =
        screen_name: match[1]

      callback = (data) =>
        post.owner_id = data.response.object_id
        @wallPost()

      VK.Api.call "utils.resolveScreenName", params, callback

    saveWallPhoto: () ->

      savedPhoto = JSON.parse(@ctx._picture.saved)

      callback = (data) =>
        attachments = new Array()
        attachments.push data.response[0].id
        attachments.push @ctx._target.href
        attachments = attachments.join ","

        post.attachments = attachments

        @setOwnerId()

      params =
        user_id: @ctx.user_id
        server: savedPhoto.server
        photo: savedPhoto.photo
        hash: savedPhoto.hash

      VK.Api.call "photos.saveWallPhoto", params, callback

    wallPost: () ->

      callback = (data) ->
        console.log data

      VK.Api.call "wall.post", post, callback

    execute: (onSuccess, onError) ->

      @saveWallPhoto()