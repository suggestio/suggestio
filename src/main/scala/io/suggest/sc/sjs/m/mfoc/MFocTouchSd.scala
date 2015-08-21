package io.suggest.sc.sjs.m.mfoc

import io.suggest.common.geom.Coord2dD

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 11:13
 * Description: Данные состояния свайпа при touch-навигации в focused-выдачи.
 */
case class MFocTouchSd(
  start : Coord2dD,
  lastX : Double,
  mode  : Option[MFocTouchMode] = None
)
