package io.suggest.model.n2.edge

import io.suggest.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 11:09
 * Description: Один эдж, хранимый внутри N2-узла.
 * В задумке это был исходящий эдж к контексте узла-источкника.
 * Но в самом коде эта ассиметрия не отражена, чтобы можно было использовать модель и для входящего эджа.
 */
object MNodeEdge extends IGenEsMappingProps {

  val PREDICATE_FN  = "p"
  val NODE_ID_FN    = "i"
  val ORDER_FN      = "o"

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MNodeEdge] = (
    (__ \ PREDICATE_FN).format[MPredicate] and
    (__ \ NODE_ID_FN).format[String] and
    (__ \ ORDER_FN).formatNullable[Int]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    // Все поля эджей должны быть include_in_all = false, ибо это сугубо техническая вещь.
    def fsNa(id: String) = FieldString(id, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    List(
      fsNa(PREDICATE_FN),
      fsNa(NODE_ID_FN),
      FieldNumber(ORDER_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/** Интерфейс экземпляров эдж-модели. */
trait INodeEdge {

  /** Предикат. */
  def predicate : MPredicate
  /** id ноды на дальнем конце эджа. Это скорее всего toId, в рамках модели это не важно. */
  def nodeId    : String
  /** Для поддержкания порядка эджей можно использовать это опциональное поле. */
  def order     : Option[Int]

  /** Сконвертить в инстанс ключа карты эджей. */
  def toEmapKey: NodeEdgesMapKey_t = {
    (predicate, nodeId)
  }

}


/** Реализация node edge-модели. */
case class MNodeEdge(
  override val predicate : MPredicate,
  override val nodeId    : String,
  override val order     : Option[Int] = None
)
  extends INodeEdge
