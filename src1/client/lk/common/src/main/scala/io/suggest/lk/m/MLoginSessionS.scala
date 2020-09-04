package io.suggest.lk.m

import diode.data.Pot
import io.suggest.proto.http.cookie.{MHttpCookie, MHttpCookieParsed}
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

}


/** Контейнер данных сессии.
  *
  * @param cookie Токен, пришедший с сервера.
  *              pending/fail-состояния связаны (в первую очередь) с сохранением токена в какое-то хранилище на девайсе.
  */
final case class MLoginSessionS(
                                 cookie           : Pot[MHttpCookieParsed]          = Pot.empty,
                               )
