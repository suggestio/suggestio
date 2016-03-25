package io.suggest.model.n2.edge

import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.common.empty.EmptyUtil._
import io.suggest.ym.model.NodeGeoLevel
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

      def INFO_SLS_FN   = _fullFn( MEdgeInfo.Fields.SLS_FN )
      def FLAG_FN       = _fullFn( MEdgeInfo.Fields.FLAG_FN )

      // Теги
      def TAGS_FN       = _fullFn( MEdgeInfo.Fields.TAGS_FN )

      // Гео-шейпы
      def INFO_GS_FN                            = _fullFn( MEdgeInfo.Fields.GEO_SHAPES_FN )

      import MEdgeInfo.Fields.{GeoShapes => Gs}
      def INFO_GS_GLEVEL_FN                     = _fullFn( Gs.GS_GLEVEL_FN )
      def INFO_GS_GJSON_COMPAT_FN               = _fullFn( Gs.GS_GJSON_COMPAT_FN )
      def INFO_GS_SHAPE_FN(ngl: NodeGeoLevel)   = _fullFn( Gs.GS_SHAPE_FN(ngl) )

    }

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdge] = (
    (__ \ PREDICATE_FN).format(MPredicates.PARENTAL_OR_DIRECT_FORMAT) and
    (__ \ NODE_ID_FN).formatNullable[String] and
    (__ \ ORDER_FN).formatNullable[Int] and
    (__ \ INFO_FN).formatNullable[MEdgeInfo]
      .inmap [MEdgeInfo] (
        opt2ImplMEmptyF( MEdgeInfo ),
        implEmpty2OptF
      )
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    // Все поля эджей должны быть include_in_all = false, ибо это сугубо техническая вещь.
    def fsNa(id: String) = FieldString(id, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    List(
      fsNa(PREDICATE_FN),
      fsNa(NODE_ID_FN),
      FieldNumber(ORDER_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(INFO_FN, enabled = true, properties = MEdgeInfo.generateMappingProps)
    )
  }

}


/** Интерфейс экземпляров эдж-модели. */
trait IEdge {

  /** Предикат. */
  def predicate : MPredicate

  /** id ноды на дальнем конце эджа, если есть. */
  def nodeIdOpt : Option[String]

  /** Для поддержкания порядка эджей можно использовать это опциональное поле.
    * Можно также использовать для некоего внутреннего доп.идентификатора. */
  def order     : Option[Int]

  /** Какие-то доп.данные текущего ребра. */
  def info      : MEdgeInfo

  /** Сборка суффикса ключа эджа в карте эджей. */
  def _extraKeyData: EdgeXKey_t = {
    val k0 = info._extraKeyData
    order.fold(k0)(_ :: k0)
  }

  /** Сконвертить в инстанс ключа карты эджей. */
  def toEmapKey: NodeEdgesMapKey_t = {
    (predicate, nodeIdOpt, _extraKeyData)
  }

  override def toString: String = {
    val sb = new StringBuilder(64)
      .append("E(")
    sb.append(predicate.strId)
      .append(':')
    for (nodeId <- nodeIdOpt) {
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
    sb.append(',')
      .toString()
  }

}


/** Реализация node edge-модели. */
case class MEdge(
  override val predicate : MPredicate,
  // Обычно nodeId задан, поэтому без default тут для защиты от возможных ошибок.
  override val nodeIdOpt : Option[String] = None,
  override val order     : Option[Int]    = None,
  override val info      : MEdgeInfo      = MEdgeInfo.empty
)
  extends IEdge
