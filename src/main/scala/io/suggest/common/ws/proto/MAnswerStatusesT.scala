package io.suggest.common.ws.proto

import io.suggest.common.menum.ILightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 11:40
 * Description: Абстрактная модель допустимых статусов ответов.
 * В слегка модифицированных видах используется на клиенте и на сервере.
 */
object AnswerStatusConstants {

  /** id статуса успеха. */
  def ST_SUCCESS        = "success"

  /** id статуса фатальной ошибки. */
  def ST_ERROR          = "error"

  /** id статуса запроса заполнения контекста недостающими данными. */
  def ST_FILL_CONTEXT   = "fillCtx"

}


/** Базовый интерфейс модели, независящий ни от чего. */
trait MAnswerStatusesBaseT extends ILightEnumeration {

  protected trait ValT extends super.ValT {
    def jsStr: String
    def isSuccess: Boolean  = false
    def isError: Boolean    = false
    override def toString   = jsStr
  }

  override type T <: ValT

  /** Аддон для быстрой сборки Success. */
  protected trait VSuccess extends ValT {
    override def isSuccess  = true
  }
  /** Аддон для быстрой сборки Error. */
  protected trait VError extends ValT {
    override def isError    = true
  }
  /** Аддон для быстрой сборки FillCtx. */
  protected trait VFillCtx extends ValT

  /** Действие исполнено полностью. */
  val Success: T

  /** Возникла ошибка при выполнении действия. Дальнейшее исполнение действия невозможно. */
  val Error: T

  /** В контексте недостаточно данных для выполнения действия.
    * Адресат должен проверить содержимое полученного контекста, найти и заполнить недостающие данные
    * и вернуть контекст отправителю.
    * Это НЕ ошибочное состояние, а часть нормального workflow в рамках какой-то конкретной задачи. */
  val FillContext: T

}
