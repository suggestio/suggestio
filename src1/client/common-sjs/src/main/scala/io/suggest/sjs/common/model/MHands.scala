package io.suggest.sjs.common.model

import io.suggest.common.MHandsLightT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 15:55
 * Description: Легковесная модель направлений лево-право для scala.js.
 */
object MHands extends MHandsLightT {

  /** Класс экземпляров модели. */
  protected abstract class Val extends ValT

  override type T = Val

  override val Left: T = new Val with LeftT
  override val Right: T = new Val with RightT

}
