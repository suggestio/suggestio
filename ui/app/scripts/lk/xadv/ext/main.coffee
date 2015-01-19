define [], () ->

  class SioPR
    instance = undefined
    ws = null
    serviceList = new Array()

    # singletone
    constructor: ->
      if instance?
        return instance
      else instance = @

    @setWs: (_ws) ->
      ws = _ws

    @prepareEnsureReady: (ctx) ->
      return new PrepareEnsureReadyBuilder(ws, ctx)

    @prepareEnsureServiceReady: (serviceName, ctx) ->
      return new PrepareEnsureServiceReadyBuilder(ws, serviceName, ctx)

    @setService: (name, adapter) ->
      serviceList[name] = adapter

    @service: (name) ->
      return serviceList[name]

  class PrepareEnsureReadyBuilder

    constructor: (@ws, @ctx) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureReadyBuilder execute"
      onSuccess @ws, @ctx

  class PrepareEnsureServiceReadyBuilder
    adapter = null

    constructor: (@ws, @name, @ctx) ->
      adapter = new VkAdapter(@ws)
      SioPR.setService @name, adapter

    setServiceName: (name) ->
      @name = name
      return @

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureServiceReadyBuilder execute"
      #SioPR.service("vk").preparePublishMessageBuilder().setMessage("test from adapter").execute()
      #SioPR.service("vk").preparePictureStorageBuilder().execute()
      #SioPR.service("vk").preparePutPicture().execute()
      console.log "---"
      console.log "context"
      console.log @ctx
      console.log "---"
      onSuccess @ws, @ctx



  class IAdapter

    constructor: (@ws) ->

    @preparePublishMessageBuilder: (ctx) ->
      return new IPublishMessageBuilder(@ws, ctx)

    @preparePictureStorage: (ctx) ->
      return new PictureStorageBuilder(@ws, ctx)

    @preparePutPicture: (ctx) ->
      return new PutPictureBuilder(@ws, ctx)

  class VkAdapter extends IAdapter
    API_ID = 4705589
    ACESS_LVL = 5

    userId = null

    constructor: (@ws) ->
      VK.init
        apiId: API_ID

      @setUserId()

    setUserId: () ->

      authInfo = (response) ->
        if response.session
          userId = response.session.mid
        else
          VK.Auth.login authInfo, ACESS_LVL

      VK.Auth.getLoginStatus authInfo

    preparePublishMessageBuilder: (ctx = new Object()) ->
      console.log "prepare adapter for msg"
      return new VkPublishMessageBuilder(@ws, ctx)

    preparePictureStorageBuilder: (ctx = new Object()) ->
      ctx.userId = userId
      return new VkPictureStorageBuilder(@ws, ctx)

    preparePutPicture: (ctx = new Object()) ->
      ctx.userId = userId
      return new VkPutPictureBuilder(@ws, ctx)



  class PictureStorageBuilder

    constructor: (@ws, @ctx) ->

    setName: (name) ->
      return true

    setDescription: (description) ->
      return true

    execute: () ->
      console.log "picture storage execute"

  class VkPictureStorageBuilder extends PictureStorageBuilder

    execute: (onSuccess, onError) ->
      console.log "vk picture storage execute"
      console.log @ctx

      params =
        group_id: @ctx.userId

      callback = (data) ->
        if data.error
          onError data.error

        console.log "---"
        console.log data
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
        onSuccess @ctx
        console.log "---"

      VK.Api.call "photos.getWallUploadServer", params, callback



  class PutPictureBuilder

    constructor: (@ctx) ->

    setPictureUrl: (url) ->
      return true

    setDescription: (description) ->
      return true

    execute: (onSucess, onError) ->
      onSuccess()

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



  class IPublishMessageBuilder

    constructor: (@ws, @ctx) ->

    setUrl: (url) ->
      return @

    execute: (onSuccess, onError) ->
      onError(ws, "execute() not implemented!")

  class VkPublishMessageBuilder extends IPublishMessageBuilder
    url = null
    message = null

    setUrl: (_url) ->
      url = _url
      return @

    setMessage: (_message) ->
      message = _message
      return @

    execute: (onSuccess, onError) ->

      callback = (data) ->
        if data.error
          onError data
        else
          onSuccess data

      params =
        message: message

      VK.Api.call "wall.post", params, callback



  init: () ->
    $input = $ "#socialApiConnection"
    url = $input.val()

    ws = new WebSocket url

    SioPR.setWs ws

    ws.onmessage = (event) =>
      message = $.parseJSON event.data
      console.log message
      if message["type"] == "js"
        eval message["data"]

