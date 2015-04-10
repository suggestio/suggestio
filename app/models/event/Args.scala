package models.event

import io.suggest.event.SioNotifier.{Classifier, Event}
import io.suggest.model.{EsModel, EnumMaybeWithName}
import models.adv.MExtTarget
import models._
import models.mext.MExtService
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
  val AdvOkId         = new Val("d")
  val AdvReqId        = new Val("e")
  val AdvRefuseId     = new Val("f")
  //val PersonId      = new Val("")

}


trait IArgs {
  def nonEmpty: Boolean
  def isEmpty: Boolean
}


object IArgsInfo {

  import ArgNames._

  implicit def writes: Writes[IArgsInfo] = (
    (__ \ AdnId.strId).writeNullable[String] and
    (__ \ AdvExtTarget.strId).write[Seq[String]] and
    (__ \ AdId.strId).writeNullable[String] and
    (__ \ AdvOkId.strId).writeNullable[Int] and
    (__ \ AdvReqId.strId).writeNullable[Int] and
    (__ \ AdvRefuseId.strId).writeNullable[Int]
  ){ s => (s.adnIdOpt, s.advExtTgIds, s.adIdOpt, s.advOkIdOpt, s.advReqIdOpt, s.advRefuseIdOpt) }

}

/** Трейт для хранимых контейнеров аргументов: id'шники всякие тут. */
trait IArgsInfo extends IArgs with Event {
  
  /** Опциональный id узла, с которым связано это событие. */
  def adnIdOpt: Option[String]

  /** id цели внешнего размещения, если есть. */
  def advExtTgIds: Seq[String]

  /** Опциональный id рекламной карточки, с которой связано это событие. */
  def adIdOpt: Option[String]

  /** id размещения по моделям adv. */
  def advOkIdOpt: Option[Int]
  def advReqIdOpt: Option[Int]
  def advRefuseIdOpt: Option[Int]

  override def nonEmpty: Boolean
  override def isEmpty: Boolean

  // TODO Надо что-то с advExtTgIdOpt => Classifier. Текущий headOption() может вызвать логические ошибки в будущем.
  override def getClassifier: Classifier = List(adIdOpt, adIdOpt, advExtTgIds.headOption)

}


/** Статическая сторона модели [[ArgsInfo]]. */
object ArgsInfo {

  import ArgNames._

  implicit def reads: Reads[ArgsInfo] = (
    (__ \ AdnId.strId).readNullable[String] and
    (__ \ AdvExtTarget.strId).read[Seq[String]] and
    (__ \ AdId.strId).readNullable[String] and
    (__ \ AdvOkId.strId).readNullable[Int] and
    (__ \ AdvReqId.strId).readNullable[Int] and
    (__ \ AdvRefuseId.strId).readNullable[Int]
  )(apply _)

  /** Исторически, для десериализации используется jackson. Тут костыли для десериализации из java Map. */
  def fromJacksonJson: PartialFunction[Any, ArgsInfo] = {
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        EmptyArgsInfo
      } else {
        val f = ArgsInfo(
          adnIdOpt      = Option(jm get AdnId.strId).map(EsModel.stringParser),
          advExtTgIds = Option(jm get AdvExtTarget.strId)
            .iterator
            .flatMap(EsModel.iteratorParser)
            .map(EsModel.stringParser)
            .toSeq,
          adIdOpt       = Option(jm get AdId.strId).map(EsModel.stringParser),
          advOkIdOpt    = Option(jm get AdvOkId.strId).map(EsModel.intParser),
          advReqIdOpt   = Option(jm get AdvReqId.strId).map(EsModel.intParser),
          advRefuseIdOpt = Option(jm get AdvRefuseId.strId).map(EsModel.intParser)
        )
        // На случай появления мусора в карте...
        if (f.isEmpty)
          EmptyArgsInfo
        else
          f
      }

