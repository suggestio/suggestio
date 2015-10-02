package io.suggest.model.n2.edge

import io.suggest.common.EmptyProduct
import io.suggest.model.{PrefixedFn, IGenEsMappingProps}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 11:00
 * Description: Подмодель для [[io.suggest.model.n2.node.MNode]] для хранения эджей прямо
 * внутри узла. В изначальной задумке N2-архитектуры модель MEdge была полностью отдельной,
 * а до N2 эджи по суди описывались отдельными конкретными полями.
 * MEdge создало проблемы с транзакционным обновлением, когда индексы эджей не успевали обновится
 * при рендере ответа формы (POST-RDR-GET), ответ отображал наполовину старые данные.
 * Так же, это затрудняет поиск.
 *
 * Обновлённый вариант N2-архитектуры денормализует MEdge в MNode, т.е. по сути исходящие
 * эджи возвращаются внутрь моделей, из которых они исходят. Это как бы золотая середина
 * для исходной архитектуры и новой.
 */
object MNodeEdges extends IGenEsMappingProps with PrefixedFn {

  val OUT_FN = "out"
  override protected def _PARENT_FN = OUT_FN

  // Префиксируем поля в out-объектах.
  def OUT_PREDICATE_FN  = _fullFn( MEdge.PREDICATE_FN )
  def OUT_NODE_ID_FN    = _fullFn( MEdge.NODE_ID_FN )
  def OUT_ORDER_FN      = _fullFn( MEdge.ORDER_FN )

  val empty: MNodeEdges = {
    new MNodeEdges() {
      override def nonEmpty = false
    }
  }

  val EMAP_FORMAT: Format[NodeEdgesMap_t] = Format(
    Reads.of[Iterable[MEdge]]
      .map[NodeEdgesMap_t] { edges =>
        edges
          .iterator
          .map { e => e.toEmapKey -> e }
          .toMap
      },
    Writes[NodeEdgesMap_t] { emap =>
      Json.toJson( emap.values )
    }
  )

  implicit val FORMAT: Format[MNodeEdges] = {
    (__ \ OUT_FN).formatNullable[NodeEdgesMap_t]
      // Приведение опциональной карты к неопциональной.
      .inmap [NodeEdgesMap_t] (
        _ getOrElse Map.empty,
        {mnes => if (mnes.isEmpty) None else Some(mnes) }
      )
      // Вместо apply используем inmap, т.к. только одно поле тут.
      .inmap [MNodeEdges](
        MNodeEdges.apply,
        _.out
      )
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNestedObject(OUT_FN, enabled = true, properties = MEdge.generateMappingProps)
    )
  }

}


case class MNodeEdges(
  out   : Map[(MPredicate, String), MEdge]    = Map.empty
)
  extends EmptyProduct
