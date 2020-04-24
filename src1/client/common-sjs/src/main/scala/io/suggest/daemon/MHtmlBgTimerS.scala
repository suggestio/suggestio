package io.suggest.daemon

import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 16:30
  * Description: Состояние голого фонового таймера на базе setInterval().
  */
object MHtmlBgTimerS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MHtmlBgTimerS] = UnivEq.derive

  def timerId = GenLens[MHtmlBgTimerS]( _.timerId )

}


/** Состояние контроллера фонового таймера.
  *
  * @param timerId id запущенного таймера.
  */
case class MHtmlBgTimerS(
                          timerId       : Option[Int]         = None,
                        )