    case _ => EmptyArgsInfo
  }

  import io.suggest.util.SioEsUtil._

  /** Поля аргументов тоже индексируются. В первую очередь, чтобы их можно было легко и быстро удалять. */
  def generateMappingProps: List[DocField] = List(
    FieldString(AdnId.strId, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(AdvExtTarget.strId, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(AdId.strId, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldNumber(AdvOkId.strId, DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldNumber(AdvReqId.strId, DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldNumber(AdvRefuseId.strId, DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  )

}


/**
 * Экземпляр того, что получается при десериализации набора MEvent.argsInfo.
 * @param adnIdOpt Если событие связано с другим узлом, то тут id узла.
 * @param adIdOpt Если событие связано с рекламной карточкой, то тут id карточки.
 */
case class ArgsInfo(
  adnIdOpt        : Option[String]  = None,
  advExtTgIds   : Seq[String]     = Nil,
  adIdOpt         : Option[String]  = None,
  advOkIdOpt      : Option[Int]     = None,
  advReqIdOpt     : Option[Int]     = None,
  advRefuseIdOpt  : Option[Int]     = None
) extends IArgsInfo with EmptyProduct {

}


/** Общий инстанс для пустой инфы по аргументам. Нанооптимизация. */
object EmptyArgsInfo extends ArgsInfo() {
  // Нанооптимизация
  override def isEmpty = true
  override def nonEmpty = false
}


/**
 * Контейнер для представления готовых инстансов аргументов. Он передаётся в шаблоны для рендера.
 * Заполняется контроллером перед рендером всех событий.
 * @param mevent Экземпляр события [[IEvent]].
 * @param withContainer Рендерить обрамляющий контейнер? [false] Контейнер используется при первичном рендере,
 *                      чтобы его потом перезаписывать через js innerHTML() содержимое этого контейнера.
 * @param adnNodeOpt Инстанс ноды, если событие связано с нодой.
 * @param advExtTgs Инстанс MExtTarget, если событие связано с ним.
 * @param madOpt Инстанс MAd, если событие связано с этой рекламной карточкой.
 * @param extServiceOpt Инфа по сервису размещения.
 * @param errors Экземпляры ошибок, если событие отображает текущие ошибки.
 */
case class RenderArgs(
  mevent        : IEvent,
  withContainer : Boolean             = false,
  adnNodeOpt    : Option[MAdnNode]    = None,
  advExtTgs     : Seq[MExtTarget]     = Nil,
  madOpt        : Option[MAd]         = None,
  advOkOpt      : Option[MAdvOk]      = None,
  advReqOpt     : Option[MAdvReq]     = None,
  advRefuseOpt  : Option[MAdvRefuse]  = None,
  extServiceOpt : Option[MExtService] = None,
  errors        : Seq[IErrorInfo]     = Nil
) extends IArgsInfo with EmptyProduct {

  def hasErrors = errors.nonEmpty

  override def adnIdOpt       = adnNodeOpt.flatMap(_.id)
  override def adIdOpt        = madOpt.flatMap(_.id)
  override def advExtTgIds    = advExtTgs.flatMap(_.id)

  override def advOkIdOpt     = advOkOpt.flatMap(_.id)
  override def advReqIdOpt    = advReqOpt.flatMap(_.id)
  override def advRefuseIdOpt = advRefuseOpt.flatMap(_.id)

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


/** Интерфейс сообщения об ошибке. */
trait IErrorInfo {
  def msg  : String
  def args : Seq[Any]
  def info : Option[String]
}

/**
 * Инфа по ошибке.
 * @param msg Сообщение или его код в messages.
 * @param args Параметры сообщения из messages, если есть/нужны.
 * @param info Какая-то доп.инфа по проблеме, если есть.
 */
case class ErrorInfo(
  msg   : String,
  args  : Seq[Any]        = Nil,
  info  : Option[String]  = None
) extends IErrorInfo

