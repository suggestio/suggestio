define [], () ->

  class SioPR
    instance = undefined
    ws = null
    serviceList = new Array()

    constructor: ->
      if instance?
        return instance
      else instance = @

    @setWs: (_ws) ->
      ws = _ws

    @prepareEnsureReady: () ->
      return new PrepareEnsureReadyBuilder(ws)

    @prepareEnsureServiceReady: (serviceName, ctx1) ->
      return new PrepareEnsureServiceReadyBuilder(ws, serviceName, ctx1)

    @setService: (name, adapter) ->
      serviceList[name] = adapter

    @service: (name) ->
      return serviceList[name]

  class PrepareEnsureReadyBuilder

    constructor: (@ws) ->

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureReadyBuilder execute"
      ctx1 = new Object()
      onSuccess @ws, ctx1

  class PrepareEnsureServiceReadyBuilder
    adapter = null

    constructor: (@ws, @name, @ctx1) ->
      adapter = new VkAdapter(@ws)
      SioPR.setService @name, adapter

    setServiceName: (name) ->
      @name = name
      return @

    execute: (onSuccess, onError) ->
      console.log "PrepareEnsureServiceReadyBuilder execute"
      ctx2 = new Object()
      #SioPR.service("vk").preparePublishMessageBuilder().setMessage("test from adapter").execute()
      SioPR.service("vk").preparePictureStorageBuilder().execute()
      onSuccess(@ws, ctx2)



  class IAdapter

    constructor: (@ws) ->

    @preparePublishMessageBuilder: (ctx0) ->
      return new IPublishMessageBuilder(@ws, ctx0)

    @preparePictureStorage: (ctx2) ->
      return new PictureStorageBuilder(@ws, ctx2)

  class VkAdapter extends IAdapter
    API_ID = 4705589
    USER_ID = null

    constructor: (@ws) ->
      VK.init
        apiId: API_ID

      @setUserId()

    setUserId: () ->

      authInfo = (response) ->
        if response.session
          USER_ID = response.session.mid
        else
          VK.Auth.login authInfo, ACESS_LVL

      VK.Auth.getLoginStatus authInfo

    preparePublishMessageBuilder: (ctx0) ->
      console.log "prepare adapter for msg"
      return new VkPublishMessageBuilder(@ws, ctx0)

    preparePictureStorageBuilder: (ctx = new Object()) ->
      ctx.userId = USER_ID
      return new VkPictureStorageBuilder(@ws, ctx)



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
        console.log "---"
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



  class IPublishMessageBuilder

    constructor: (@ws, @ctx0) ->

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
      onSuccess = () ->
        console.log "success call for post message"

      params =
        message: message

      VK.Api.call "wall.post", params, onSuccess



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

