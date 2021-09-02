package io.suggest.sjs.dom2

import scala.scalajs.js

@js.native
trait XmlHttpRequest2 extends js.Object {

  /** Undefined variant of HTTP status.
    * Used for cordova-plugin-wkwebview-file-xhr file:/// schema interceptor.
    */
  def statusU: js.UndefOr[Int] = js.native

}

object XmlHttpRequest2 {

  /** cordova-plugin-wkwebview-file-xhr misses static constants. So, they are redefined here... */
  final def UNSENT = 0
  final def OPENED = 1
  final def HEADERS_RECEIVED = 2
  final def LOADING = 3
  final def DONE = 4

}
