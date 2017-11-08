package io.suggest.grid

import japgolly.scalajs.react.vdom.Attr.ValueType
import japgolly.scalajs.react.vdom.Attr.ValueType.Simple
import japgolly.scalajs.react.vdom.HtmlAttrs
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 14:56
  */
package object react {

  implicit val vdomAttrBlockMetaVT: Simple[MItemPropsExtData] = ValueType.direct

  final val itemAttrsExtAttrName = "dataIae"

  /** item height attr for item container tag. */
  val itemAttrsExtAttr = VdomAttr[MItemPropsExtData]( itemAttrsExtAttrName )

  implicit class VdomGridAttrsExt( private val htmlAttrs: HtmlAttrs ) extends AnyVal {

    def itemAttrsExt = itemAttrsExtAttr

  }

}
