package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.common.empty.OptionUtil
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
  val panel = GenLens[MScSearch](_.panel)
  val text  = GenLens[MScSearch](_.text)

}


/** Класс состояния панели поиска.
  *
  * @param mapInit Состояние инициализации карты.
  * @param text Состояние текстового поиска.
  * @param panel Состояние компонента панели на экране для панели поиска?
  */
case class MScSearch(
                      geo                 : MGeoTabS,
                      panel               : MSearchPanelS         = MSearchPanelS.default,
                      text                : MScSearchText         = MScSearchText.empty,
                    ) {

  def withGeo       ( geo: MGeoTabS )                   = copy( geo = geo )
  def withText      ( text: MScSearchText )             = copy( text = text )
  def withPanel     ( panel: MSearchPanelS )            = copy( panel = panel )


  /** Сброс состояния найденных узлов (тегов), если возможно. */
  def maybeResetNodesFound: MScSearch = {
    resetNodesFoundIfAny.getOrElse(this)
  }

  /** Вернуть обновлённый инстанс [[MScSearch]], если теги изменились в ходе сброса. */
  def resetNodesFoundIfAny: Option[MScSearch] = {
    OptionUtil.maybe( geo.found.nonEmpty ) {
      resetTagsForce
    }
  }

  def resetTagsForce: MScSearch = {
    withGeo( geo.withFound( MNodesFoundS.empty ) )
  }


  /** Дедубликация кода сброса значения this.mapInit.loader. */
  // TODO Заинлайнить? Код по факту переместился в под-модель geo, а тут просто дёргается.
  def resetMapLoader: MScSearch = {
    withGeo( geo.resetMapLoader )
  }

}
