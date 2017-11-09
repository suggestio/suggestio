package io.suggest.ad.edit.m

import io.suggest.ad.blk.BlockPadding
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
    val BLOCK_PADDING = "p"
  }

  implicit val MAD_EDIT_FORM_INIT_FORMAT: OFormat[MAdEditFormInit] = (
    (__ \ Fields.CONF_FN).format[MAdEditFormConf] and
    (__ \ Fields.FORM_FN).format[MAdEditForm] and
    (__ \ Fields.BLOCK_PADDING).format[BlockPadding]
  )(apply, unlift(unapply))

}


/** Модель данных инициализация формы редактирора рекламной карточки.
  *
  * @param conf Конфиг формы.
  * @param form Начальные данные формы.
  * @param blockPadding Расстояние между блоками.
  */
case class MAdEditFormInit(
                            conf          : MAdEditFormConf,
                            form          : MAdEditForm,
                            blockPadding  : BlockPadding
                          )
