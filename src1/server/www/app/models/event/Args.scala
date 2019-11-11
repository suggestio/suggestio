package models.event

import java.{util => ju}

import io.suggest.common.empty.EmptyProduct
import io.suggest.event.SioNotifier.{Classifier, Event}
import io.suggest.ext.svc.MExtService
import io.suggest.model.n2.node.MNode
import io.suggest.util.JacksonParsing
import models.adv.MExtTarget
import models.blk
import play.api.libs.functional.syntax._
import play.api.libs.json._

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


object ArgNames {

  def AdnId          = "a"
  def AdvExtTarget   = "b"
  def AdId           = "c"
  //val PersonId     : T = new Val("")

}


trait IArgs {
  def nonEmpty: Boolean
  def isEmpty: Boolean
}


object IArgsInfo {

  import ArgNames._

  implicit def writes: Writes[IArgsInfo] = (
    (__ \ AdnId).writeNullable[String] and
    (__ \ AdvExtTarget).write[Iterable[String]] and
    (__ \ AdId).writeNullable[String]
  ){ s => (s.adnIdOpt, s.advExtTgIds, s.adIdOpt) }

}

/** Трейт для хранимых контейнеров аргументов: id'шники всякие тут. */
trait IArgsInfo extends IArgs with Event {
  
  /** Опциональный id узла, с которым связано это событие. */
  def adnIdOpt: Option[String]

  /** id цели внешнего размещения, если есть. */
  def advExtTgIds: Iterable[String]

  /** Опциональный id рекламной карточки, с которой связано это событие. */
  def adIdOpt: Option[String]

  override def nonEmpty: Boolean
  override def isEmpty: Boolean

  // TODO Надо что-то с advExtTgIdOpt => Classifier. Текущий headOption() может вызвать логические ошибки в будущем.
  override def getClassifier: Classifier = List(adIdOpt, adIdOpt, advExtTgIds.headOption)

}


/** Статическая сторона модели [[ArgsInfo]]. */
object ArgsInfo {

  import ArgNames._

  implicit def argsInfoFormat: OFormat[ArgsInfo] = (
    (__ \ AdnId).formatNullable[String] and
    (__ \ AdvExtTarget).format[Iterable[String]] and
    (__ \ AdId).formatNullable[String]
  )(apply, unlift(unapply))

  /** Исторически, для десериализации используется jackson. Тут костыли для десериализации из java Map. */
  def fromJacksonJson: PartialFunction[Any, ArgsInfo] = {
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        EmptyArgsInfo
      } else {
        val f = ArgsInfo(
          adnIdOpt      = Option(jm get AdnId).map(JacksonParsing.stringParser),
          advExtTgIds = Option(jm get AdvExtTarget)
            .iterator
            .flatMap(JacksonParsing.iteratorParser)
            .map(JacksonParsing.stringParser)
            .toSeq,
          adIdOpt       = Option(jm get AdId).map(JacksonParsing.stringParser)
        )
        // На случай появления мусора в карте...
        if (f.isEmpty)
          EmptyArgsInfo
        else
          f
      }

    case _ => EmptyArgsInfo
  }

  import io.suggest.es.util.SioEsUtil._

  /** Поля аргументов тоже индексируются. В первую очередь, чтобы их можно было легко и быстро удалять. */
  def generateMappingProps: List[DocField] = List(
    FieldKeyword(AdnId, index = true, include_in_all = false),
    FieldKeyword(AdvExtTarget, index = true, include_in_all = false),
    FieldKeyword(AdId, index = true, include_in_all = false)
  )

}


/**
 * Экземпляр того, что получается при десериализации набора MEvent.argsInfo.
 *
 * @param adnIdOpt Если событие связано с другим узлом, то тут id узла.
 * @param adIdOpt Если событие связано с рекламной карточкой, то тут id карточки.
 */
case class ArgsInfo(
  adnIdOpt        : Option[String]  = None,
  advExtTgIds     : Iterable[String]     = Nil,
  adIdOpt         : Option[String]  = None
)
  extends IArgsInfo
  with EmptyProduct


/** Общий инстанс для пустой инфы по аргументам. Нанооптимизация. */
object EmptyArgsInfo extends ArgsInfo() {
  // Нанооптимизация
  override def nonEmpty = false
}


/**
 * Контейнер для представления готовых инстансов аргументов. Он передаётся в шаблоны для рендера.
 * Заполняется контроллером перед рендером всех событий.
 *
 * @param mevent Экземпляр события [[IEvent]].
 * @param withContainer Рендерить обрамляющий контейнер? [false] Контейнер используется при первичном рендере,
 *                      чтобы его потом перезаписывать через js innerHTML() содержимое этого контейнера.
 * @param adnNodeOpt Инстанс ноды, если событие связано с нодой.
 * @param advExtTgs Инстанс MExtTarget, если событие связано с ним.
 * @param madOpt Инстанс MAd, если событие связано с этой рекламной карточкой.
 * @param extServiceOpt Инфа по сервису размещения.
 * @param errors Экземпляры ошибок, если событие отображает текущие ошибки.
 * @param brArgs Для рендера превьюшки карточки необходимо иметь на руках экземпляр block RenderArgs.
 */
case class RenderArgs(
  mevent        : IEvent,
  withContainer : Boolean             = false,
  adnNodeOpt    : Option[MNode]       = None,
  advExtTgs     : Seq[MExtTarget]     = Nil,
  madOpt        : Option[MNode]       = None,
  extServiceOpt : Option[MExtService] = None,
  brArgs        : Option[blk.RenderArgs] = None,
  errors        : Seq[IErrorInfo]     = Nil
)
  extends IArgsInfo
  with EmptyProduct
{

  def hasErrors = errors.nonEmpty

  override def adnIdOpt       = adnNodeOpt.flatMap(_.id)
  override def adIdOpt        = madOpt.flatMap(_.id)
  override def advExtTgIds    = advExtTgs.flatMap(_.id)

}


/** Интерфейс сообщения об ошибке. */
trait IErrorInfo {
  def msg  : String
  def args : Seq[Any]
  def info : Option[String]
}

/**
 * Инфа по ошибке.
 *
 * @param msg Сообщение или его код в messages.
 * @param args Параметры сообщения из messages, если есть/нужны.
 * @param info Какая-то доп.инфа по проблеме, если есть.
 */
case class ErrorInfo(
  msg   : String,
  args  : Seq[Any]        = Nil,
  info  : Option[String]  = None
) extends IErrorInfo

