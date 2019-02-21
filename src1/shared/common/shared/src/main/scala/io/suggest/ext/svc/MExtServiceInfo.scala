package io.suggest.ext.svc

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 17:53
  * Description: Модель передачи инфы по внешнему сервису для js.
  */
object MExtServiceInfo {

  object Fields {
    def NAME_FN     = "n"
    def APP_ID_FN   = "i"
  }

  /** Поддержка сериализации/десериализации JSON. */
  implicit def mExtServiceInfoFormat: OFormat[MExtServiceInfo] = (
    (__ \ Fields.NAME_FN).format[MExtService] and
    (__ \ Fields.APP_ID_FN).formatNullable[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MExtServiceInfo] = UnivEq.derive

}


/** Класс-контейнер данных по внешнему сервису для js.
  *
  * @param service Идентификатор-название сервиса.
  * @param appId Идентификатор приложения s.io, зареганного на стороне сервиса, если есть.
  */
case class MExtServiceInfo(
                            service   : MExtService,
                            appId     : Option[String]
                          )
