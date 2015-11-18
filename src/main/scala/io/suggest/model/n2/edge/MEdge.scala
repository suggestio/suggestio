package io.suggest.model.n2.edge

import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 11:09
 * Description: Один эдж, хранимый внутри N2-узла.
 * В задумке это был исходящий эдж к контексте узла-источкника.
 * Но в самом коде эта ассиметрия не отражена, чтобы можно было использовать модель и для входящего эджа.
 *
 * Ранее (до sio2:eb2e628b9061) модель MEdge была отдельно от [[io.suggest.model.n2.node.MNode]],
 * это вызовало ряд проблем и скорую денормализацию.
 */
object MEdge extends IGenEsMappingProps {

  /** Контейнер имён полей. */
  object Fields {

    val PREDICATE_FN  = "p"
    val NODE_ID_FN    = "i"
    val ORDER_FN      = "o"
    val INFO_FN       = "n"

    object Info extends PrefixedFn {
      override protected def _PARENT_FN = INFO_FN

      def INFO_SLS_FN   = _fullFn( MEdgeInfo.SLS_FN )
      def FLAG_FN       = _fullFn( MEdgeInfo.FLAG_FN )
    }

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdge] = (
    (__ \ PREDICATE_FN).format(MPredicates.PARENTAL_OR_DIRECT_FORMAT) and
    (__ \ NODE_ID_FN).format[String] and
    (__ \ ORDER_FN).formatNullable[Int] and
    (__ \ INFO_FN).formatNullable[MEdgeInfo]
      .inmap [MEdgeInfo] (
        _ getOrElse MEdgeInfo.empty,
        { mei => if (mei.isEmpty) None else Some(mei) }
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

  /** id ноды на дальнем конце эджа. Это скорее всего toId, в рамках модели это не важно. */
  def nodeId    : String

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
    (predicate, nodeId, _extraKeyData)
  }

  override def toString: String = {
    val sb = new StringBuilder(64)
    sb.append(predicate.strId)
      .append('/')
      .append(nodeId)
    for (ord <- order) {
      sb.append(':').append(ord)
    }
    if (info.nonEmpty) {
      sb.append('{')
        .append(info)
        .append('}')
    }
    sb.toString()
  }

}


/** Реализация node edge-модели. */
case class MEdge(
  override val predicate : MPredicate,
  override val nodeId    : String,
  override val order     : Option[Int] = None,
  override val info      : MEdgeInfo   = MEdgeInfo.empty
)
  extends IEdge
