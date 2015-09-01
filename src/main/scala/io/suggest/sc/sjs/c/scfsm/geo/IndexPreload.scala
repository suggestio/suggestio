package io.suggest.sc.sjs.c.scfsm.geo

import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.m.msrv.index.{MNodeIndexTimestamped, MNodeIndex}
import io.suggest.sjs.common.msg.WarnMsgs
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.15 9:37
 * Description: Утиль для сборки состояний, связанных с опережающей загрузкой node index
 * по имеющимся геоданным.
 */
trait IndexPreload extends Index {

  /** Аддон для запуска получения sc node index и получения ответа назад с временной отметкой. */
  trait AskGeoIndex extends FsmState with GetIndexUtil {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      // Запустить асинхронный запрос индексной страницы.
      val fut = _getIndex(sd0)
      _sendFutResBackTimestamped(fut, MNodeIndexTimestamped)
    }

  }


  /** Получение и обычная обработка timestamped index ответа. */
  trait GeoIndexWaitSimpleStateT extends FsmEmptyReceiverState with ProcessIndexReceivedUtil with IGetNodeIndexFailed {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Реакция на получения ответа с опрежающего index-запроса сервера.
      case MNodeIndexTimestamped(tryRes, tstamp) =>
        val sd0 = _stateData
        // Подавляем возможный race conditions на параллельных index-запросах через timestamp начала запроса.
        // В состоянии лежит timestamp последего запроса в рамках этой фазы.
        if ( !sd0.geo.inxReqTstamp.exists(_ > tstamp) ) {
          tryRes match {
            case Success(mni) =>
              _nodeIndexReceived(mni, tstamp)
            case Failure(ex) =>
              _getNodeIndexFailed(ex)
          }
        } else {
          log( WarnMsgs.INDEX_RESP_TOO_OLD )
        }
    }

    /** Состояние, на которое надо перещёлкнуть, когда node index preload не удался. */
    def _getNodeIndexFailedState: FsmState

    override protected def _getNodeIndexFailed(ex: Throwable): Unit = {
      become(_getNodeIndexFailedState)
    }

    protected def _nodeIndexReceived(mni: MNodeIndex, timestamp: Long): Unit = {
      val sd0 = _stateData
      _stateData = sd0.copy(
        geo = sd0.geo.copy(
          inxReqTstamp = None,
          lastInx = None
        )
      )
      _nodeIndexReceived(mni)
    }
  }


  /** Аддон для сборки состояний, ожидающий index во время геолокации. */
  trait GeoIndexWaitStateT extends GeoIndexWaitSimpleStateT {

    /** Реакция на успешный результат запроса node index.
      * Надо посмотреть текущую оценку достаточности геоточности и принять решение о переключении
      * либо о продолжении определения геолокации. */
    override protected def _nodeIndexReceived(mni: MNodeIndex, timestamp: Long): Unit = {
      if (mni.geoAccurEnought contains true) {
        super._nodeIndexReceived(mni)
      } else {
        // Сервер считает, что можно попробовать уточнить данные геолокации.
        // Значит надо закешировать полученный ответ от сервера в _stateData, чтобы
        val sd0 = _stateData
        val sd1 = sd0.copy(
          geo = sd0.geo.copy(
            inxReqTstamp  = Some(timestamp),
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
