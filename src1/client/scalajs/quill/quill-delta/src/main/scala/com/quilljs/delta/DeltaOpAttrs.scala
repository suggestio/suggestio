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

  /** 1..6 */
  var header: UndefOr[Int | Null] = js.undefined

  var src: UndefOr[String] = js.undefined

  var list: UndefOr[String] = js.undefined

  // undefined ==> 0, 0..8
  var indent: UndefOr[Int | Null] = js.undefined

  var `code-block`: UndefOr[Boolean | Null] = js.undefined

  var blockquote: UndefOr[Boolean | Null] = js.undefined

  var font: UndefOr[String] = js.undefined

  // String вида "46", т.е. строковое представление кегля.
  var size: UndefOr[String] = js.undefined

  var script: UndefOr[String] = js.undefined

  var align: UndefOr[String] = js.undefined

  // С width и height есть особенности: это размеры картинки (или иного визуального 2D-объекта).
  // Официально, image resize не поддерживается. https://github.com/quilljs/quill/issues/753
  // Но они работают в Firefox! Единственное, что это сырые аттрибуты, т.е. строки вида "543".
  // Мы исходим из того, что тип данных могут внезапно заменить на int, и ПОКА поддерживаем одновременно оба варианта.
  var width: UndefOr[String | Int] = js.undefined

  var height: UndefOr[String | Int] = js.undefined

}
