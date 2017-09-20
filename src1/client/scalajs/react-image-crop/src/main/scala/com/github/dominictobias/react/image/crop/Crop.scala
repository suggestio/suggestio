package com.github.dominictobias.react.image.crop

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 15:33
  * Description: Crop data model.
  */
sealed trait AbstractCrop[V <: AnyVal] extends js.Object {

  val x: js.UndefOr[V] = js.undefined

  val y: js.UndefOr[V] = js.undefined

  val width: js.UndefOr[V] = js.undefined

  val height: js.UndefOr[V] = js.undefined

}


trait PercentCrop extends AbstractCrop[Percentage_t]

trait PixelCrop extends AbstractCrop[Int]
