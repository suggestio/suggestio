package io.suggest.dom

import japgolly.univeq.UnivEq
import org.scalajs.dom.File

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 12:58
  * Description: Утиль для скрещивания DOM и UnivEq.
  */
object DomUnivEq {

  implicit def fileUnivEq: UnivEq[File] = UnivEq.force

}
