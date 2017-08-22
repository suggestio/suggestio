package io.suggest.model.n2.edge

import io.suggest.model.PrefixedFn
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.geo.MNodeGeoLevel
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 11:09
 * Description: Модель эджа, карта которых хранится внутри N2-узла [[io.suggest.model.n2.node.MNode]].
 *
 * Изначально, этот эдж был направленным ребром N2-графа. При этом сама направленность нигде не использовалась.
 * Потом, появилась параметризация эджа каким-то дополнительным payload'ом.
 * Затем, nodeId стал необязательным, и эдж стал некоей совсем абстрактной перечисляемой единицей данных,
 * которая в частном случае является ребром графа N2.
 * Теперь основная суть MEdge: описывать отношения узла с остальным миром (в широком смысле).
 */
object MEdge extends IGenEsMappingProps {

  /** Контейнер имён полей. */
  object Fields {

    val PREDICATE_FN  = "p"
    val NODE_ID_FN    = "i"
    val ORDER_FN      = "o"
    val INFO_FN       = "n"

    /** Модель названий полей, проброшенных сюда из под-моделей. */
    object Info extends PrefixedFn {

      override protected def _PARENT_FN = INFO_FN

      import MEdgeInfo.{Fields => F}

      def FLAG_FN       = _fullFn( F.FLAG_FN )

      // Теги
      def TAGS_FN       = _fullFn( F.TAGS_FN )
      def TAGS_RAW_FN   = _fullFn( F.Tags.TAGS_RAW_FN )

      // Гео-шейпы
      def INFO_GS_FN                            = _fullFn( F.GEO_SHAPES_FN )

      import F.{GeoShapes => Gs}
      def INFO_GS_GLEVEL_FN                     = _fullFn( Gs.GS_GLEVEL_FN )
      def INFO_GS_GJSON_COMPAT_FN               = _fullFn( Gs.GS_GJSON_COMPAT_FN )
      def INFO_GS_SHAPE_FN(ngl: MNodeGeoLevel)   = _fullFn( Gs.GS_SHAPE_FN(ngl) )

      // Гео-точки
      def INFO_GEO_POINTS_FN                    = _fullFn( F.GEO_POINT_FN )

    }

  }


  import Fields._

  /** JSON-маппер для поля nodeIds.
    * 2016.apr.1 nodeIds был Option[String], стал Set[String]. Поэтому тут reads с compat-костылём.
    */
  private val NODE_IDS_FORMAT = {
    val path = __ \ NODE_ID_FN

    val reads = path
      .readNullable[Set[String]]
      .map { _.getOrElse(Set.empty) }
      .orElse {
        path.readNullable[String]
          .map { _.toSet }
      }

    val writes = path.writeNullable[Set[String]]
      .contramap[Set[String]] { nodeIds =>
        if (nodeIds.nonEmpty) Some(nodeIds) else None
      }

    OFormat(reads, writes)
  }

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdge] = (
    (__ \ PREDICATE_FN).format(MPredicate.MPREDICATE_DEEP_FORMAT) and
    NODE_IDS_FORMAT and
    (__ \ ORDER_FN).formatNullable[Int] and
    (__ \ INFO_FN).formatNullable[MEdgeInfo]
      .inmap [MEdgeInfo] (
        opt2ImplMEmptyF( MEdgeInfo ),
        implEmpty2OptF
      )
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    // Все поля эджей должны быть include_in_all = false, ибо это сугубо техническая вещь.
    def fsNa(id: String) = FieldKeyword(id, index = true, include_in_all = false)
    List(
      fsNa(PREDICATE_FN),
      fsNa(NODE_ID_FN),
      // orderId -- not_analyzed, используется в т.ч. для хранения статистики использования геотегов, как это не странно...
      FieldNumber(ORDER_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = false),
      FieldObject(INFO_FN, enabled = true, properties = MEdgeInfo.generateMappingProps)
    )
  }

}



/** Реализация node edge-модели.
  *
  * @param predicate Предикат, т.е. некоторый тип эджа.
  * @param nodeIds id ноды на дальнем конце эджа, если есть.
  * @param order Для поддержкания порядка эджей можно использовать это опциональное поле.
  * Можно также использовать для некоего внутреннего доп.идентификатора.
  * @param info Контейнер доп.данных текущего эджа.
  */
case class MEdge(
                  predicate  : MPredicate,
                  // Обычно nodeId задан, поэтому без default тут для защиты от возможных ошибок.
                  nodeIds    : Set[String]    = Set.empty,
                  order      : Option[Int]    = None,
                  info       : MEdgeInfo      = MEdgeInfo.empty
                ) {

  override def toString: String = {
    val sb = new StringBuilder(64)
      .append("E(")
    sb.append(predicate.strId)
      .append(':')
    for (nodeId <- nodeIds) {
      sb.append(nodeId)
        .append(',')
    }
    for (ord <- order) {
      sb.append(':').append(ord)
    }
    if (info.nonEmpty) {
      sb.append('{')
        .append(info)
        .append('}')
    }
    sb.append(')')
      .toString()
  }

}
