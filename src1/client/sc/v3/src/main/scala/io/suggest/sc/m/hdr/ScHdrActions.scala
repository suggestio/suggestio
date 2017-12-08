package io.suggest.sc.m.hdr

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 20:57
  * Description: Diode-экшены для событий в заголовке выдачи.
  */
trait IScHdrAction extends DAction

// TODO Сделать классами с флагами внутри. Чтобы точно знать, что подразумевалось под каждым кликом.
// Это защитит от двойных кликов.

/** Клик по кнопке поиска (справа) с указанной целью.
  * @param open true - для открытия поиска. false -- для закрытия.
  */
case class HSearchBtnClick(open: Boolean, silent: Boolean = false) extends IScHdrAction

/** Клик по кнопке меню (слева). */
case object HMenuBtnClick extends IScHdrAction

/** Клик по кнопке влево/назад. */
case object HLeftBtnClick extends IScHdrAction

/** Клик по логотипу или названию узла. */
case object HLogoClick extends IScHdrAction
