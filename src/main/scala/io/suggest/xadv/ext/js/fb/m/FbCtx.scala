package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:24
 * Description: Контекст Facebook-адаптера. Передается при необходимости в mctx.custom.
 */

object FbCtx extends FromJsonT {

  override type T = FbCtx

  /** Имя сериализованного поля c инфой по таргету. */
  def FB_TG_FN = "g"

  /** Десериализация контекста из JSON. */
  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]
    FbCtx(
      fbTg = FbTarget fromJson d(FB_TG_FN)
    )
  }
}


import FbCtx._


case class FbCtx(
  fbTg: FbTarget
) extends IToJsonDict {

  /** Сериализация экземпляра контекста в JSON. */
  override def toJson: Dictionary[Any] = {
    Dictionary[Any](
      FB_TG_FN -> fbTg.toJson
    )
  }

}
