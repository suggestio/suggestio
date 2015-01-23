package models.event

import io.suggest.model.EnumMaybeWithName
import play.api.libs.json.{JsObject, JsValue, JsString}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 11:22
 * Description: Разные нотификации имеют разные наборы аргументов для рендера и разные типы.
 * Но все они должны жить в одной модели, при это работая в режиме статической типизации.
 * Тут файл с моделями, которые описывают всё это необъятное множество.
 *
 * У нас есть два уровня аргументов нотификации:
 *  - Контейнеры с хранимыми в [[MEvent]] id'шниками: [[IArgsInfo]]
 *  - Контейнеры с готовыми к рендеру интансами: [[IArgsInst]]
 */


object ArgNames extends Enumeration with EnumMaybeWithName {
  protected class Val(val strId: String) extends super.Val(strId)

  type ArgName = Val
  override type T = ArgName

  val AdnId     = new Val("a")

  val PersonId  = new Val("p")
  val AdvId     = new Val("v")

}


trait IArgs {
  def nonEmpty: Boolean
  def isEmpty: Boolean
}


/** Трейт для хранимых контейнеров аргументов: id'шники всякие тут. */
trait IArgsInfo extends IArgs {
  
  /** Сериализовать данные в json. */
  def toPlayJson: JsObject

  /**
   * Отфетчить все аргументы из хранилища, используя вспомогательную карту уже отфеченных инстансов.
   * @param runtimeArgs Карта с уже имеющимися аргументами, если есть.
   * @return Фьючерс с аргументами.
   */
  def toArgs(runtimeArgs: Map[ArgName, AnyRef] = Map.empty): Future[IArgsInst]
}


/** Трейт для контейнеров готовых инстансов аргументов. */
trait IArgsInst extends IArgs {

  /**
   * Упростить до [[IArgsInfo]].
   * @return Экземпляр реализации [[IArgsInfo]].
   */
  def toInfo: IArgsInfo
}


// Пустые аргументы. Инстанс на всех один.
sealed trait IEmptyArgs extends IArgs {
  override def nonEmpty = false
  override def isEmpty = true
}
case object EmptyArgsInst extends IArgsInst with IEmptyArgs {
  override def toInfo = EmptyArgsInfo
}
case object EmptyArgsInfo extends IArgsInfo with IEmptyArgs {
  override def toPlayJson = JsObject(Nil)
  override def toArgs(runtimeArgs: Map[ArgName, AnyRef]) = {
    Future successful EmptyArgsInst
  }
}

