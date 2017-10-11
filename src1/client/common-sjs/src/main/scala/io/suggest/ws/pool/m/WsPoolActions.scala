package io.suggest.ws.pool.m

import io.suggest.spa.DAction
import io.suggest.ws.{MWsMsg, MWsMsgType}
import org.scalajs.dom.raw.WebSocket

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 18:20
  * Description: Diode-экшены контроллера пула ws-коннекшенов.
  */

sealed trait IWsPoolAction extends DAction


/** Организовать в пуле коннекшенов новый коннекшен.
  * Если коннекшен уже существует, то будет обновлён ttl.
  *
  * @param target Хост и url path.
  * @param closeAfterSec Автозакрытие спустя указанное время.
  */
case class WsEnsureConn(
                         target           : MWsConnTg,
                         closeAfterSec    : Option[Int] = None
                       )
  extends IWsPoolAction


/** Сообщение об успешном открытии веб-сокета. */
case class WsOpenedConn(
                         target         : MWsConnTg,
                         ws             : Try[WebSocket],
                         closeAfterSec  : Option[Int]
                       )
  extends IWsPoolAction


/** Принудительно закрыть указанный коннекшен. */
case class WsCloseConn( target: MWsConnTg ) extends IWsPoolAction


/** Поступило новое сообщение из сокета. */
case class WsRawMsg(target: MWsConnTg, payload: Any) extends IWsPoolAction

/** Результат обработки [[WsRawMsg]]: распарсенное сообщение из websocket WS-Channel.
  *
  * @param target Ключ websocket'а в пуле сокетов.
  * @param msg Распарсенное сообщение.
  */
case class WsChannelMsg(target: MWsConnTg, msg: MWsMsg) extends IWsPoolAction


/** Сообщение об ошибке сокета. */
case class WsError(target: MWsConnTg, message: String) extends IWsPoolAction


/** Сигнал к закрытию всех имеющихся ws-коннекшенов. */
case object WsCloseAll extends IWsPoolAction



/** При откладывании экшенов на базе сообщений из websocket'а WS-Channel может помочь эта модель.
  * Она хранит исходный экшен в произвольном, а так же
  *
  * @param typ Тип ws-channel-сообщения.
  * @param payload Данные экшена.
  * @param retryCount Счётчик повторных попыток.
  * @tparam T Тип завёрнутых внутри данных.
  */
case class WsRetriedAction[T](
                               typ         : MWsMsgType,
                               payload     : Try[T],
                               retryCount  : Int          = 0
                             )
  extends IWsPoolAction

