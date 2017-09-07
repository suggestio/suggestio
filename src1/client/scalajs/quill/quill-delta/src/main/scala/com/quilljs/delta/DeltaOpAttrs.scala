package com.quilljs.delta

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:16
  * Description: Delta operation attributes.
  */

@js.native
trait DeltaOpAttrs extends js.Object {

  // TODO У всех есть значение null, которая в diff-режиме обозначает снятие аттрибута. А в insert-режиме не встречается.

  var bold: UndefOr[Boolean | Null] = js.undefined

  var italic: UndefOr[Boolean | Null] = js.undefined

  var underline: UndefOr[Boolean | Null] = js.undefined

  var strike: UndefOr[Boolean | Null] = js.undefined

  /** #rrggbb or #rgb or ... */
  var color: UndefOr[String] = js.undefined

  var background: UndefOr[String] = js.undefined

  var link: UndefOr[String] = js.undefined

  var header: UndefOr[Int | Null] = js.undefined

  var src: UndefOr[String] = js.undefined

  var list: UndefOr[String] = js.undefined

  var indent: UndefOr[Int | Null] = js.undefined

  var `code-block`: UndefOr[Boolean | Null] = js.undefined

  var blockquote: UndefOr[Boolean | Null] = js.undefined

  var font: UndefOr[String] = js.undefined

  // String вида "46", т.е. строковое представление кегля.
  var size: UndefOr[String] = js.undefined

  var script: UndefOr[String] = js.undefined

  var align: UndefOr[String] = js.undefined

}
