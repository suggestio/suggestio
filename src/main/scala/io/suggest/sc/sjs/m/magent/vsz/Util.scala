package io.suggest.sc.sjs.m.magent.vsz

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 10:55
 * Description: Утиль для сборки аддонов для определения размеров окна.
 */

/** API доступ к wh DOM-элемента  */
trait ClientElementWhSafe extends js.Object {
  def clientWidth : UndefOr[Int] = js.native
  def clientHeight: UndefOr[Int] = js.native
}


trait ElSafeGetIntValueT {

  def _elSafe: UndefOr[ClientElementWhSafe]

  def getValue(supVal: Option[Int])(docVal: ClientElementWhSafe => UndefOr[Int]): Option[Int] = {
    supVal orElse {
      _elSafe
        .flatMap(docVal)
        .toOption
    }
  }

}
