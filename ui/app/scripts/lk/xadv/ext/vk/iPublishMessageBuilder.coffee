define ["IPublishMessageBuilder"], (IPublishMessageBuilder) ->

  class VkPublishMessageBuilder extends IPublishMessageBuilder
    url = null
    text = null

    setUrl: (_url) ->
      url = _url
      return @

    setText: (_text) ->
      text = _text
      return @

    saveWallPhoto: () ->

      savedPhoto = JSON.parse(@ctx._picture.saved)

      callback = (data) =>
        attachments = new Array()
        attachments.push data.response[0].id
        #attachments.push @ctx._target.href
        #TODO убрать статичную ссылку
        attachments.push "http://localhost"
        attachments = attachments.join ","

        post =
          attachments: attachments

        @wallPost(post)

      params =
        user_id: @ctx.user_id
        server: savedPhoto.server
        photo: savedPhoto.photo
        hash: savedPhoto.hash

      VK.Api.call "photos.saveWallPhoto", params, callback

    wallPost: (post) ->

      if text?
        post.message = text

      callback = (data) ->
        console.log data

      VK.Api.call "wall.post", post, callback

    execute: (onSuccess, onError) ->

      @saveWallPhoto()