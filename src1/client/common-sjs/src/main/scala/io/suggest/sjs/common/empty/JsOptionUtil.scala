package io.suggest.sjs.common.empty

import scala.scalajs.js
import scala.scalajs.js.UndefOr

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 15:26
  * Description: Доп.утиль для работы с option'ами на уровне js.
  */
object JsOptionUtil {

  /** Конвертация Option[T] в UndefOr[T]. */
  implicit def opt2undef[T](opt: Option[T]): UndefOr[T] = {
    opt.fold [UndefOr[T]] (js.undefined) (v => v)
  }

}
