package io.suggest.id

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 11:19
  * Description: JSON-модель данных логина по имени-паролю.
  */
object MEpwLoginReq {

  /** Поддержка play-json. */
  implicit def epwLoginReq: OFormat[MEpwLoginReq] = (
    (__ \ "e").format[String] and
    (__ \ "p").format[String] and
    (__ \ "f").format[Boolean]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MEpwLoginReq] = UnivEq.derive

}


/** Контейнер данных запроса логина по имени-паролю.
  *
  * @param name Имя (email).
  * @param password Пароль.
  * @param isForeignPc Галочка "чужой компьютер".
  */
case class MEpwLoginReq(
                         name           : String,
                         password       : String,
                         isForeignPc    : Boolean,
                       )
