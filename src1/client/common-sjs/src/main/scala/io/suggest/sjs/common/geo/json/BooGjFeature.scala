package io.suggest.sjs.common.geo.json

import boopickle.Pickler
import io.suggest.pick.MPickledPropsJs

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 11:11
  * Description: Sjs-модель поверх JSON-нативной [[GjFeature]], чтобы иметь прозрачный достук
  * к boopickle-сериализованным properties.
  * Модель в повторяет поля исходной [[GjFeature]], но не wrap'и их, а дублирует.
  */
object BooGjFeature {

  /** Десериализация одной [[GjFeature]] (GeoJSON Feature).
    *
    * @param featureOrNull Gj-фича или null.
    *                      Передача null допускается, т.к. на сервере используется streaming-сериализатор, добивающий null'ом хвост списка.
    * @param p Поддержка boopickle.
    * @tparam V Тип хранимого в props значения.
    * @return Опциональный инстанс [[BooGjFeature]].
    */
  def fromFeatureOpt[V](featureOrNull: GjFeature)(implicit p: Pickler[V]): Option[BooGjFeature[V]] = {
    for {
      feature     <- Option( featureOrNull )
      mpp         <- MPickledPropsJs.applyOpt[V]( feature.properties )
    } yield {
      apply(
        props     = mpp,
        geometry  = feature.geometry
      )
    }
  }


  def fromFeaturesIter[V](features: js.Array[GjFeature])(implicit p: Pickler[V]): Iterator[BooGjFeature[V]] = {
    features
      .toIterator
      .flatMap { fromFeatureOpt[V](_) }
  }

}


case class BooGjFeature[V](
                            props     : V,
                            geometry  : GjGeometry
                          )
