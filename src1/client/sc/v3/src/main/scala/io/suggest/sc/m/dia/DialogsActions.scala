package io.suggest.sc.m.dia

import io.suggest.sc.m.ISc3Action

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 17:31
  * Description: Экшены для визарда/визардов.
  */
sealed trait IDiaAction extends ISc3Action


/** Скрыто отрендерить диалог первого запуска, чтобы была анимация на первом шаге. */
case class InitFirstRunWz( isRendered: Boolean ) extends IDiaAction

/** Запустить first-run wizard. */
case class ShowFirstRunWz( isShown: Boolean ) extends IDiaAction

/** Клик по кнопкам "да" или "нет". */
case class YesNoWz( yesNo: Boolean ) extends IDiaAction
