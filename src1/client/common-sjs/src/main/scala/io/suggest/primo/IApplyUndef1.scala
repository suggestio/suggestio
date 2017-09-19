package io.suggest.primo

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.09.16 11:04
  * Description: Расширение вокруг applyOpt() на js.UndefOr[].
  */
trait IApplyUndef1 extends IApplyOpt1 {

  def applyUndef(vUnd: js.UndefOr[ApplyArg_t]): Option[T] = {
    applyOpt( vUnd.toOption )
  }

}
