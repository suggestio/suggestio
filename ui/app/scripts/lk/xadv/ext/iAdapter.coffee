define [], () ->

  class IAdapter

    constructor: (@ws) ->

    preparePublishMessage: (ctx) ->
      return new IPublishMessageBuilder(@ws, ctx)

    preparePictureStorage: (ctx) ->
      return new PictureStorageBuilder(@ws, ctx)

    preparePutPicture: (ctx) ->
      return new PutPictureBuilder(@ws, ctx)