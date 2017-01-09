package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.{IMExtTarget, IMExtTargetWrapper}

import scala.scalajs.js.Any

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.03.15 18:13
 * Description: Модель-враппер над общей IMExtTarget.
 */

object FbExtTarget {
  def fromTarget(tg: IMExtTarget): FbExtTarget = {
    FbExtTarget(
      tgUnderlying   = tg,
      fbTgUnderlying = FbTarget.fromJson( tg.customRaw.get )
    )
  }
}


trait FbExtTargetT extends IMExtTargetWrapper with FbTargetWrapperT {
  override def customRaw: Option[Any] = {
    Some(fbTgUnderlying.toJson)
  }
}

/**
 * Враппер над MExtTarget для контекста. Имитирует поведение обеих моделей одновременно.
 * @param tgUnderlying Исходный экземпляр класса.
 * @param fbTgUnderlying facebook-only данные по цели.
 */
case class FbExtTarget(tgUnderlying: IMExtTarget, fbTgUnderlying: FbTarget)
  extends FbExtTargetT


