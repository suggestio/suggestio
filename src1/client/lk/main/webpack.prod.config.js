var webpack = require('webpack');
var UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = require('./scalajs.webpack.config');

module.exports.plugins = (module.exports.plugins || []).concat([
  new webpack.DefinePlugin({
    'process.env.NODE_ENV': JSON.stringify('production')
  })
  // TODO [info] ERROR in lk-sjs-opt-library.js from UglifyJs
  // [info] Invalid assignment [./node_modules/source-map-loader!./node_modules/quill-delta/lib/delta.js:119,0][lk-sjs-opt-library.js:800
  //new UglifyJsPlugin({ sourceMap: module.exports.devtool === 'source-map' })
]);
