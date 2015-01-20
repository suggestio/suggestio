define ["iAdapter", "VkPublishMessageBuilder", "VkPictureStorageBuilder", "VkPutPictureBuilder"], (IAdapter, VkPublishMessageBuilder, VkPictureStorageBuilder, VkPutPictureBuilder) ->

  class VkAdapter extends IAdapter
    API_ID = 4705589
    ACESS_LVL = 8197

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

    preparePublishMessage: (ctx = new Object()) ->
      ctx.user_id = userId
      return new VkPublishMessageBuilder(@ws, ctx)

    preparePictureStorage: (ctx = new Object()) ->
      ctx.userId = userId
      return new VkPictureStorageBuilder(@ws, ctx)

    preparePutPicture: (ctx = new Object()) ->
      ctx.userId = userId
      return new VkPutPictureBuilder(@ws, ctx)


  return VkAdapter