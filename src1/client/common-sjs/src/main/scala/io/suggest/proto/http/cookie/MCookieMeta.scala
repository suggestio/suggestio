package io.suggest.proto.http.cookie

import java.time.Instant

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.dt.CommonDateTimeUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.2020 15:21
  * Description: Модель описания метаданных кукиса.
  * Под метаданными подразумеваются какие-либо внешние (по отношению к кукису) данные,
  * например время получения кукиса с сервера на клиент и др.
  */
object MCookieMeta {

  object Fields {
    final def RECEIVED_AT = "r"
  }

  implicit def cookieMetaJson: OFormat[MCookieMeta] = {
    val F = Fields
    (__ \ F.RECEIVED_AT).format[Instant]
      .inmap(apply, _.receivedAt)
  }

  @inline implicit def univEq: UnivEq[MCookieMeta] = UnivEq.derive

}


/** Контейнер метаданных кукиса.
  *
  * @param receivedAt Момент получения кукиса на клиенте.
  */
final case class MCookieMeta(
                              receivedAt                : Instant               = Instant.now(),
                            )
