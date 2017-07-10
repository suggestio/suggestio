package io.suggest.sc.hdr.m

import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 20:57
  * Description: Diode-экшены для событий в заголовке выдачи.
  */
trait IScHdrAction extends DAction

/** Клик по кнопке поиска (справа). */
case object HSearchBtnClick extends IScHdrAction

/** Клик по кнопке меню (слева). */
case object HMenuBtnClick extends IScHdrAction

/** Клик по кнопке влево/назад. */
case object HLeftBtnClick extends IScHdrAction

/** Клик по кнопке справа/вперёд. */
case object HRightBtnClick extends IScHdrAction

/** Клик по логотипу или названию узла. */
case object HLogoClick extends IScHdrAction
