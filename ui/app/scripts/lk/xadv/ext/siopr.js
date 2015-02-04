define([], function() {

  requirejs.config({

    baseUrl: "/vassets/scripts/lk/xadv/ext/",

    paths: {
      SioPR: "main",
      view: "view",

      vk: "vk/vk",
      facebook: "fb/facebook",
      twitter: "twitter/twitter",
    }

  });

  require(["SioPR", "view"], function(SioPR, View) {
    $input = $("#socialApiConnection");
    url = $input.val()       

    if(url) {
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
    }

  });

});