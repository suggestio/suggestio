package io.suggest.react

import japgolly.scalajs.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport


/** Explicitly extract browser version, because node-version depends on nodejs's require("stream"),
  * Stream extension is not available in default GraalVM JSContext environment.
  */
@JSImport("react-dom/server.browser", JSImport.Namespace)
@js.native
object ReactBrowserDOMServer extends react.raw.ReactDOMServer

