package models.event

import io.suggest.model.{EsModel, EnumMaybeWithName}
import models.adv.MExtTarget
import models.{MAd, MAdnNode}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 11:22
 * Description: Разные нотификации имеют разные наборы аргументов для рендера и разные типы.
 * Но все они должны жить в одной модели, при это работая в режиме статической типизации.
 * Тут файл с моделями, которые описывают всё это необъятное множество.
 *
 * У нас есть два уровня аргументов нотификации:
 *  - Контейнеры с хранимыми в [[MEvent]] id'шниками моделей: [[IArgsInfo]]
 *  - Контейнеры данных с готовыми к рендеру интансами моделей: [[RenderArgs]]
 */


object ArgNames extends Enumeration with EnumMaybeWithName {
  protected class Val(val strId: String) extends super.Val(strId)

  type ArgName = Val
  override type T = ArgName

  val AdnId           = new Val("a")
  val AdvExtTarget    = new Val("b")
  val AdId            = new Val("c")
  //val PersonId      = new Val("d")
  //val AdvId         = new Val("e")

}


trait IArgs {
  def nonEmpty: Boolean
  def isEmpty: Boolean
}


object IArgsInfo {

  import ArgNames._

  implicit def writes: Writes[IArgsInfo] = (
    (__ \ AdnId.strId).writeNullable[String] and
    (__ \ AdvExtTarget.strId).writeNullable[String] and
    (__ \ AdId.strId).writeNullable[String]
  ){ s => (s.adnIdOpt, s.advExtTgIdOpt, s.adIdOpt) }

}

/** Трейт для хранимых контейнеров аргументов: id'шники всякие тут. */
trait IArgsInfo extends IArgs {
  
  /** Опциональный id узла, с которым связано это событие. */
  def adnIdOpt: Option[String]

  /** id цели внешнего размещения, если есть. */
  def advExtTgIdOpt: Option[String]

  /** Опциональный id рекламной карточки, с которой связано это событие. */
  def adIdOpt: Option[String]

  override def nonEmpty: Boolean
  override def isEmpty: Boolean

}


/** Статическая сторона модели [[ArgsInfo]]. */
object ArgsInfo {

  import ArgNames._

  implicit def reads: Reads[ArgsInfo] = (
    (__ \ AdnId.strId).readNullable[String] and
    (__ \ AdvExtTarget.strId).readNullable[String] and
    (__ \ AdId.strId).readNullable[String]
  )(apply _)

  /** Исторически, для десериализации используется jackson. Тут костыли для десериализации из java Map. */
  def fromJacksonJson: PartialFunction[Any, ArgsInfo] = {
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        EmptyArgsInfo
      } else {
        val f = ArgsInfo(
          adnIdOpt      = Option(jm get AdnId.strId).map(EsModel.stringParser),
          advExtTgIdOpt = Option(jm get AdvExtTarget.strId).map(EsModel.stringParser),
          adIdOpt       = Option(jm get AdId.strId).map(EsModel.stringParser)
        )
        // На случай появления мусора в карте...
        if (f.isEmpty)
          EmptyArgsInfo
        else
          f
      }

    case _ => EmptyArgsInfo
  }

}


/**
 * Экземпляр того, что получается при десериализации набора MEvent.argsInfo.
 * @param adnIdOpt Если событие связано с другим узлом, то тут id узла.
 * @param adIdOpt Если событие связано с рекламной карточкой, то тут id карточки.
 */
case class ArgsInfo(
  adnIdOpt        : Option[String]  = None,
  advExtTgIdOpt   : Option[String]  = None,
  adIdOpt         : Option[String]  = None
) extends IArgsInfo with EmptyProduct


/** Общий инстанс для пустой инфы по аргументам. Нанооптимизация. */
object EmptyArgsInfo extends ArgsInfo() {
  // Нанооптимизация
  override def isEmpty = true
  override def nonEmpty = false
}


/** Контейнер для представления готовых инстансов аргументов. Он передаётся в шаблоны для рендера.
  * Заполняется контроллером перед рендером всех событий. */
case class RenderArgs(
  mevent      : IEvent,
  adnNodeOpt  : Option[MAdnNode]    = None,
  advExtTgOpt : Option[MExtTarget]  = None,
  madOpt      : Option[MAd]         = None
) extends IArgsInfo with EmptyProduct {

  override def adnIdOpt = adnNodeOpt.flatMap(_.id)
  override def adIdOpt  = madOpt.flatMap(_.id)
  override def advExtTgIdOpt = advExtTgOpt.flatMap(_.id)

}


/** Вынести куда-нить... */
trait EmptyProduct extends Product {
  def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_]           => opt.nonEmpty
      case col: TraversableOnce[_]  => col.nonEmpty
      case _                        => true
    }
  }
  def isEmpty: Boolean = !nonEmpty
}


