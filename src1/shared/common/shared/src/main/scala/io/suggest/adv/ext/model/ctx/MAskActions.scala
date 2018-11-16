package io.suggest.adv.ext.model.ctx

import io.suggest.common.menum.{EnumMaybeWithName, ILightEnumeration, LightEnumeration, StrIdValT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 11:44
 * Description: Модель допустимых запрашиваемых действий по отношению js ext.adv подсистеме со стороны сервера sio.
 */
object MAskActions {

  /** Значение поля mctx.action для запроса инициализации клиента. */
  def ENSURE_READY    = "a"

  /** Значение поля mctx.action для запроса обработки одной цели размещения. */
  def HANDLE_TARGET   = "b"

  /** Базовая инициализация системы без конкретных адаптеров. */
  def INIT            = "c"

  /** Запрос данных из хранилища. */
  def STORAGE_GET     = "d"

  /** Выставление данных в хранилище браузера. */
  def STORAGE_SET     = "e"

}


import MAskActions._


/** Абстрактный трейт будущего enum'а с допустимыми экшенами. */
trait MAskActionsBaseT extends ILightEnumeration with StrIdValT {

  trait ValT extends super.ValT {
    override val strId: String
  }

  override type T <: ValT

  val Init: T

  /** Запрос подготовки к работе в рамках доменов. */
  val EnsureReady: T

  /** Запрос публикации одной цели. */
  val HandleTarget: T

  /** Запрос на чтение хранилища. */
  val StorageGet: T

  /** Запрос на запись/стирание значение из хранилища. */
  val StorageSet: T

}


/** Трейт будущей реализации модели [[MAskActionsBaseT]] на базе scala.Enumeration. */
trait MAskActionsT extends Enumeration with EnumMaybeWithName with MAskActionsBaseT {
  protected class Val(val strId: String) extends super.Val(strId) with ValT

  override type T = Val

  override val Init: T          = new Val(INIT)
  override val EnsureReady: T   = new Val(ENSURE_READY)
  override val HandleTarget: T  = new Val(HANDLE_TARGET)
  override val StorageGet: T    = new Val(STORAGE_GET)
  override val StorageSet: T    = new Val(STORAGE_SET)
}


/** Трейт легковесной реализации модели [[MAskActionsBaseT]] для внедрения в js.
  * Реализация не зависит от scala.Enumeration, collections и т.д. */
trait MAskActionLightBaseT extends MAskActionsBaseT with LightEnumeration {
  override def maybeWithName(n: String): Option[T] = {
    n match {
      case HandleTarget.strId   => Some(HandleTarget)
      case EnsureReady.strId    => Some(EnsureReady)
      case Init.strId           => Some(Init)
      case StorageGet.strId     => Some(StorageGet)
      case StorageSet.strId     => Some(StorageSet)
      case _                    => None
    }
  }
}
