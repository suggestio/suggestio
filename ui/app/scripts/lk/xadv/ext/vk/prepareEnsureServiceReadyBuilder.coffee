define ["PrepareEnsureServiceReadyBuilder"], (PrepareEnsureServiceReadyBuilder) ->

  class VkPrepareEnsureServiceReadyBuilder extends PrepareEnsureServiceReadyBuilder
    API_ID = 4705589
    ACESS_LVL = 8197

    authorization: (onSuccess, onError) ->

      authInfo = (response) =>
        if response.session
          @ctx.user_id = response.session.mid
          @createAdapter onSuccess, onError
        else
          VK.Auth.login authInfo, ACESS_LVL
          onError @ws, "auth error"

      VK.Auth.getLoginStatus authInfo

    createAdapter: (onSuccess, onError) ->
      requirejs(
        [@name, "SioPR"]
        (VkAdapter, SioPR) =>
          adapter = new VkAdapter(@ws)
          SioPR = new SioPR()

          SioPR.registerService @name, adapter
          onSuccess @ws, @ctx
      )

    execute: (onSuccess, onError) ->
      console.log "Vk PrepareEnsureServiceReadyBuilder execute"

      VK.init
        apiId: API_ID

      @authorization onSuccess, onError
