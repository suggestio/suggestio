package io.suggest.adv.ext.model

import io.suggest.model.{LightEnumeration, ILightEnumeration, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 11:40
 * Description: Абстрактная модель допустимых статусов ответов.
 * В слегка модифицированных видах используется на клиенте и на сервере.
 */
object MAnswerStatuses {

  def ST_SUCCESS        = "success"
  def ST_ERROR          = "error"
  def ST_FILL_CONTEXT   = "fillCtx"

}


import MAnswerStatuses._


/** Базовый интерфейс модели, независящий ни от чего. */
trait MAnswerStatusesBaseT extends ILightEnumeration {

  protected trait ValT extends super.ValT {
    def jsStr: String
    def isSuccess: Boolean
    def isError: Boolean
    override def toString = jsStr
  }

  override type T <: ValT

  protected trait VSuccess extends ValT {
    override def isSuccess  = true
    override def isError    = false
  }
  protected trait VError extends ValT {
    override def isSuccess  = false
    override def isError    = false
  }
  protected trait VFillCtx extends ValT {
    override def isError    = false
    override def isSuccess  = false
  }

  /** Действие исполнено. */
  val Success: T

  /** Возникла ошибка при выполнении действия. Дальнейшее исполнение действия невозможно. */
  val Error: T

  /** Статус означает, что в контексте недостаточно данных для выполнения публикации.
    * Возможно не хватает картинки, нужны доп.карточки или ещё чего-то. */
  val FillContext: T

}


trait MAnswerStatusesT extends Enumeration with EnumMaybeWithName with MAnswerStatusesBaseT {

  /** Класс-заготовка одного инстанса модели. */
  protected abstract class Val(val jsStr: String) extends super.Val(jsStr) with ValT {
    def isSuccess: Boolean
    def isError: Boolean
  }

  override type T = Val

  override val Success: T = new Val(ST_SUCCESS) with VSuccess
  override val Error: T = new Val(ST_ERROR) with VError
  override val FillContext: T = new Val(ST_FILL_CONTEXT) with VFillCtx

}


/** Легковесная реализация enumeration'а без использования коллекций. */
trait MAnswerStatusesLightT extends MAnswerStatusesBaseT with LightEnumeration {

  protected sealed abstract class Val(val jsStr: String) extends super.ValT

  override type T = Val

  override val Success: T     = new Val(ST_SUCCESS) with VSuccess
  override val Error: T       = new Val(ST_ERROR) with VError
  override val FillContext: T = new Val(ST_FILL_CONTEXT) with VFillCtx

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Success.jsStr      => Some(Success)
      case Error.jsStr        => Some(Error)
      case FillContext.jsStr  => Some(FillContext)
      case _                  => None
    }
  }
}

