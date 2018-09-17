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
object IMediaInfo {

  import boopickle.Default._

  /** Поддержка бинарной сериализации между клиентом и сервером. */
  implicit val iMediaItemPickler: Pickler[IMediaInfo] = {
    implicit val mMediaTypeP = MMediaType.mMediaTypePickler
    implicit val size2diP = MSize2di.size2diPickler
    implicit val p = compositePickler[IMediaInfo]
    p.addConcreteType[MMediaInfo]
  }


  implicit def IMEDIA_INFO_FORMAT: OFormat[IMediaInfo] = (
    (__ \ "y").format[MMediaType] and
    (__ \ "u").format[String] and
    (__ \ "i").lazyFormatNullable( IMEDIA_INFO_FORMAT ) and
    (__ \ "w").formatNullable[MSize2di]
  ).apply( MMediaInfo.apply, { imi: IMediaInfo => (imi.giType, imi.url, imi.thumb, imi.whPx) } )

  implicit def univEq: UnivEq[IMediaInfo] = UnivEq.derive

}

// TODO Удалить трейт IMediaInfo следом за boopickle-сериализацией здесь. Оставить только MMediaInfo.

/** Трейт, т.к. у нас тут рекурсивная модель, без трейта нельзя. */
sealed trait IMediaInfo {

  /** Тип элемента галлереи. Изначально только Image. */
  def giType   : MMediaType

  /** Ссылка на картинку. */
  def url      : String

  /**
    * Элемент thumb-галлереи, если есть.
    * По идее всегда картинка или None. В теории же -- необязательно.
    */
  def thumb    : Option[IMediaInfo]

  /** Опциональная инфа по ширине и высоте картинки/видео. */
  def whPx     : Option[MSize2di]

}


/** Класс-контейнер данных по одному элементу галлереи.
  *
  * @param giType Тип элемента галлереи. Изначально только Image.
  * @param url Ссылка на картинку.
  * @param thumb Элемент thumb-галлереи, если есть.
  *              По идее всегда картинка или None.
  *              В теории же -- необязательно.
  */
case class MMediaInfo(
                       override val giType   : MMediaType,
                       override val url      : String,
                       override val thumb    : Option[IMediaInfo] = None,
                       override val whPx     : Option[MSize2di] = None
                     )
  extends IMediaInfo


