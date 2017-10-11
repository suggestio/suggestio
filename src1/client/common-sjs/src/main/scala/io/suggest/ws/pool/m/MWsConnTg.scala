package io.suggest.ws.pool.m

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 21:08
  * Description: Модель данных по одной цели для подключения по websocket.
  * Просто строку хоста не используем для возможности кастомайзить параметры соединения.
  * MHostUrl не подходит, потому что relUrl слишком сильно варьируется.
  */
object MWsConnTg {

  implicit def univEq: UnivEq[MWsConnTg] = UnivEq.derive

}


/** Класс модели описания цели для коннекта по WebSocket.
  *
  * @param host Хостнейм сервера, на который надо приконнектиться.
  */
case class MWsConnTg(
                      host: String
                    )
