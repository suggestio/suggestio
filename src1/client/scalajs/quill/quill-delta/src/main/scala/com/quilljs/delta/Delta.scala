package com.quilljs.delta

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 16:07
  * Description: Quill delta format spec.
  */
@ScalaJSDefined
trait Delta extends js.Object {

  val ops: js.Array[DeltaOp]

  // TODO insert() и т.д.

}


@ScalaJSDefined
trait DeltaOp extends js.Object {

  val insert: UndefOr[js.Any] = js.undefined

  val attributes: UndefOr[DeltaOpAttributes] = js.undefined

  val delete: UndefOr[Int] = js.undefined

  val retain: UndefOr[Int] = js.undefined

}


@ScalaJSDefined
trait DeltaOpAttributes extends js.Object {

  val bold: UndefOr[Boolean] = js.undefined

  val italic: UndefOr[Boolean] = js.undefined

  /** #rrggbb or #rgb or ... */
  val color: UndefOr[String] = js.undefined

  val link: UndefOr[String] = js.undefined

  val header: UndefOr[Int] = js.undefined

}
