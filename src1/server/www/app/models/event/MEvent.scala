package models.event

import java.time.OffsetDateTime

import io.suggest.event.SioNotifier.{Classifier, Event}
import io.suggest.es.model._
import io.suggest.primo.id.OptStrId

// TODO Модель оставлена тут только для совместимости с legacy-кодом из lk-adv-ext.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.15 19:46
 * Description: Модель, описывающая события для узла или другого объекта системы suggest.io.
 *
 * 2015.feb.12: Поле isUnseen работает по значениям: true или missing. Это позволяет разгружать индекс поля, когда
 * сообщения прочитаны (а таких большинство, и искать по этому значение не требуется).
 */
object MEvent {

  def TTL_DAYS_UNSEEN = 90

  def isCloseableDflt = true
  def dateCreatedDflt = OffsetDateTime.now()
  def argsDflt        = EmptyArgsInfo


  /**
   * Сборка event classifier для простоты взаимодействия с SioNotifier.
   *
   * @param etype Тип события, если нужен.
   * @param ownerId id владельца, если требуется.
   * @param argsInfo Экземпляр [[ArgsInfo]], если есть.
   * @return Classifier.
   */
  def getClassifier(etype: Option[MEventType], ownerId: Option[String], argsInfo: ArgsInfo = EmptyArgsInfo): Classifier = {
    Some(classOf[MEvent].getSimpleName) ::
      etype ::
      ownerId ::
      argsInfo.getClassifier
  }

}


/** Класс-экземпляр одной нотификации. */
case class MEvent(
                   etype         : MEventType,
                   ownerId       : String,
                   argsInfo      : ArgsInfo        = MEvent.argsDflt,
                   dateCreated   : OffsetDateTime  = MEvent.dateCreatedDflt,
                   isCloseable   : Boolean         = MEvent.isCloseableDflt,
                   isUnseen      : Boolean         = true,
                   ttlDays       : Option[Int]     = Some(MEvent.TTL_DAYS_UNSEEN),
                   id            : Option[String]  = None,
                   versioning    : EsDocVersion    = EsDocVersion.empty,
)
  extends EsModelT
  with IMEvent



/** Минимальный интерфейс абстрактного события.
  * Используется для рендера шаблонов, аргументы которых абстрагированы от конкретной реализации. */
trait IEvent extends OptStrId with Event {
  def etype         : MEventType
  def ownerId       : String
  def argsInfo      : ArgsInfo
  def dateCreated   : OffsetDateTime
  def isCloseable   : Boolean
  def isUnseen      : Boolean
}

/** Частичная реализация [[IEvent]] с реализацией getClassifier() в рамках текущей модели. */
trait IMEvent extends IEvent {
  override def getClassifier: Classifier = {
    MEvent.getClassifier(etype = Some(etype), ownerId = Some(ownerId), argsInfo = argsInfo)
  }
}


/**
 * Реализация [[IEvent]] для нехранимого события, т.е. когда что-то нужно отрендерить в режиме БЫСТРАБЛДЖАД!
 *
 * @param etype Тип события.
 * @param ownerId id owner'а.
 * @param argsInfo Необязательная инфа по параметрам [[EmptyArgsInfo]].
 * @param dateCreated Дата создания [now].
 * @param isCloseable Закрывабельность [false].
 * @param isUnseen Юзер в первый раз видит событие? [конечно true].
 */
case class MEventTmp(
  etype       : MEventType,
  ownerId     : String,
  argsInfo    : ArgsInfo        = EmptyArgsInfo,
  dateCreated : OffsetDateTime  = OffsetDateTime.now(),
  isCloseable : Boolean         = false,
  isUnseen    : Boolean         = true,
  id          : Option[String]  = None
) extends IMEvent


