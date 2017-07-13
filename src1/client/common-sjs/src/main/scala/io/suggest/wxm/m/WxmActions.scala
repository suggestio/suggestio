package io.suggest.wxm.m

import java.nio.ByteBuffer

import io.suggest.sjs.common.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 21:37
  * Description: Diode-экшены для Wxm FSM.
  */
sealed trait IWxmAction extends DAction


/** Экшен открытия web-socket'а с помощью указанной ссылки. */
protected[wxm] case class NewConn(url: String) extends IWxmAction


/** Перевести FSM в режим накапливания сообщений для отправки одной пачкой. */
protected[wxm] case object Begin extends IWxmAction

/** FSM пора отправить все сообщения из очереди. */
protected[wxm] case object Commit extends IWxmAction


/** Экшен отправки реквестов. */
case class SendReqs[In, Out](
                              msgs   : Seq[MMsgInfo[In, Out]]
                            )
  extends IWxmAction


/** Получение XHR-ответа сервера по ранее отправленному запросу. */
protected[wxm] case class HandleXhrResp( tryResp: Try[ByteBuffer] ) extends IWxmAction

/** Результат синхронного эффекта отправки сообщения через вебсокет. */
protected[wxm] case class WsSentRes( trySend: Try[_] ) extends IWxmAction
