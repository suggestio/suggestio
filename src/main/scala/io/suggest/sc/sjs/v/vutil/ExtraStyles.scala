package io.suggest.sc.sjs.v.vutil

import org.scalajs.dom.raw.CSSStyleDeclaration

import scala.scalajs.js
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 13:35
 * Description: Поддержка не очень стандартных свойств типа will-change и других.
 */
trait ExtraStylesStub extends js.Object {

  var willChange: String = js.native

}


trait ExtraStyles {
  
  implicit protected def style2xstyle(style: CSSStyleDeclaration): ExtraStylesStub = {
    style.asInstanceOf[ExtraStylesStub]
  }

}
