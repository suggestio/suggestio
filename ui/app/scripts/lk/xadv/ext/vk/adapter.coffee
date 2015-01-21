define ["iAdapter", "VkPublishMessageBuilder", "VkPictureStorageBuilder", "VkPutPictureBuilder"], (IAdapter, VkPublishMessageBuilder, VkPictureStorageBuilder, VkPutPictureBuilder) ->

  class VkAdapter extends IAdapter

    constructor: (@ws) ->

    preparePublishMessage: (ctx) ->
      return new VkPublishMessageBuilder(@ws, ctx)

    preparePictureStorage: (ctx) ->
      return new VkPictureStorageBuilder(@ws, ctx)

    preparePutPicture: (ctx) ->
      return new VkPutPictureBuilder(@ws, ctx)


  return VkAdapter