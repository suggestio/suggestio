define([], function() {

  requirejs.config({
    urlArgs: "bust=" + (new Date()).getTime(),
    "baseUrl": "/assets/javascripts/",
    "paths": {
      "router": "bower_components/requirejs-router/router",
      "dom": "sc/utilites/dom"
    }
  });

  requirejs.onError = function (err) {
    console.log(err.requireType);
    console.log('modules: ' + err.requireModules);
    throw err;
  };
  
  // Load the router
  require(["router"], function(router) {
    router
      .registerRoutes({
        home: {path: "/", moduleId: "dom"}
      })
      .on("routeload", function(module, routeArguments) {
        module.init();
      })
      .init();
  });

});
  