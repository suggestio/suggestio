package io.suggest.id.login.m.session

import diode.data.Pot
import io.suggest.proto.http.cookie.MCookieState
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.2020 18:15
  * Description: Контейнер данных сессии.
  */
object MSessionS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MSessionS] = UnivEq.derive

  def token = GenLens[MSessionS]( _.cookie )

}


/** Контейнер данных сессии.
  *
  * @param cookie Cookie, пришедший с сервера.
  *               pending/fail-состояния связаны (в первую очередь) с сохранением токена в какое-то хранилище на девайсе.
  */
final case class MSessionS(
                            cookie           : Pot[MCookieState]               = Pot.empty,
                          )
