define([], function() {

  requirejs.config({

      baseUrl: "/vassets/scripts/lk/xadv/ext/",

      paths: {
        "SioPR": "main",
        "IAdapter": "iAdapter",
        "PictureStorageBuilder": "pictureStorageBuilder",
        "IPublishMessageBuilder": "iPublishMessageBuilder",
        "PutPictureBuilder": "putPictureBuilder",
        "vk": "vk/adapter",
        "VkPictureStorageBuilder": "vk/pictureStorageBuilder",
        "VkPublishMessageBuilder": "vk/iPublishMessageBuilder",
        "VkPutPictureBuilder": "vk/putPictureBuilder"
      }

  });

  require(["SioPR"], function(SioPR) {
    $input = $("#socialApiConnection");
    url = $input.val()

    ws = new WebSocket(url);

    SioPR = new SioPR();
    SioPR.setWs(ws);

    ws.onmessage = function(event) {
      message = $.parseJSON(event.data);
      if(message["type"] == "js") {
        console.log(message);
        eval(message["data"]);
      }
    }
  });

});