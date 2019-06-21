package io.suggest.id.reg

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.common.empty.OptionUtil.BoolOptJsonFormatOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.06.19 12:32
  * Description: Контейнер данных отправки формы кода (смс-кода, кода капчи) на сервер.
  */
object MCodeFormData {

  implicit def mIdMsgFormDataJson: OFormat[MCodeFormData] = (
    (__ \ "c").formatNullable[String] and
    (__ \ "r").formatNullable[Boolean].formatBooleanOrFalse
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MCodeFormData] = UnivEq.derive

}


/** Контейнер данных засабмиченной формы ввода кода.
  *
  * @param code Значение кода, если отправлен.
  * @param reload Запрошена перезагрузка формы.
  */
case class MCodeFormData(
                          code      : Option[String]  = None,
                          reload    : Boolean         = false,
                        )
