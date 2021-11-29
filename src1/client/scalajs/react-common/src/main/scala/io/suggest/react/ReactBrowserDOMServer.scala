package io.suggest.react

import japgolly.scalajs.react.raw

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport


/** Explicitly extract browser version, because node-version depends on nodejs's require("stream"),
  * Stream extension is not available in default GraalVM JSContext environment.
  */
//@JSImport("react-dom/cjs/react-dom-server.browser.production.min.js", JSImport.Namespace)
// TODO .production.min.js throws error around react.Context: japgolly.sjs-react.Context.rawValue returns undefined o_O even if Context's internal value looks visible in JS-debugger.
@JSImport("react-dom/cjs/react-dom-server.browser.development.js", JSImport.Namespace)
@js.native
object ReactBrowserDOMServer extends raw.ReactDOMServer

