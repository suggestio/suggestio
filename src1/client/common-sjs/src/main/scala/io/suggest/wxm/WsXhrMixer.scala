package io.suggest.wxm

import java.nio.ByteBuffer

import boopickle.Pickler
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.wxm.m._

import scala.concurrent.{Future, Promise}
/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 21:35
  * Description: Интерфейсы и реализация FSM для WS/XHR Mixing'а запросов к серверу.
  */

/** Интерфейс минимального WXM. */
trait IWsXhrMixerSimple[In, Out] {

  def calls(messages: Out*): Seq[Future[In]]

  def callSync(messages: Out*): Future[Seq[In]]

  def call(message: Out): Future[In]

  def cast(messages: Out*): Unit

}


/** Интерфейс WxM, поддерживающего транзакционную отправку сообщений на сервер. */
trait IWsXhrMixer[In, Out] extends IWsXhrMixerSimple[In, Out] {

  def sendAtOnce[Res](f: IWsXhrMixer[In, Out] => Res): Res

}


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 21:35
  * Description: Это реализация FSM для WS/XHR Mixing'а запросов к серверу.
  *
  * Когда работа с сервером надёжно скрыта за простым интерфейсом API'шки,
  * то можно запросы пропихивать через WebSocket вместо неспешного HTTP.
  *
  * @param xhrFallbackF Функция для передачи запроса через XHR, когда работа через WebSocket невозможна.
  *
  * @tparam In Тип и typeclass для сериализации исходящих по ws сообщений.
  * @tparam Out Тип и typeclass для десериализации входящий по ws сообщений.
  */
class WsXhrMixer[In: Pickler, Out: Pickler](
                                             xhrFallbackF    : IWxmXhrArgs[Out] => Future[ByteBuffer]
                                           )
  extends CircuitLog[MWxmRoot[In, Out]]
  with IWsXhrMixer[In, Out]
{ that =>

  implicit val rootFastEq = MWxmRoot.MWxmRootFastEq[In, Out]

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.WS_XHR_MIXING_ERROR

  override protected def initialModel: MWxmRoot[In, Out] = {
    MWxmRoot[In, Out]()
  }

  override protected def actionHandler: HandlerFunction = {
    ???
  }


  /** Список сообщений обменять на список фьючерсов с ответами. */
  def calls(messages: Out*): Seq[Future[In]] = {
    if (messages.isEmpty) {
      Nil
    } else {
      val msgsPromised = for (msg <- messages) yield {
        val p = Promise[In]()
        MMsgInfo(msg, Some(p))
      }
      val action = SendReqs(
        msgs   = msgsPromised
      )
      dispatch( action )

      for {
        m <- msgsPromised
        p = m.replyTo.get   // Тут всего Some() сейчас, поэтому get безопасен.
      } yield {
        p.future
      }
    }
  }


  /** Вызов API с сервера, искуственно синхронизированный на ожидание всех ответов,
    * и только потом -- возвратом ответов в исходном порядке. */
  override def callSync(messages: Out*): Future[Seq[In]] = {
    val cs = calls(messages: _*)
    Future.sequence(cs)
  }


  /** Отправка одного сообщения на сервер с возвратом ровно одного результата. */
  override def call(message: Out): Future[In] = {
    calls(message).head
  }


  /** Отправка на сервер данных без ожидания ответа.
    *
    * @param messages Сообщения для отправки.
    */
  override def cast(messages: Out*): Unit = {
    if (messages.nonEmpty) {
      val action = SendReqs(
        msgs = messages.map( MMsgInfo(_, None) )
      )
      dispatch(action)
    }
  }


  /** Транзакционая отправка пачек сообщений одним комплектом для сложных случаев.
    *
    * @param f Функция, дёргающая $1.call/cast().
    * @tparam Res Тип результата, возвращаемого функцией f.
    * @return Res, т.е. результат f().
    */
  override def sendAtOnce[Res](f: IWsXhrMixer[In, Out] => Res): Res = {
    dispatch( Begin )
    try {
      f(that)
    } finally {
      dispatch( Commit )
    }
  }

}
