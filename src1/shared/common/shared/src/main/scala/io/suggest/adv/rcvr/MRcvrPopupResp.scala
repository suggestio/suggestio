package io.suggest.adv.rcvr

import io.suggest.common.tree.{NodesTreeApiIId, NodesTreeWalk}
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.primo.id.IId
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:21
  * Description: Сериализуемые клиент-серверные модели содержимого попапа над ресивером на карте георазмещения.
  */

object MRcvrPopupResp {

  implicit def rcvrPopupRespJson: OFormat[MRcvrPopupResp] = {
    (__ \ "n").formatNullable[MRcvrPopupNode]
      .inmap[MRcvrPopupResp]( apply, _.node )
  }

  @inline implicit def univEq: UnivEq[MRcvrPopupResp] = UnivEq.derive
}

/** Модель ответа сервера на запрос попапа для ресивера.
  *
  * @param node Рекурсивная модель данных по размещению на узлу и под-узлах, если возможно.
  */
case class MRcvrPopupResp(
                           // TODO Нужно сделать Tree.Node(MRcvrPopupNode) вместо рекурсивных костылей с subGroups-полем.
                           node: Option[MRcvrPopupNode]
                         )


/** Поддержка для модели узлов и подузлов ресиверов в попапе ресивера. */
object MRcvrPopupNode extends NodesTreeApiIId with NodesTreeWalk {

  override type T = MRcvrPopupNode

  override protected def _subNodesOf(node: MRcvrPopupNode): Iterator[MRcvrPopupNode] = {
    node.subGroups
      .iterator
      .flatMap(_.nodes)
  }

  implicit def rcvrPopupNodeJson: OFormat[MRcvrPopupNode] = (
    (__ \ "i").format[String] and
    (__ \ "n").formatNullable[String] and
    (__ \ "c").formatNullable[MRcvrPopupMeta] and
    (__ \ "g").format[Seq[MRcvrPopupGroup]]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MRcvrPopupNode] = UnivEq.force

}

/** Описание узла с данными одного узла в ответе [[MRcvrPopupResp]]. */
final case class MRcvrPopupNode(
                                 id              : String,
                                 name            : Option[String],
                                 checkbox        : Option[MRcvrPopupMeta],
                                 subGroups       : Seq[MRcvrPopupGroup]
                               )
  extends IId[String]

object MRcvrPopupMeta {

  @inline implicit def univEq: UnivEq[MRcvrPopupMeta] = UnivEq.derive

  implicit def rcvrPopupMetaJson: OFormat[MRcvrPopupMeta] = (
    (__ \ "e").format[Boolean] and
    (__ \ "c").format[Boolean] and
    (__ \ "o").format[Boolean] and
    (__ \ "d").format[MRangeYmdOpt]
  )(apply, unlift(unapply))

}
/** Метаданные узла для размещения прямо на нём.
  *
  * @param isCreate true -- ожидается создание размещения.
  *                 false -- возможно удаление текущего размещения.
  * @param checked Исходное состояние галочки.
  * @param isOnlineNow Сейчас активен?
  * @param dateRange Диапазон дат размещения, если есть.
  *                  Если содержит хоть какое-то значение внутри, значит уже есть проплаченное срочное размещение.
  */
case class MRcvrPopupMeta(
                           isCreate        : Boolean,
                           checked         : Boolean,
                           isOnlineNow     : Boolean,
                           dateRange       : MRangeYmdOpt
                         )


/** Группа под-узлов.
  *
  * @param title Отображаемый заголовок группы, если требуется.
  * @param nodes Узлы данной группы, которые могут содержать подгруппы [[MRcvrPopupGroup]].
  */
case class MRcvrPopupGroup(
                            title  : Option[String],
                            nodes  : Seq[MRcvrPopupNode]
                          )
object MRcvrPopupGroup {

  @inline implicit def univEq: UnivEq[MRcvrPopupGroup] = UnivEq.derive

  implicit def rcvrPopupGroupJson: OFormat[MRcvrPopupGroup] = (
    (__ \ "t").formatNullable[String] and
    // TODO Нужно завернуть в Tree.Node(MRcvrPopupNode), и убрать этот класс-костыль.
    (__ \ "n").lazyFormat( implicitly[Format[Seq[MRcvrPopupNode]]] )
  )(apply, unlift(unapply))

}
