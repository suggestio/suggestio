package io.suggest.model.n2.node.common

import io.suggest.model.n2.node.MNodeType
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 14:41
 * Description: Модель общих для всех N2-узлов полей [[io.suggest.model.n2.node.MNode]].
 */
object MNodeCommon {

  val NTYPE_FN      = "t"
  val IS_DEPEND_FN  = "d"

  /** Сериализация и десериализация JSON. */
  implicit val FORMAT: OFormat[MNodeCommon] = (
    (__ \ NTYPE_FN).format[MNodeType] and
    (__ \ IS_DEPEND_FN).format[Boolean]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  /** ES-схема полей модели. */
  def generateMappingProps: List[DocField] = {
    List(
      FieldString(NTYPE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(IS_DEPEND_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/**
 * Контейнер common-данных всех узлов графа N2.
 * @param ntype Тип узла.
 * @param isDependent Является ли узел зависимым?
 *                    Если true, то можно удалять узел, когда на него никто не указывает.
 */
case class MNodeCommon(
  ntype         : MNodeType,
  isDependent   : Boolean
)
