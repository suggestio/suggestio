package io.suggest.media

import io.suggest.common.geom.d2.MSize2di
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 9:42
  * Description: Кросс-модель описания одного элемента галереи.
  */
object MMediaInfo {

  implicit def MEDIA_INFO_FORMAT: OFormat[MMediaInfo] = {
    (
      (__ \ "y").format[MMediaType] and
      (__ \ "u").format[String] and
      (__ \ "t").format[String] and
      (__ \ "w").formatNullable[MSize2di]
    )( apply, unlift(unapply) )
  }

  @inline implicit def univEq: UnivEq[MMediaInfo] = UnivEq.derive

}



/** Класс-контейнер данных по одному элементу галлереи.
  *
  * @param giType Тип элемента галлереи. Изначально только Image.
  * @param url Ссылка на картинку.
  * @param contentType Тип MIME.
  */
case class MMediaInfo(
                       giType   : MMediaType,
                       url      : String,
                       contentType: String,
                       whPx     : Option[MSize2di] = None
                     )
