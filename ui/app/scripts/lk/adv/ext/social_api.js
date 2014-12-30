define([], function() {

  requirejs.config({

      baseUrl: "/ui/dist/scripts/social_api",

      paths: {
        "main": "main",
        "vk": "main"
      }
  });

  require(["main"], function(main) {
    console.log("require module");
    main.init();
  });

});