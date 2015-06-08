package io.suggest.sc.sjs.v.vutil

import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.view.vutil.OnClickT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 10:23
 * Description: Реализация common-sjs/OnClickT, который отвечает за упрощенное вешанье click-событий.
 */
trait OnClick extends OnClickT {

  override protected def isTouchLocked = MTouchLock()

}
