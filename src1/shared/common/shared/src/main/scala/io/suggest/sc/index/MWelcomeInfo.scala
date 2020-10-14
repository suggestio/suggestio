package io.suggest.sc.index

import io.suggest.color.MColorData
import io.suggest.common.empty.EmptyProduct
import io.suggest.media.IMediaInfo
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.07.17 18:1
  * Description: Модель welcome info для рендера splash-скрина приветствия.
  *
  * Построена по мотивам MWelcomeRenderArgs, но:
  * - поле fgText выкинуто, т.к. оно всегда совпадало с node name.
  * - поле bg распилено на простые опциональные поля bgColor + bgImage.
  */
object MWelcomeInfo {

  implicit def MWELCOME_INFO_FORMAT: OFormat[MWelcomeInfo] = (
    (__ \ "a").formatNullable[MColorData] and
    (__ \ "b").formatNullable[IMediaInfo] and
    (__ \ "c").formatNullable[IMediaInfo]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MWelcomeInfo] = UnivEq.derive

}


/** Класс модели данных приветствия узла.
  *
  * @param bgColor Цвет фона, если есть.
  * @param bgImage Фоновая картинка, если есть.
  * @param fgImage Изображение переднего плана, если есть.
  */
case class MWelcomeInfo(
                         bgColor: Option[MColorData],
                         bgImage: Option[IMediaInfo],
                         fgImage: Option[IMediaInfo]
                       )
  extends EmptyProduct
{

  override def toString: String = {
    StringUtil.toStringHelper( this, 128 ) { renderF =>
      bgColor foreach renderF("bc")
      bgImage foreach renderF("bg")
      fgImage foreach renderF("fg")
    }
  }

}
