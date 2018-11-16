package io.suggest.ad.edit.m

import io.suggest.ad.blk.BlockPadding
import io.suggest.jd.MJdAdData
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
    val CONF_FN         = "c"
    val AD_DATA_FN      = "a"
    val BLOCK_PADDING   = "p"
  }

  implicit val MAD_EDIT_FORM_INIT_FORMAT: OFormat[MAdEditFormInit] = (
    (__ \ Fields.CONF_FN).format[MAdEditFormConf] and
    (__ \ Fields.AD_DATA_FN).format[MJdAdData] and
    (__ \ Fields.BLOCK_PADDING).format[BlockPadding]
  )(apply, unlift(unapply))

}


/** Модель данных инициализация формы редактирора рекламной карточки.
  *
  * @param conf Конфиг формы.
  * @param adData Начальные данные формы.
  * @param blockPadding Расстояние между блоками.
  */
case class MAdEditFormInit(
                            conf          : MAdEditFormConf,
                            adData        : MJdAdData,
                            blockPadding  : BlockPadding
                          )