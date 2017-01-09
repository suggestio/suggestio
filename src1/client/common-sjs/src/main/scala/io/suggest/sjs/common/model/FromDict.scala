package io.suggest.sjs.common.model

import io.suggest.primo.TypeT

import scala.scalajs.js.Dictionary

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 10:06
  * Description: Есть ряд виртуальных моделей, которые на стороне JSON представляют просто JSON,
  * где поля обозначают var'ы модели.
  * Такие модели задаются trait'ом и команьоном на базе вот этого чуда.
  */
trait FromDict extends TypeT {

  def empty: T = Dictionary.empty[Any].asInstanceOf[T]

}
