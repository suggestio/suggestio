package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.Size2dCtx._

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 14:14
 * Description: Модель для представления двумерных размеров.
 */

object MSize2D extends FromStringT {

  override type T = MSize2D

  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[Int]] : WrappedDictionary[Int]
    MSize2D(width = d(WIDTH_FN), height = d(HEIGHT_FN))
  }
}

case class MSize2D(width: Int, height: Int) extends IToJsonDict {
  override def toJson = js.Dictionary[js.Any](
    WIDTH_FN  -> width,
    HEIGHT_FN -> height
  )
}

