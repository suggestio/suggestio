package io.suggest.wxm.m

import diode.FastEq
import io.suggest.wxm.WxmMsgId_t
import org.scalajs.dom.{WebSocket, XMLHttpRequest}

import scala.concurrent.Promise

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 21:39
  * Description: Корневая модель WXM FSM.
  */
object MWxmRoot {

  implicit def MWxmRootFastEq[In,Out] = new FastEq[MWxmRoot[In,Out]] {
    override def eqv(a: MWxmRoot[In,Out], b: MWxmRoot[In,Out]): Boolean = {
      (a.wsConns eq b.wsConns) &&
        (a.idCounter == b.idCounter) &&
        (a.wsReqs eq b.wsReqs) &&
        (a.partsAcc eq b.partsAcc)
    }
  }

}


/** Класс корневой модель WXM FSM.
  * Это контейнер всех данных текущего состояния этого FSM.
  *
  * @param wsConns Текущие WebSocket-коннекшены.
  * @param idCounter Счётчик уникальных id'шников для сообщений, отправляемых на сервер.
  * @param wsReqs Текущие реквесты, отправленные на сервер по WebSocket.
  * @param partsAcc Режим аккамулирования входящих запросов в очередь на отправку одной пачкой.
  */
case class MWxmRoot[In, Out](
                              wsConns      : Map[String, MWsConnInfo]           = Map.empty,
                              idCounter    : WxmMsgId_t                         = 0,
                              wsReqs       : Map[WxmMsgId_t, MWxmReq]                  = Map.empty,
                              partsAcc     : Option[Iterable[MMsgInfo[In,Out]]]   = None
                            ) {

  def withWsConns(wsConns: Map[String, MWsConnInfo]) = copy(wsConns = wsConns)
  def withIdCounter(idCounter: WxmMsgId_t) = copy(idCounter = idCounter)
  def withWsReqs(wsReqs: Map[WxmMsgId_t, MWxmReq]) = copy(wsReqs = wsReqs)
  def withPartsAcc(partsAcc: Option[Iterable[MMsgInfo[In,Out]]]) = copy(partsAcc = partsAcc)

}


/** Модель данных состояния одного текущего реквеста.
  *
  * @param wsTimeoutTimerId Таймер для таймаута по WebSocket.
  * @param xhr Возможно запущенный по XHR запрос.
  */
case class MWxmReq(
                    wsTimeoutTimerId  : Int,
                    xhr               : Option[XMLHttpRequest]
                    // TODO таймер, fallback-фунция XHR, опциональный XHR-resp-инстанс, др.
                  )


/** Данные по websocket-коннекшену.
  *
  * @param conn Веб-сокет, открытый или открывающийся.
  * @param isReady Готов ли данный сокет к обмену сообщениями?
  */
case class MWsConnInfo(
                       conn     : WebSocket,
                       isReady  : Boolean
                     )

/** Модель описания сообщения в очереди на отправку. */
case class MMsgInfo[In, Out](
                              payload : Out,
                              replyTo : Option[Promise[In]]
                            )
