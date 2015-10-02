package io.suggest.model.n2.edge

import io.suggest.common.EmptyProduct
import io.suggest.model.IGenEsMappingProps
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
object MNodeEdges extends IGenEsMappingProps {

  val OUT_FN = "out"

  val empty: MNodeEdges = {
    new MNodeEdges() {
      override def nonEmpty = false
    }
  }

  val EMAP_FORMAT: Format[NodeEdgesMap_t] = {
    __.format[Iterable[MNodeEdge]]
      .inmap [NodeEdgesMap_t] (
        {  _.iterator
            .map { e => e.toEmapKey -> e }
            .toMap
        },
        _.values
      )
  }

  implicit val FORMAT: Format[MNodeEdges] = {
    (__ \ OUT_FN).formatNullable[NodeEdgesMap_t]
      .inmap [NodeEdgesMap_t] (
        _ getOrElse Map.empty,
        {mnes => if (mnes.isEmpty) None else Some(mnes) }
      )
      .inmap [MNodeEdges](
        MNodeEdges.apply,
        _.out
      )
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNestedObject(OUT_FN, enabled = true, properties = MNodeEdge.generateMappingProps)
    )
  }

}


case class MNodeEdges(
  out   : Map[(MPredicate, String), MNodeEdge]    = Map.empty
)
  extends EmptyProduct
