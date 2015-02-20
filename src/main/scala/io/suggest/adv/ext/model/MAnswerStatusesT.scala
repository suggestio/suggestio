package io.suggest.adv.ext.model

import io.suggest.model.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 11:40
 * Description: Абстрактная модель допустимых статусов ответов.
 * В слегка модифицированных видах используется на клиенте и на сервере.
 */
trait MAnswerStatusesT extends Enumeration with EnumMaybeWithName {

  /** Класс-заготовка одного инстанса модели. */
  protected abstract class Val(val jsStr: String) extends super.Val(jsStr) {
    def isSuccess: Boolean
    def isError: Boolean
  }

  type AnswerStatus = Val
  override type T = AnswerStatus

  val Success: AnswerStatus = new Val("success") {
    override def isSuccess  = true
    override def isError    = false
  }

  val Error: AnswerStatus   = new Val("error") {
    override def isSuccess  = false
    override def isError    = true
  }

  /** Статус означает, что в контексте недостаточно данных для выполнения публикации.
    * Возможно не хватает картинки, нужны доп.карточки или ещё чего-то. */
  val FillContext: AnswerStatus = new Val("fillCtx") {
    override def isSuccess  = false
    override def isError    = false
  }

}
