package io.suggest.ad.edit.m

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:16
  * Description: Модель начальной инициализации формы редактирования карточки.
  */
object MAdEditFormInit {

  object Fields {
    val CONF_FN   = "c"
    val FORM_FN   = "f"
  }

  implicit val MAD_EDIT_FORM_INIT_FORMAT: OFormat[MAdEditFormInit] = (
    (__ \ Fields.CONF_FN).format[MAdEditFormConf] and
    (__ \ Fields.FORM_FN).format[MAdEditForm]
  )(apply, unlift(unapply))

}


/** Модель данных инициализация формы редактирора рекламной карточки.
  *
  * @param conf Конфиг формы.
  * @param form Начальные данные формы.
  */
case class MAdEditFormInit(
                            conf  : MAdEditFormConf,
                            form  : MAdEditForm
                          )
