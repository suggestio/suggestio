package io.suggest.jd.tags

import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.primo.TypeT
import play.api.libs.json.{OFormat, __}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 10:27
  * Description: Общий код для поддержки поля с цветом фона.
  */
object IBgColorOpt {

  val bgColorOptFormat: OFormat[Option[MColorData]] = {
    (__ \ "bg").formatNullable[MColorData]
  }

}


trait IBgColorOpt extends IDocTag with TypeT {

  override type T <: IBgColorOpt

  def bgColor: Option[MColorData]

  def withBgColor(bgColor: Option[MColorData]): IBgColorOpt

}
