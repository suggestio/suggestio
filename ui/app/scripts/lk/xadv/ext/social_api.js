define([], function() {

  requirejs.config({

      baseUrl: "/vassets/scripts/lk/xadv/ext/",

      paths: {
        "main": "main",
        "vk": "vk"
      }

  });

  require(["main"], function(Main) {
    Main.init();
  });

});