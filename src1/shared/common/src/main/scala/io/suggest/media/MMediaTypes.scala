package io.suggest.media

import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 9:43
  * Description: Модель типов элементов визуальной галлереи.
  */
object MMediaType {

  import MMediaTypes._
  import boopickle.Default._

  /** Поддержка бинарной сериализации между клиентом и сервером. */
  implicit val mGalItemTypePickler: Pickler[MMediaType] = {
    compositePickler[MMediaType]
      .addConcreteType[Image.type]
  }

}


/** Класс одного элемента модели. */
sealed abstract class MMediaType extends EnumEntry with IStrId {

  override final def strId = toString

  def isImage: Boolean

}


/** Модель с типами галерных элементов. */
object MMediaTypes extends Enum[MMediaType] {

  /** Картинка (изображение). */
  case object Image extends MMediaType {
    override def toString = "i"
    override def isImage = true
  }

  // TODO video, когда поддержка будет.

  override def values = findValues

}