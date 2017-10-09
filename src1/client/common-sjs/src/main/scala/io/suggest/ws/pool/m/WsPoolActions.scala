package io.suggest.ws.pool.m

import io.suggest.primo.MConflictAction
import io.suggest.sjs.common.spa.DAction
import io.suggest.url.MHostUrl

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
  * @param hostUrl Хост и url path.
  * @param conflictRes Что делать, когда на коннекшене уже висит какой-то другой callback?
  * @param closeAfterSec Автозакрытие спустя указанное время.
  * @param onOpenErrorF Что делать при ошибке сокета?
  */
case class WsEnsureConn(
                         hostUrl          : MHostUrl,
                         callbackF        : WsCallbackF,
                         conflictRes       : MConflictAction,
                         closeAfterSec    : Option[Int] = None,
                         onOpenErrorF     : Option[Throwable => Unit] = None
                       )
  extends IWsPoolAction


/** Принудительно закрыть указанный коннекшен. */
case class WsCloseConn( hostUrl: MHostUrl ) extends IWsPoolAction


/** Поступило новое сообщение из сокета. */
case class WsMsg(from: MHostUrl, payload: Any) extends IWsPoolAction


/** Сообщение об ошибке сокета. */
case class WsError(from: MHostUrl, message: String) extends IWsPoolAction


/** Сигнал к закрытию всех имеющихся ws-коннекшенов. */
case object WsCloseAll extends IWsPoolAction

