package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.common.empty.OptionUtil
import io.suggest.model.n2.node.MNodeTypes
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
      (a.geo        ===* b.geo) &&
        (a.text     ===* b.text) &&
        (a.isShown  ==*  b.isShown)
    }
  }

  implicit def univEq: UnivEq[MScSearch] = UnivEq.derive

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
                      isShown             : Boolean               = false,
                    ) {

  /** id текущего тега. Временный костыль, ведь тегов может быть много. */
  lazy val selTagNodeId: Option[String] = {
    geo.found.selectedId
      .filter { _ =>
        geo.found.selectedNode
          .exists(_.props.ntype ==* MNodeTypes.Tag)
      }
  }

  def withGeo       ( geo: MGeoTabS )                   = copy( geo = geo )
  def withText      ( text: MScSearchText )             = copy( text = text )
  def withIsShown   ( isShown: Boolean )                = copy( isShown = isShown )


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
