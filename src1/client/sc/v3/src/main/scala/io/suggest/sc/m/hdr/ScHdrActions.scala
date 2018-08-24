package io.suggest.sc.m.hdr

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 20:57
  * Description: Diode-экшены для событий в заголовке выдачи.
  */
sealed trait IScHdrAction extends DAction


/** Экшен открытия/закрытия панели поиска (справа).
  * @param open true - для открытия поиска. false -- для закрытия.
  */
case class SearchOpenClose(open: Boolean) extends IScHdrAction


/** Экшен открытия/закрытия панели меню (слева). */
case class MenuOpenClose(open: Boolean) extends IScHdrAction


/** Клик по логотипу или названию узла. */
case object HLogoClick extends IScHdrAction
