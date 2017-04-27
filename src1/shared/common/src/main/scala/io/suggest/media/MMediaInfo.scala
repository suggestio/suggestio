package io.suggest.media

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
    implicit val giTypeP = MMediaType.mMediaTypePickler
    implicit val p = compositePickler[IMediaInfo]
    p.addConcreteType[MMediaInfo]
  }

}


/** Трейт, т.к. у нас тут рекурсивная модель, без трейта нельзя. */
trait IMediaInfo {

  /** Тип элемента галлереи. Изначально только Image. */
  def giType   : MMediaType

  /** Ссылка на картинку. */
  def url      : String

  /**
    * Элемент thumb-галлереи, если есть.
    * По идее всегда картинка или None. В теории же -- необязательно.
    */
  def thumb    : Option[MMediaInfo]

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
                       override val thumb    : Option[MMediaInfo] = None
                     )
  extends IMediaInfo


