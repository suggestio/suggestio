package io.suggest.jd.tags.event

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.jd.JdConst
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Validation
import scalaz.syntax.validation._
import scalaz.syntax.apply._
import scalaz.std.list._
import scalaz.std.iterable._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.04.2021 12:06
  * Description: Система описания реакции на какие-либо события для jd-тегов.
  */
object MJdtEvents extends IEmpty {

  override type T = MJdtEvents
  override val empty = apply()

  object Fields {
    def EVENTS = "e"
  }

  @inline implicit def univEq: UnivEq[MJdtEvents] = UnivEq.derive

  def events = GenLens[MJdtEvents](_.events)

  implicit def jdtEventsJson: OFormat[MJdtEvents] = {
    val F = Fields
    (__ \ F.EVENTS)
      .formatNullable[Seq[MJdtEventActions]]
      .inmap[Seq[MJdtEventActions]](
        EmptyUtil.opt2ImplEmptyF(Nil),
        events => Option.when(events.nonEmpty)(events)
      )
      .inmap(apply, _.events)
  }

  implicit def validate(jdEvents: MJdtEvents): StringValidationNel[MJdtEvents] = {
    ScalazUtil
      .validateAll( jdEvents.events )( MJdtEventActions.validate(_).map(_ :: Nil) )
      .andThen {
        Validation.liftNel(_)( _.lengthIs > JdConst.Event.MAX_EVENT_LISTENERS_PER_TAG, Fields.EVENTS )
      }
      .map( apply )
  }

}
/** Общий контейнер данных по обработке событий. */
final case class MJdtEvents(
                             events: Seq[MJdtEventActions] = Nil,
                           )
  extends EmptyProduct



object MJdtEventTypes extends StringEnum[MJdtEventType] {
  case object Click extends MJdtEventType( "c" )

  override def values = findValues
}
sealed abstract class MJdtEventType(override val value: String) extends StringEnumEntry
object MJdtEventType {
  @inline implicit def univEq: UnivEq[MJdtEventType] = UnivEq.derive
  implicit def jdtEventTypeFormat: Format[MJdtEventType] =
    EnumeratumUtil.valueEnumEntryFormat( MJdtEventTypes )
}


/** Контейнер из критериев события и связанных с событием экшенов. */
final case class MJdtEventActions(
                                   event          : MJdtEventInfo,
                                   actions        : List[MJdtAction],
                                 )
object MJdtEventActions {

  object Fields {
    def EVENT_TYPE = "t"
    def ACTIONS = "a"
  }

  def event = GenLens[MJdtEventActions](_.event)
  def actions = GenLens[MJdtEventActions](_.actions)

  @inline implicit def univEq: UnivEq[MJdtEventActions] = UnivEq.derive

  implicit def jdtEventJson: Format[MJdtEventActions] = {
    val F = Fields
    (
      (__ \ F.EVENT_TYPE).format[MJdtEventInfo] and
      (__ \ F.ACTIONS).format[List[MJdtAction]]
    )(apply, unlift(unapply))
  }

  def validate(e: MJdtEventActions): StringValidationNel[MJdtEventActions] = {
    (
      MJdtEventInfo.validate( e.event ) |@|
      ScalazUtil
        .validateAll( e.actions )( MJdtAction.validate(_).map(_ :: Nil) )
        .andThen {
          Validation.liftNel(_)(_.lengthIs > JdConst.Event.MAX_ACTIONS_PER_LISTENER, Fields.ACTIONS)
        }
    )(apply)
  }

}

/** Критерии события, на которое подписка.
  *
  * @param eventType Тип перехватываемого события.
  */
final case class MJdtEventInfo(
                                eventType      : MJdtEventType,
                              )
object MJdtEventInfo {

  object Fields {
    final def EVENT_TYPE = "t"
  }

  @inline implicit def univEq: UnivEq[MJdtEventInfo] = UnivEq.derive

  def eventType = GenLens[MJdtEventInfo](_.eventType)

  implicit def jdtEventInfoJson: Format[MJdtEventInfo] = {
    val F = Fields
    (__ \ F.EVENT_TYPE)
      .format[MJdtEventType]
      .inmap( apply, _.eventType )
  }

  def validate(v: MJdtEventInfo): StringValidationNel[MJdtEventInfo] = {
    // Пока нечего проверять: вернуть исходный инстанс целиком.
    v.successNel
  }

}


/** Описание действия при событии.
  *
  * @param action Тип действия, которое требуется совершить.
  * @param edgeUids Эджи, связанные с совершаемым действием.
  */
final case class MJdtAction(
                             action        : MJdActionType,
                             edgeUids      : List[EdgeUid_t] = Nil,
                           )
object MJdtAction {

  object Fields {
    def ACTION_FN = "a"
    def EDGE_UIDS = "e"
  }

  def action = GenLens[MJdtAction](_.action)
  def edgeUids = GenLens[MJdtAction](_.edgeUids)

  @inline implicit def univEq: UnivEq[MJdtAction] = UnivEq.derive
  implicit def jdtActionJson: Format[MJdtAction] = {
    val F = Fields
    (
      (__ \ F.ACTION_FN).format[MJdActionType] and
      (__ \ F.EDGE_UIDS).format[List[EdgeUid_t]]
    )(apply, unlift(unapply))
  }

  def validate(a: MJdtAction): StringValidationNel[MJdtAction] = {
    // Пока всё минимально: одно действие, максимум одна карточка.
    (
      a.action.successNel[String] |@|
      Validation.liftNel( a.edgeUids )( _.lengthIs <= 1, Fields.EDGE_UIDS )
    )( apply )
  }

}


/** Типы действий. */
object MJdActionTypes extends StringEnum[MJdActionType] {
  /** Вставить карточку в плитку. */
  case object InsertAds extends MJdActionType("a")

  override def values = findValues
}
/** Тип действия. */
sealed abstract class MJdActionType(override val value: String) extends StringEnumEntry

object MJdActionType {
  @inline implicit def univEq: UnivEq[MJdActionType] = UnivEq.derive
  implicit def jdActionTypeJson: Format[MJdActionType] =
    EnumeratumUtil.valueEnumEntryFormat( MJdActionTypes )
}