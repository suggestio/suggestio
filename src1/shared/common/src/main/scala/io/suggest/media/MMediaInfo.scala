package io.suggest.media

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 9:42
  * Description: Кросс-модель описания одного элемента галереи.
  */
object MMediaInfo {

  import boopickle.Default._

  /** Поддержка бинарной сериализации между клиентом и сервером. */
  implicit val mGalItemPickler: Pickler[MMediaInfo] = {
    implicit val giTypeP = MMediaType.mGalItemTypePickler
    implicit val p = compositePickler[MMediaInfo]
    p.addConcreteType[MMediaInfo]
  }

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
                       giType   : MMediaType,
                       url      : String,
                       thumb    : Option[MMediaInfo] = None
                     )


