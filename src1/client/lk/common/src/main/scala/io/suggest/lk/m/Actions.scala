package io.suggest.lk.m

import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:35
  * Description: Diode-экшены для нужд lk-common-sjs.
  */

sealed trait ILkCommonAction extends DAction

/** Экшен закрытия попапа с инфой по узлу. */
case object NodeInfoPopupClose extends ILkCommonAction
