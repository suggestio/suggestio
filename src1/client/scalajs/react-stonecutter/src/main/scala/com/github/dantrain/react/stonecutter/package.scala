package com.github.dantrain.react

import japgolly.scalajs.react.vdom.HtmlAttrs
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 16:10
  */
package object stonecutter {

  /** Original js-project name (same as in npmDependencies). */
  final val REACT_STONECUTTER = "react-stonecutter"


  type GridComponent_t = String

  type LayoutF_t = js.Function2[js.Array[ItemProps], PropsCommon, LayoutFunRes]

  type EnterExitF_t = js.Function3[ItemProps, PropsCommon, GridState, js.Object]


  /** item height attr for item container tag. */
  val itemHeightAttr = VdomAttr[Int]("itemHeight")

  implicit class VdomAttrsExt( private val htmlAttrs: HtmlAttrs ) extends AnyVal {

    def itemHeight = itemHeightAttr

  }

}
