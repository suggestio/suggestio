package io.suggest.sc.sjs.m.mmap

import io.suggest.sjs.common.fsm.IFsmMsg
import io.suggest.sjs.common.model.loc.IGeoLocMin

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 22:20
  * Description: Сигналы для MapFsm.
  */

/** Интерфейс сообщений для карты. */
trait IMapFsmMsg extends IFsmMsg


/** Сигнал инициализации карты. */
case class EnsureMap()
  extends IMapFsmMsg


/** Сигнал о принудительном выставлении карты на новую позицию. */
case class SetGeoLoc(mgl: IGeoLocMin)
  extends IMapFsmMsg
