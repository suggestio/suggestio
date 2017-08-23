package io.suggest.ad.edit.m

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:17
  * Description: Модель данных server-side конфигурации формы редактирования карточки.
  */
object MAdEditFormConf {

  object Fields {
    val PRODUCER_ID_FN = "p"
    val AD_ID_FN       = "a"
  }

  implicit val MAD_EDIT_FORM_CONF_FORMAT: OFormat[MAdEditFormConf] = (
    (__ \ Fields.PRODUCER_ID_FN).format[String] and
    (__ \ Fields.AD_ID_FN).formatNullable[String]
  )(apply, unlift(unapply))

}


/** Класс модели-контейнера данных конфигурации формы редактирования карточки.
  *
  * @param producerId id текущего узла-владельца карточки.
  * @param adId id текущей рекламной карточки.
  */
case class MAdEditFormConf(
                            producerId  : String,
                            adId        : Option[String],
                          )
