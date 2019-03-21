package io.suggest.id.login.m

import io.suggest.ext.svc.MExtService
import io.suggest.id.login.MLoginTab
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


object IEpwSetValueStatic {
  @inline implicit def univEq: UnivEq[IEpwSetValueStatic] = UnivEq.derive
}
sealed trait IEpwSetValueStatic extends IApply1 {
  override type ApplyArg_t = String
  override type T <: ILoginFormAction
}

/** Ввод имени. */
case class EpwSetName(name: String) extends ILoginFormAction
case object EpwSetName extends IEpwSetValueStatic {
  override type T = EpwSetName
}

/** Ввод пароля. */
case class EpwSetPassword(password: String) extends ILoginFormAction
case object EpwSetPassword extends IEpwSetValueStatic {
  override type T = EpwSetPassword
}


/** Сигнал от кнопки запуска логина. */
case object EpwDoLogin extends ILoginFormAction

/** Результат запроса логина. */
case class EpwLoginResp( timestampMs: Long, tryRes: Try[String] ) extends ILoginFormAction


/** Выставление галочки "Чужой компьютер?" */
case class EpwSetForeignPc(isForeign: Boolean ) extends ILoginFormAction


/** Логин через внешний сервис. */
case class ExtLoginVia(service: MExtService ) extends ILoginFormAction
case class ExtLoginViaTimeout(tstamp: Long) extends ILoginFormAction

