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
object MLoginSessionS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MLoginSessionS] = UnivEq.derive

  def token = GenLens[MLoginSessionS]( _.cookie )
  def logout = GenLens[MLoginSessionS]( _.logout )

}


/** Контейнер данных сессии.
  *
  * @param cookie Cookie, пришедший с сервера.
  *               pending/fail-состояния связаны (в первую очередь) с сохранением токена в какое-то хранилище на девайсе.
  * @param logout Состояние диалога выхода из системы.
  */
final case class MLoginSessionS(
                                 cookie           : Pot[MCookieState]               = Pot.empty,
                                 logout           : Option[MLogOutDia]              = None,
                               )
