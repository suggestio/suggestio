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
    // Чтобы инстанс был только один на операцию, но вычищался из памяти, тут используется связка из var.
    var fmt: OFormat[MMediaInfo] = null
    fmt = (
      (__ \ "y").format[MMediaType] and
      (__ \ "u").format[String] and
      (__ \ "t").format[String] and
      (__ \ "i").lazyFormatNullable( fmt ) and
      (__ \ "w").formatNullable[MSize2di]
    )( apply, unlift(unapply) )

    fmt
  }

  @inline implicit def univEq: UnivEq[MMediaInfo] = UnivEq.derive

}


/** Класс-контейнер данных по одному элементу галлереи.
  *
  * @param giType Тип элемента галлереи. Изначально только Image.
  * @param url Ссылка на картинку.
  * @param thumb Элемент thumb-галлереи, если есть.
  *              По идее всегда картинка или None.
  *              В теории же -- необязательно.
  * @param contentType Тип MIME.
  */
case class MMediaInfo(
                       giType   : MMediaType,
                       url      : String,
                       contentType: String,
                       // TODO ЗАменить поле на обёртку Tree.Node()
                       thumb    : Option[MMediaInfo] = None,
                       whPx     : Option[MSize2di] = None
                     )
