// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
  'use strict';

  // -- RequireJS config --
  requirejs.config({
    // Packages = top-level folders; loads a contained file named 'main.js"
    packages: ['umap']
    //,shim: {}
  });

  requirejs.onError = function (err) {
    console.log(err);
  };

})(requirejs);

