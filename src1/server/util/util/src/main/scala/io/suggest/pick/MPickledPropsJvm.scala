package io.suggest.pick

import boopickle.Pickler
import io.suggest.bin.ConvCodecs
import PickleSrvUtil._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 17:07
  * Description: Server-side JSON-модель MPickledProps.
  */
object MPickledPropsJvm {

  /** Поддержка JSON-сериализации и десериализации.
    *
    * @param p boopickle-сериализатор.
    * @tparam V Тип хранимого значения внутри [[MPickledPropsJvm]].
    * @return JSON-форматтер, ориентированный на JSON вида '{ _: "BaSe64StRiNg" }'.
    */
  implicit def mPickledPropsFormat[V](implicit p: Pickler[V]): OFormat[MPickledPropsJvm[V]] = {
    (__ \ PickleUtil.PICKED_FN)
      .format[String]
      .inmap [MPickledPropsJvm[V]] (
        {b64 =>
          val v = PickleUtil.unpickleConv[String, ConvCodecs.Base64, V]( b64 )
          apply(v)
        },
        {mpp =>
          val v = mpp.value
          PickleUtil.pickleConv[V, ConvCodecs.Base64, String]( v )
        }
      )
  }

}


/** Класс модели-контейнера для произвольных данных.
  *
  * @param value Хранимое внутри значение.
  * @tparam V Тип хранимого значения.
  */
case class MPickledPropsJvm[V](
                                value  : V
                              )
