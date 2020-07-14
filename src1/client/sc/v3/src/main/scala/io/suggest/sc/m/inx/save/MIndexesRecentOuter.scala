package io.suggest.sc.m.inx.save

import diode.data.Pot
import io.suggest.sc.v.search.SearchCss
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.2020 15:49
  * Description: Контейнер состояния по сохранённым узлам.
  */
object MIndexesRecentOuter {

  def searchCss = GenLens[MIndexesRecentOuter]( _.searchCss )
  def saved = GenLens[MIndexesRecentOuter]( _.saved )

  @inline implicit def univEq: UnivEq[MIndexesRecentOuter] = UnivEq.derive

}


/** Контейнер данных состояния по недавно-пройденным узлам (состояниям).
  *
  * @param searchCss css.
  * @param saved Текущее сохранённое состояние.
  */
final case class MIndexesRecentOuter(
                                      searchCss     : SearchCss,
                                      saved         : Pot[MIndexesRecent]       = Pot.empty,
                                    )
