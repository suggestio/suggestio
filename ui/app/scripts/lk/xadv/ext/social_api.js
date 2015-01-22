define([], function() {

  requirejs.config({

    baseUrl: "/vassets/scripts/lk/xadv/ext/",

    paths: {
      SioPR: "main",
      IAdapter: "iAdapter",
      PictureStorageBuilder: "pictureStorageBuilder",
      IPublishMessageBuilder: "iPublishMessageBuilder",
      PutPictureBuilder: "putPictureBuilder",
      PrepareEnsureServiceReadyBuilder: "prepareEnsureServiceReadyBuilder",

      vk: "vk/adapter",
      VkPictureStorageBuilder: "vk/pictureStorageBuilder",
      VkPublishMessageBuilder: "vk/publishMessageBuilder",
      VkPrepareEnsureServiceReadyBuilder: "vk/prepareEnsureServiceReadyBuilder",
      VkPutPictureBuilder: "vk/putPictureBuilder",

      fb: "fb/adapter",
      FbPrepareEnsureServiceReadyBuilder: "fb/prepareEnsureServiceReadyBuilder",
      FbPublishMessageBuilder: "fb/publishMessageBuilder"
    }

  });

  require(["SioPR"], function(SioPR) {
    $input = $("#socialApiConnection");
    url = $input.val()

    ws = new WebSocket(url);

    SioPR = new SioPR();
    SioPR.setWs(ws);

    SioPR.prepareEnsureServiceReady("fb",{}).execute();

    ws.onmessage = function(event) {
      message = $.parseJSON(event.data);
      if(message["type"] == "js") {
        console.log(message);
        eval(message["data"]);
      }
    }

  });

});