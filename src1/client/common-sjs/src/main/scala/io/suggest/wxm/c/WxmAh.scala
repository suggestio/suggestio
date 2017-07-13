package io.suggest.wxm.c

import java.nio.ByteBuffer

import boopickle.Pickler
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.wxm.{MWxmMsg, MWxmMsg}
import io.suggest.wxm.m._

import scala.concurrent.Future
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.util.{Success, Try}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.07.17 11:31
  * Description: Контроллер WXM FSM, реагирующий на события WXM.
  */
class WxmAh[In: Pickler, Out: Pickler, M](
                                           xhrFallbackF    : IWxmXhrArgs[Out] => Future[ByteBuffer],
                                           modelRW         : ModelRW[M, MWxmRoot[In, Out]]
                                         )
  extends ActionHandler( modelRW )
  with Log
{

  /** Произвести отправку сообщения по доступному каналу связи. */
  private def doSendMsgs(msgInfos : Iterable[MMsgInfo[In, Out]],
                         v0       : MWxmRoot[In, Out] = value): ActionResult[M] = {

    // Предварительно подготовить сообщения к сериализации и отправке:
    val (idCounter2, msgInfosWithPartsRev) = msgInfos
      .iterator
      .foldLeft( (v0.idCounter, List.empty[(MMsgInfo[In,Out], MWxmMsg[Out])]) ) {
        case ((counter0, acc0), msgInfo) =>
          val part = MWxmMsg(
            id = msgInfo.replyTo
              .map(_ => counter0 + 1),
            payload = msgInfo.payload
          )
          val counter1 = part.id.getOrElse(counter0)
          (counter1, (msgInfo, part) :: acc0)
      }

    // Восстановить исходный порядок. TODO Надо ли это делать вообще?
    val msgInfosWithParts = msgInfosWithPartsRev.reverse

    // Собрать сообщение для отправки
    val msg = MWxmMsg(
      parts = msgInfosWithParts.map(_._2)
    )
    // Отправить сообщение готовому каналу связи:
    val msgBytes = PickleUtil.pickle( msg )

    // Наконец отправить пачку запросов на сервер
    // Найти рабочий WebSocket-коннекшен:
    val fx = v0.wsConns
      .valuesIterator
      .find(_.isReady)
      .fold [Effect] {
        // Нет готового к работе websocket'а.
        // Надо задействовать XHR, который вернёт все ответы ровно одной пачкой.
        Effect {
          val xhrArgs = MWxmXhrArgs(msg, msgBytes)
          val xhrFut = try {
            xhrFallbackF(xhrArgs)
          } catch {
            case ex: Throwable =>
              Future.failed(ex)
          }
          xhrFut.transform { tryResp =>
            Success( HandleXhrResp(tryResp) )
          }
        }

      } { wsConn =>
        // Есть готовый к работе ws-коннекшен. Впихнуть туда исходящее сообщение.
        //
        // Тут мутно описано, что надо typedArray() юзать вместо arrayBuffer(),
        // потому ByteBuffer внутрях держит TypedArray[Short,_]:
        // https://stackoverflow.com/a/44587782
        Effect.action {
          Try {
            wsConn.conn.send( msgBytes.typedArray().buffer )
          }
        }
      }

    // TODO Залить отправленые запросы (ожидающие ответа сообщения) в состояние. Вернуть вместе с опциональным эффектом.
    val wsReqsIter = for {
      (_, msgPart) <- msgInfosWithParts.iterator
      msgId        <- msgPart.id
    } yield {
      // Сконверить в MWxmReq.
      msgId -> MWxmReq( ???, ??? )
    }
    val v2 = v0.copy(
      idCounter2,
      wsReqs    = v0.wsReqs ++ wsReqsIter
    )
    updated(v2, fx)
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен с несериализованными сообщениями для отправки на сервер.
    case m: SendReqs[In, Out] =>
      if (m.msgs.isEmpty) {
        noChange
      } else {
        // Есть сообщения для отправки.
        // Отправить или запихнуть в очередь в зависимости от текущего состояния системы.
        val v0 = value
        v0.partsAcc.fold {
          // Без транзакции. Отправить всё сразу на сервер.
          ???
        } { partsAcc0 =>
          // Активен аккамулятор накопления данных. Впихнуть в него всё и закончить на этом.
          val v2 = v0.withPartsAcc(
            Some( partsAcc0 ++ m.msgs )
          )
          updated( v2 )
        }
      }

    // Запрашивается начало транзакции.
    case Begin =>
      val v0 = value
      v0.partsAcc.fold {
        // Инициализировать аккамулятор.
        val v2 = v0.withPartsAcc( Some(Nil) )
        updated( v2 )
      } { parts0 =>
        // Аккамулятор уже открыт, данный сигнал ошибочен.
        LOG.warn( WarnMsgs.TXN_ALREADY_OPENED, msg = parts0 )
        noChange
      }

    // Закрытие транзакции и отправка сообщений из очереди.
    case Commit =>
      val v0 = value
      v0.partsAcc.fold {
        LOG.warn( WarnMsgs.TXN_NOT_OPENED )
        noChange

      } { partsAcc =>
        if (partsAcc.isEmpty) {
          // Транзакция закрыта, но никаких данных не было пропихнуто.
          LOG.info( WarnMsgs.CLOSED_TXN_EMPTY )
          val v2 = v0.withPartsAcc( None )
          updated(v2)

        } else {
          // Есть в очереди сообщения, готовые к отправке на сервер.
          // Собрать мессагу и отправить, опустошив акк.
          val msg = MWxmMsg(
            parts = partsAcc
          )

          ???
        }

      }


  }

}
