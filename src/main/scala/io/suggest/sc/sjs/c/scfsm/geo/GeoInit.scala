package io.suggest.sc.sjs.c.scfsm.geo

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfsm.IFsmMsgCompanion
import io.suggest.sc.sjs.m.mgeo._
import org.scalajs.dom
import org.scalajs.dom.{PositionError, Position, PositionOptions}

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 15:56
 * Description: ScFsm-аддон с утилью для сборки состояний, связанных с геолокацией.
 */
trait GeoInit extends ScFsmStub {

  /** FsmState-аддно для запуска запроса геолокации у юзера во время инициализации состояния. */
  protected trait GeoAskStartT extends FsmState {

    /** Использовать ли GPS? */
    def isHighAccuracy: Boolean

    /** Сборщик положительных сигналов геолокации. */
    def geoLocCompanion: IFsmMsgCompanion[Position]
    /** Сборщик сигналов-ошибок геолокции. */
    def geoErrCompanion: IFsmMsgCompanion[PositionError]

    override def afterBecome(): Unit = {
      super.afterBecome()
      val wid = _watchPosition()
      val sd0 = _stateData
      _stateData = sd0.copy(
        geo = _saveGeoWid(wid, sd0.geo)
      )
    }

    /**
     * Создать ватчер позишенов.
     * @return id созданного ватчера.
     */
    protected def _watchPosition(): Int = {
      val opts = Dictionary[Any]()
        .asInstanceOf[PositionOptions]
      opts.enableHighAccuracy = isHighAccuracy
      dom.window.navigator.geolocation.watchPosition(
        _signalCallbackF( geoLocCompanion ),
        _signalCallbackF( geoErrCompanion ),
        opts
      )
    }

    /** Сохранение нового watchId в данные состояния геолокации. */
    protected def _saveGeoWid(wid: Int, gsd0: MGeoLocSd): MGeoLocSd

  }


  /** Трейт с запуском геолокации по BSS, без GPS. */
  protected trait BssGeoAskStartT extends GeoAskStartT {
    override def isHighAccuracy = false
    override def geoLocCompanion = BssGeoLocSignal
    override def geoErrCompanion = BssGeoErrorSignal
    override protected def _saveGeoWid(wid: Int, gsd0: MGeoLocSd): MGeoLocSd = {
      gsd0.copy(
        bssWid = Some(wid)
      )
    }
  }


  /** Аддон для состояний, реагирующих на получение данных геолокации. */
  protected trait GeoWaitStateT extends FsmState {

    // TODO Первая геолокация часто совсем не точная (accuracy > километра). Бывает нужно подождать данных по-точнее.

    /** Реакция на получение данных геолокации: нужно перейти в состояние ожидания готовности js-роутера. */
    override def _geoLocReceived(gs: IGeoLocSignal): Unit = {
      super._geoLocReceived(gs)
      val sd0 = _stateData
      val sd1 = sd0.copy(
        geo = sd0.geo.copy(
          lastBssPos = Option( gs.data )
        )
      )
      become(_geoReadyState, sd1)
    }

    /** Реакция на получение ошибки получения геолокация. Нужно переключиться на роутер. */
    override def _geoLocErrorReceived(ge: IGeoErrorSignal): Unit = {
      super._geoLocErrorReceived(ge)
      become(_geoFailedState)
    }

    /** Следующее состояние при получении успешной геолокации. */
    protected def _geoReadyState: FsmState
    /** Состояние при получении ошибки геолокации. */
    protected def _geoFailedState: FsmState

  }

}
