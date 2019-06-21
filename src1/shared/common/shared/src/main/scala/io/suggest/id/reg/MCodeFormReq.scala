package io.suggest.id.reg

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.19 18:28
  * Description: Реквест на сервер с данными смс-кода.
  */
object MCodeFormReq {

  implicit def mRegSmsCodeReqJson: OFormat[MCodeFormReq] = (
    (__ \ "t").format[String] and
    (__ \ "s").format[MCodeFormData]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MCodeFormReq] = UnivEq.derive

}

case class MCodeFormReq(
                         token        : String,
                         formData     : MCodeFormData,
                       )
