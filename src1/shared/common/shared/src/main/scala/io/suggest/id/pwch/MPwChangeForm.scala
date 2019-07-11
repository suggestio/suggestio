package io.suggest.id.pwch

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.19 17:35
  * Description: Сабмит на сервер данных формы смены пароля.
  */
object MPwChangeForm {

  implicit def mPwChangeFormJson: OFormat[MPwChangeForm] = (
    (__ \ "o").format[String] and
    (__ \ "n").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MPwChangeForm] = UnivEq.derive

}


/** @param pwOld Текущий пароль.
  * @param pwNew Новый пароль.
  */
case class MPwChangeForm(
                          pwOld     : String,
                          pwNew     : String,
                        )
