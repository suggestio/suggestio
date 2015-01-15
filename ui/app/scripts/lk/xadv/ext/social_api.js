define([], function() {

  requirejs.config({

      baseUrl: "/vassets/scripts/lk/adv/ext/",

      paths: {
        "main": "main",
        "vk": "vk"
      }

  });

  require(["main"], function(Main) {
    Main.init();
  });

});