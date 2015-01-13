define([], function() {

  requirejs.config({

      baseUrl: "/vassets/scripts/lk/adv/ext/",

      paths: {
        "main": "main"
      }

  });

  require(["main"], function(Main) {
    Main.init();
  });

});