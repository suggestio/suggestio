var webpack = require('webpack');

module.exports = require('./scalajs.webpack.config');

// Target is hard-coded/fixed to default "web", in contrast with showcase-ssr with another explicit target.
module.exports.target = "web";

module.exports.plugins = (module.exports.plugins || []).concat([
  new webpack.DefinePlugin({
    'process.env.NODE_ENV': JSON.stringify('production')
  })
]);
