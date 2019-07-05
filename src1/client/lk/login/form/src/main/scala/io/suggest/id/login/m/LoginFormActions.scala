package io.suggest.id.login.m

import io.suggest.ext.svc.MExtService
import io.suggest.id.login.MLoginTab
import io.suggest.id.reg.MRegTokenResp
import io.suggest.primo.IApply1
import io.suggest.spa.DAction
import japgolly.univeq.UnivEq

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


/** Трейт, описывающий компаньоны для экшенов, которые управляют состоянием чек-бокса. */
sealed trait ICheckBoxActionStatic extends IApply1 {
  override type ApplyArg_t = Boolean
  override type T <: ILoginFormAction
}
object ICheckBoxActionStatic {
  @inline implicit def univEq: UnivEq[ICheckBoxActionStatic] = UnivEq.derive
}

/** Выставление галочки "Чужой компьютер?" */
case class EpwSetForeignPc(isForeign: Boolean ) extends ILoginFormAction
case object EpwSetForeignPc extends ICheckBoxActionStatic {
  override type T = EpwSetForeignPc
}


/** Логин через внешний сервис. */
case class ExtLoginVia(service: MExtService ) extends ILoginFormAction
case class ExtLoginViaTimeout(tstamp: Long) extends ILoginFormAction


/** Рега: Управления галочкой согласия с условиями сервиса. */
case class RegTosSetAccepted( isAccepted: Boolean ) extends ILoginFormAction
case object RegTosSetAccepted extends ICheckBoxActionStatic {
  override type T = RegTosSetAccepted
}

/** Рега: Управление галочкой разрешения на обработку перс.данных. */
case class RegPdnSetAccepted( isAccepted: Boolean ) extends ILoginFormAction
case object RegPdnSetAccepted extends ICheckBoxActionStatic {
  override type T = RegPdnSetAccepted
}

case object RegAccept extends ILoginFormAction


/** Редактирование поля email при регистрации. */
case class RegEmailEdit( email: String ) extends ILoginFormAction

case object RegEmailBlur extends ILoginFormAction


/** Редактирование поля номера телефона. */
case class RegPhoneEdit( phone: String ) extends ILoginFormAction

case object RegPhoneBlur extends ILoginFormAction


// TODO Унифицировать все Resp-сигналы с помощью step-аргумента.
/** Результат сабмита реквизитов регистрации. */
case class RegCredsSubmitResp(tstamp: Long, resp: Try[MRegTokenResp]) extends ILoginFormAction

/** Ответ от результата сабмита формы регистрации по паролю. */
case class RegCaptchaSubmitResp(tstamp: Long, resp: Try[MRegTokenResp]) extends ILoginFormAction

/** Ответ по поводу проверки смс-кода. */
case class RegSmsCheckResp(tstamp: Long, tryResp: Try[MRegTokenResp]) extends ILoginFormAction

/** Результат сабмита финальной формы регистрации. */
case class RegFinalSubmitResp(tstamp: Long, tryResp: Try[MRegTokenResp]) extends ILoginFormAction


/** Нажимание кнопки "Далее" в форме регистрации. */
case object RegNextClick extends ILoginFormAction
case object RegBackClick extends ILoginFormAction


/** Редактирование поля номера телефона. */
case class SetPasswordEdit(value: String, isRetype: Boolean ) extends ILoginFormAction
/** Расфокусировка одного из полей выставления пароля. */
case object SetPasswordBlur extends ILoginFormAction

/** Включение-выключение режима сброса пароля. */
case class PwReset(enable: Boolean) extends ILoginFormAction
