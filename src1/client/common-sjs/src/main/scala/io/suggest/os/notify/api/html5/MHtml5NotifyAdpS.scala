package io.suggest.os.notify.api.html5

import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.04.2020 11:51
  * Description: Состояние html5-адаптера.
  */
object MHtml5NotifyAdpS {

  @inline implicit def univEq: UnivEq[MHtml5NotifyAdpS] = UnivEq.derive

  def permission = GenLens[MHtml5NotifyAdpS]( _.permission )

}


case class MHtml5NotifyAdpS(
                             permission       : Option[Boolean]                     = None,
                           )
