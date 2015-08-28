package io.suggest.sc.sjs.c.scfsm.geo

import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.m.mfsm.IFsmMsgCompanion
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sc.sjs.m.msrv.index.{MNodeIndexTimestamped, MNodeIndex}
import io.suggest.sc.sjs.vm.SafeWnd
import io.suggest.sjs.common.msg.WarnMsgs
import org.scalajs.dom.{PositionError, Position, PositionOptions}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.scalajs.js.{Dictionary, Any}

// TODO Нужны аддоны для таймаута, для клика по div'у педалирования геолокации и можно собирать трейт гео-фазы.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 15:56
 * Description: ScFsm-аддон с утилью для сборки состояний, связанных с геолокацией.
 * Фаза init-геолокации спроектирована исходя из того, что первая геолокация часто очень не точная,
 * но оценку эту производит сервер, возвращая index-страницу. Бывает, что нужно подождать немного.
 */
trait GeoInit extends Index {

  trait IGeoFailed {
    /** Состояние при получении ошибки геолокации. */
    protected def _geoFailedState: FsmState
  }


  /** FsmState-аддно для запуска запроса геолокации у юзера во время инициализации состояния. */
  protected trait GeoAskStartT extends FsmState with IGeoFailed {

    /** Использовать ли GPS? */
    def isHighAccuracy: Boolean

    /** Сборщик положительных сигналов геолокации. */
    def geoLocCompanion: IFsmMsgCompanion[Position]
    /** Сборщик сигналов-ошибок геолокции. */
    def geoErrCompanion: IFsmMsgCompanion[PositionError]

    override def afterBecome(): Unit = {
      super.afterBecome()
      _watchPosition() match {
        case Some(wid) =>
          val sd0 = _stateData
          _stateData = sd0.copy(
            geo = _saveGeoWid(wid, sd0.geo)
          )
        case None =>
          become(_geoFailedState)
      }

    }

    /** Сборка настроек геопозиционирования. */
    protected def _posOptions: PositionOptions = {
      val opts = Dictionary[Any]()
        .asInstanceOf[PositionOptions]
      opts.enableHighAccuracy = isHighAccuracy
      opts
    }

    /**
     * Создать ватчер позишенов.
     * @return Some(id созданного ватчера).
     *         None, если watchPosition() не поддерживается.
     */
    protected def _watchPosition(): Option[Int] = {
      for {
        nav <- SafeWnd.navigator
        gl  <- nav.geolocation
      } yield {
        gl.watchPosition(
          _signalCallbackF( geoLocCompanion ),
          _signalCallbackF( geoErrCompanion ),
          _posOptions
        )
      }
    }

    /** Сохранение нового watchId в данные состояния геолокации. */
    protected def _saveGeoWid(wid: Int, gsd0: MGeoLocSd): MGeoLocSd

  }


  /** Трейт с запуском геолокации по BSS, без GPS. */
  trait BssGeoAskStartT extends GeoAskStartT {
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
  trait GeoWaitStateT extends FsmState with IGeoFailed {

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

  }


  /** Аддон для запуска получения sc node index и получения ответа назад с временной отметкой. */
  trait AskGeoIndex extends FsmState with GetIndexUtil {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      // Запустить асинхронный запрос индексной страницы.
      val fut = _getIndex(sd0)
      val timestamp = _sendFutResBackTimestamped(fut, MNodeIndexTimestamped)
      // Сохранить timestamp запуска запроса в данные состояния.
      _stateData = sd0.copy(
        geo = sd0.geo.copy(
          inxReqTstamp = Some(timestamp)
        )
      )
    }

  }


  /** Аддон для сборки состояний, ожидающий index во время геолокации. */
  trait GeoIndexWaitStateT extends FsmEmptyReceiverState with ProcessIndexReceivedUtil with HandleNodeIndex {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Реакция на получения ответа с опрежающего index-запроса сервера.
      case MNodeIndexTimestamped(tryRes, tstamp) =>
        val sd0 = _stateData
        // Подавляем возможный race conditions на параллельных index-запросах через timestamp начала запроса.
        // В состоянии лежит timestamp последего запроса в рамках этой фазы.
        if (sd0.geo.inxReqTstamp contains tstamp) {
          _handleNodeIndexResult(tryRes)
        } else {
          log( WarnMsgs.INDEX_RESP_TOO_OLD + ": " + tryRes )
        }
    }

    /** Реакция на успешный результат запроса node index.
      * Надо посмотреть текущую оценку достаточности геоточности и принять решение о переключении
      * либо о продолжении определения геолокации. */
    override protected def _nodeIndexReceived(mni: MNodeIndex): Unit = {
      if (mni.geoAccurEnought contains true) {
        super._nodeIndexReceived(mni)
      } else {
        // Сервер считает, что можно попробовать уточнить данные геолокации.
        // Значит надо закешировать полученный ответ от сервера в _stateData, чтобы
        val sd0 = _stateData
        val sd1 = sd0.copy(
          geo = sd0.geo.copy(
            inxReqTstamp  = None,
            lastInx       = Some(mni)
          )
        )
        become(_waitMoreGeoState, sd1)
      }
    }

    /** Состояние, когда требуется ещё подождать данные геолокации. */
    def _waitMoreGeoState: FsmState

  }

}
