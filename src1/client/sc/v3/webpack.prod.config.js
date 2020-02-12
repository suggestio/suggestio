var webpack = require('webpack');
//var UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = require('./scalajs.webpack.config');

module.exports.plugins = (module.exports.plugins || []).concat([
  new webpack.DefinePlugin({
    'process.env.NODE_ENV': JSON.stringify('production')
  })//,
  //new UglifyJsPlugin({ sourceMap: module.exports.devtool === 'source-map' })
]);
