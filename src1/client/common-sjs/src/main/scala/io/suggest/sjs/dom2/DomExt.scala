package io.suggest.sjs.dom2

import org.scalajs.dom

import scala.util.Try

object DomExt {

  // js.global scope probing does not work properly. So, we using Try() here.
  val windowOpt = Try( dom.window ).toOption

}
