package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.sc.search.{MSearchTab, MSearchTabs}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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
      (a.geo        ===* b.geo)  &&
        (a.text     ===* b.text)     &&
        (a.tags     ===* b.tags)     &&
        (a.currTab  ===* b.currTab)  &&
        (a.isShown  ==* b.isShown)
    }
  }

  implicit def univEq: UnivEq[MScSearch] = {
    UnivEq.derive
  }

}


/** Класс состояния панели поиска.
  *
  * @param mapInit Состояние инициализации карты.
  * @param text Состояние текстового поиска.
  * @param currTab Текущий таб на панели поиска.
  * @param isShown Открыта ли панель поиска на экране?
  */
case class MScSearch(
                      geo                 : MGeoTabS,
                      text                : MScSearchText         = MScSearchText.empty,
                      tags                : MTagsSearchS          = MTagsSearchS.empty,
                      currTab             : MSearchTab            = MSearchTabs.default,
                      isShown             : Boolean               = false,
                    ) {

  def withGeo       ( geo: MGeoTabS )                   = copy( geo = geo )
  def withText      ( text: MScSearchText )             = copy( text = text )
  def withTags      ( tags: MTagsSearchS )              = copy( tags = tags )
  def withCurrTab   ( currTab: MSearchTab )             = copy( currTab = currTab )
  def withIsShown   ( isShown: Boolean )                = copy( isShown = isShown )


  /** Сброс состояния тегов, если возможно. */
  def maybeResetTags: MScSearch = {
    resetTagsIfAny.getOrElse(this)
  }

  /** Вернуть обновлённый инстанс [[MScSearch]], если теги изменились в ходе сброса. */
  def resetTagsIfAny: Option[MScSearch] = {
    OptionUtil.maybe( tags.nonEmpty ) {
      resetTagsForce
    }
  }

  def resetTagsForce: MScSearch = {
    withTags( MTagsSearchS.empty )
  }

  def isShownTab(tab: MSearchTab): Boolean = {
    isShown && currTab ==* tab
  }


  /** Дедубликация кода сброса значения this.mapInit.loader. */
  // TODO Заинлайнить? Код по факту переместился в под-модель geo, а тут просто дёргается.
  def resetMapLoader: MScSearch = {
    withGeo( geo.resetMapLoader )
  }

  /** Вернуть pot поиска, относящийся к указанному (текущему) табу. */
  def tabSearchPot(tab: MSearchTab = currTab): Pot[MSearchRespInfo[_]] = {
    tab match {
      case MSearchTabs.GeoMap => geo.mapInit.rcvrs
      case MSearchTabs.Tags   => tags.tagsReq
    }
  }

}
