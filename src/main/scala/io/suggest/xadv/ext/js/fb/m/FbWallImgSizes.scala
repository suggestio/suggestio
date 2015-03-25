package io.suggest.xadv.ext.js.fb.m

import io.suggest.adv.ext.model.im.FbWallImgSizesBaseT
import io.suggest.xadv.ext.js.runner.m.IMSize2D

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 16:29
 * Description: Модель размеров wall-иллюстраций на стороне JS.
 */
object FbWallImgSizes extends FbWallImgSizesBaseT {

  /** Абстрактный экземпляр модели. */
  trait ValT extends super.ValT with IMSize2D

  override type T = ValT

  override val Community: T   = new ValT with CommunityValT
  override val User: T        = new ValT with UserValT

}
