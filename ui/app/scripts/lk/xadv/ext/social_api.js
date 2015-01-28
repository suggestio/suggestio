define([], function() {

  requirejs.config({

    baseUrl: "/vassets/scripts/lk/xadv/ext/",

    paths: {
      SioPR: "main",
      View: "view",

      vk: "vk/vk",
      facebook: "fb/facebook",
    }

  });

  require(["SioPR"], function(SioPR) {
    $input = $("#socialApiConnection");
    url = $input.val()       

    ws = new WebSocket(url);

    SioPR = new SioPR();
    SioPR.setWs(ws);

    //SioPR.prepareEnsureServiceReady("fb",{}).execute();

    ws.onmessage = function(event) {
      message = $.parseJSON(event.data);
      if(message["type"] == "js") {
        console.log(message);
        eval(message["data"]);
      }
    }

  });

});