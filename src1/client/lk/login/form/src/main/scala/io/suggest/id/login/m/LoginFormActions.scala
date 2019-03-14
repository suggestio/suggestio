package io.suggest.id.login.m

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:10
  * Description: Экшены формы логина.
  */
sealed trait ILoginFormAction extends DAction


/** Сокрытие или отображение на экран диалога с формой логина. */
case class LoginShowHide( isShow: Boolean ) extends ILoginFormAction

/** Смена таба в форме логина. */
case class SwitсhLoginTab( tab: MLoginTab ) extends ILoginFormAction


/** Ввод имени. */
case class EpwSetName(name: String) extends ILoginFormAction

/** Ввод пароля. */
case class EpwSetPassword(password: String) extends ILoginFormAction

/** Кнопка запуска логина. */
case object EpwDoLogin extends ILoginFormAction

