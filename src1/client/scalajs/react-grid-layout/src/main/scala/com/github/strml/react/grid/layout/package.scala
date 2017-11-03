package com.github.strml.react.grid

import io.suggest.common.html.HtmlConstants
import japgolly.scalajs.react.vdom.Attr.ValueType
import japgolly.scalajs.react.vdom.Attr.ValueType.Simple
import japgolly.scalajs.react.vdom.HtmlAttrs
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 17:27
  */
package object layout {

  type CompactType_t = String

  type LayoutItem = js.Object

  /** (layout: Layout, oldItem: LayoutItem, newItem: LayoutItem,
    *  placeholder: LayoutItem, e: MouseEvent, element: HTMLElement) => void;
    */
  type ItemCallback = js.Function6[Layout, LayoutItem, LayoutItem, LayoutItem, MouseEvent, HTMLElement, _]


  implicit val vdomAttrVtJsObject: Simple[Layout] = ValueType.direct

  val dataGridAttr = VdomAttr[Layout](HtmlConstants.ATTR_PREFIX + "grid")

  implicit class VdomAttrsExt( private val htmlAttrs: HtmlAttrs ) extends AnyVal {

    def dataGrid = dataGridAttr

  }

}
