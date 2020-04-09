package io.suggest.sc.m.grid

import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.04.2020 11:42
  * Description: Контейнер данных состояния нотификаций по карточкам в плитке.
  * Изначально создана для хранения истории нотифакций, уже выведенных ранее, т.е. для защиты от дубликатов.
  */
object MGridNotifyS {

  def empty = apply()

  def seenAdIds = GenLens[MGridNotifyS](_.seenAdIds)

  @inline implicit def univEq: UnivEq[MGridNotifyS] = UnivEq.derive

}


case class MGridNotifyS(
                         seenAdIds        : Set[String]         = Set.empty,
                       )
