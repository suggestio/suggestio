package io.suggest.sc.sjs.c.scfsm

import io.suggest.geo.MLocEnv
import io.suggest.sc.sjs.m.mgrid.MFindGridAdsArgsLimitOffsetT
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.m.msrv.tile.MFindAdsReqDflt

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.07.16 17:56
  * Description: Вспомогательные модели аргументов запроса карточек.
  * Никакой особой ценности не представляют, их можно заинлайнить в тех немногих места, где они используются.
  */

trait FindAdsArgsT extends ScFsmStub {

  /**
    * compile-time флаг для разрешения/запрещания сокрытия карточек из маячков,
    * при нахождении внутри ресиверов.
    */
  private def HIDE_BEACONS_ON_RECEIVERS = false


  /** Заполнить аргументы поиска карточек на основе данных состояния [[MScSd]] от ScFsm. */
  trait MFindAdsArgsT extends MFindAdsReqDflt {

    /** Текущее состояние ScFsm. */
    def _sd: MScSd = _stateData

    override def screenInfo = Some(_sd.common.screen)

    override def generation = Some(_sd.common.generation)

    override def receiverId = _sd.common.adnIdOpt

    override def locEnv: MLocEnv = {
      // 2016.nov.10: Маячковые карточки остаются висеть после перехода на какой-то узел в выдаче.
      // Нужно устранить этот косяк путём отказа от loc env при наличии adnId.
      if (HIDE_BEACONS_ON_RECEIVERS && _sd.common.adnIdOpt.nonEmpty) {
        MLocEnv.empty
      } else {
        _sd.locEnv
      }

    }

    override def tagNodeId = _sd.common.tagOpt.map(_.nodeId)

  }


  /** Расширенный вариант [[MFindAdsArgsT]] с заполнением значений limit и offset. */
  trait MFindAdsArgsLimOffT extends MFindAdsArgsT with MFindGridAdsArgsLimitOffsetT {
    override def _mgs = _sd.grid.state
  }

  case class MFindAdsArgsLimOff(override val _sd: MScSd = _stateData)
    extends MFindAdsArgsLimOffT

}
