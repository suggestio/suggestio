package io.suggest.sc.ads

import io.suggest.common.empty.EmptyUtil
import io.suggest.n2.edge.{MEdge, MPredicate}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.n2.node.MNodeType
import io.suggest.text.StringUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.03.2020 14:52
  * Description: Информация по найденным данных.
  */
object MScAdMatchInfo {

  /** Поддержка play-json. */
  implicit def scAdMatchInfoJson: OFormat[MScAdMatchInfo] = (
    (__ \ MEdge.Fields.PREDICATE_FN).formatNullable[Set[MPredicate]]
      .inmap[Set[MPredicate]](
        EmptyUtil.opt2ImplEmptyF( Set.empty ),
        { preds => Option.when(preds.nonEmpty)( preds ) }
      ) and
    (__ \ MEdge.Fields.NODE_ID_FN).formatNullable[Seq[MScNodeMatchInfo]]
      .inmap[Seq[MScNodeMatchInfo]](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        { nodeIds => Option.when(nodeIds.nonEmpty)( nodeIds ) }
      )
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MScAdMatchInfo] = UnivEq.derive

}


/** Контейнер данных по сработавшим узлам.
  *
  * @param predicates Предикат, если совпал.
  * @param nodeMatchings Данные по узлам.
  */
case class MScAdMatchInfo(
                           predicates         : Set[MPredicate]           = Set.empty,
                           nodeMatchings      : Seq[MScNodeMatchInfo]     = Nil,
                         ) {
  override def toString = StringUtil.toStringHelper(null) { f =>
    if (predicates.nonEmpty) f("pred")(predicates)
    if (nodeMatchings.nonEmpty) f("node")(nodeMatchings)
  }
}


object MScNodeMatchInfo {
  implicit def nodeMatchInfo: OFormat[MScNodeMatchInfo] = (
    (__ \ "n").formatNullable[String] and
    (__ \ "t").formatNullable[MNodeType]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MScNodeMatchInfo] = UnivEq.derive
}
/** Инфа по поисковому матчингу одного узла. */
case class MScNodeMatchInfo(
                             nodeId   : Option[String]    = None,
                             ntype    : Option[MNodeType] = None,
                           ) {
  override def toString = StringUtil.toStringHelper( null ) { f =>
    nodeId foreach f("id")
    ntype foreach f("ntype")
  }
}
