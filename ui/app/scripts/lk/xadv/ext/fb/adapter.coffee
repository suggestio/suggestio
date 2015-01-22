define ["iAdapter", "FbPublishMessageBuilder"], (IAdapter, FbPublishMessageBuilder) ->

  class FbAdapter extends IAdapter

    constructor: (@ws) ->

    preparePublishMessage: (ctx) ->
      return new FbPublishMessageBuilder(@ws, ctx)


  return FbAdapter