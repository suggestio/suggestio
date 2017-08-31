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

  /** #rrggbb or #rgb or ... */
  var color: UndefOr[String] = js.undefined

  var link: UndefOr[String] = js.undefined

  var header: UndefOr[Int | Null] = js.undefined

  var src: UndefOr[String] = js.undefined

  //var alt: UndefOr[String] = js.undefined

}
