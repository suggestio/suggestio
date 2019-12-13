package io.suggest.sc.m.menu

import io.suggest.sc.m.ISc3Action

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 14:43
  * Description: Экшены менюшки выдачи.
  */
sealed trait IScMenuAction extends ISc3Action


/** Экшены для кнопки скачивания нативного приложения. */
sealed trait INativeAppAction extends IScMenuAction


/** MenuNativeApp Open/Close - экшен клика по элементу приложения. */
case class MenuAppOpenClose(openClose: Boolean) extends IScMenuAction
