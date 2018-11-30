package com.github.gcl

import org.scalajs.dom.experimental.Response

import scala.scalajs.js
import scala.scalajs.js.{Promise, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.18 11:29
  */
package object swtoolbox {

  type RouterHandlerF = js.Function2[String, js.Dictionary[String], Promise[Response] | Response]

}
