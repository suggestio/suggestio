package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.dev.MScreenInfo
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 14:32
  * Description: Модель аргументов для css-шаблона доп.данных гео-панели.
  */
object MSearchCssProps {

  implicit object MSearchCssPropsFastEq extends FastEq[MSearchCssProps] {
    override def eqv(a: MSearchCssProps, b: MSearchCssProps): Boolean = {
      (a.nodesFound ===* b.nodesFound) &&
      (a.screenInfo ===* b.screenInfo) &&
      (a.searchBar ==* b.searchBar)
    }
  }

  @inline implicit def univEq: UnivEq[MSearchCssProps] = UnivEq.derive

  def nodesFound = GenLens[MSearchCssProps](_.nodesFound)
  def screenInfo = GenLens[MSearchCssProps](_.screenInfo)
  def searchBar = GenLens[MSearchCssProps](_.searchBar)

}


/** Аргументы для SearchCss.
  *
  * @param nodesFound Найденный список узлов.
  * @param screenInfo Данные экрана устройства.
  * @param searchBar Рендерить ли стили для панели поиска в целом?
                     TODO args.searchBar - надо вынести это в отдельные стили, которые рендерятся только для GeoTab.
  */
case class MSearchCssProps(
                            nodesFound   : MNodesFoundS   = MNodesFoundS.empty,
                            screenInfo   : MScreenInfo,
                            searchBar    : Boolean        = false,
                          )
