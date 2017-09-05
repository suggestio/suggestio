package com.quilljs.quill.modules.formats

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 13:48
  * Description: Quill list format APIs.
  */
object QfList {

  final val LIST = "list"

  def ORDERED = "ordered"

  def BULLET = "bullet"

}


/** List toolbar btn. */
trait QfListTb extends js.Object {

  @JSName( QfList.LIST )
  val list: String

}
