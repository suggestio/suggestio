define([], function() {

  requirejs.config({

      baseUrl: "/ui/scripts/lk/adv/ext/compiled/",

      paths: {
        "main": "main"
      }
  });

  require(["main"], function(Main) {
    Main.init();
  });

});