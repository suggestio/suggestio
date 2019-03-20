package io.suggest.id.login.m

import io.suggest.ext.svc.MExtService
import io.suggest.spa.DAction

import scala.util.Try

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

/** Сигнал от кнопки запуска логина. */
case object EpwDoLogin extends ILoginFormAction

/** Результат запроса логина. */
case class EpwLoginResp( timestampMs: Long, tryRes: Try[String] ) extends ILoginFormAction


/** Выставление галочки "Чужой компьютер?" */
case class EpwSetForeignPc(isForeign: Boolean ) extends ILoginFormAction


/** Логин через внешний сервис. */
case class ExtLoginVia(service: MExtService ) extends ILoginFormAction
case class ExtLoginViaTimeout(tstamp: Long) extends ILoginFormAction
