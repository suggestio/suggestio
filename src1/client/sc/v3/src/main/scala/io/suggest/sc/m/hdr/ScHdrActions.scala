package io.suggest.sc.m.hdr

import io.suggest.sc.m.search.MSearchTab
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
  * @param onTab Открыть на указанном табе, если задан.
  *              Если не задано, то открыть табе, заданном в состоянии.
  */
case class SearchOpenClose(open: Boolean, onTab: Option[MSearchTab] = None) extends IScHdrAction


/** Экшен открытия/закрытия панели меню (слева). */
case class MenuOpenClose(open: Boolean) extends IScHdrAction


/** Клик по логотипу или названию узла. */
case object HLogoClick extends IScHdrAction
