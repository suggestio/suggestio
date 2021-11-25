package io.suggest.sc.model.search

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:31
  * Description: Над-модель для панели поиска.
  */
object MScSearch {

  /** Поддержка FastEq для инстансов [[MScSearch]]. */
  implicit object MScSearchFastEq extends FastEq[MScSearch] {
    override def eqv(a: MScSearch, b: MScSearch): Boolean = {
      (a.geo       ===* b.geo)    &&
      (a.panel     ===* b.panel)  &&
      (a.text      ===* b.text)
    }
  }

  @inline implicit def univEq: UnivEq[MScSearch] = UnivEq.derive

  val geo   = GenLens[MScSearch](_.geo)
  def panel = GenLens[MScSearch](_.panel)
  def text  = GenLens[MScSearch](_.text)


  implicit class ScSearchExt( private val scSearch: MScSearch ) extends AnyVal {

    /** Дедубликация кода сброса значения this.mapInit.loader. */
    // TODO Заинлайнить? Код по факту переместился в под-модель geo, а тут просто дёргается.
    def resetMapLoader: MScSearch = {
      geo.modify(_.resetMapLoader)(scSearch)
    }

  }

}


/** Класс состояния панели поиска.
  *
  * @param geo Состояние панели с картой.
  * @param text Состояние текстового поиска.
  * @param panel Состояние компонента панели на экране для панели поиска?
  */
case class MScSearch(
                      geo                 : MGeoTabS,
                      panel               : MSearchPanelS         = MSearchPanelS.default,
                      text                : MScSearchText         = MScSearchText.empty,
                    )
