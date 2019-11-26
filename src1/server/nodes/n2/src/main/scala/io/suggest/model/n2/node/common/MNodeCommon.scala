package io.suggest.model.n2.node.common

import io.suggest.model.n2.node.MNodeType
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.common.empty.OptionUtil.BoolOptOps
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 14:41
 * Description: Модель общих для всех N2-узлов полей [[io.suggest.model.n2.node.MNode]].
 */
object MNodeCommon extends IGenEsMappingProps {

  val NODE_TYPE_FN          = "t"
  val IS_DEPEND_FN          = "d"
  val IS_ENABLED_FN         = "e"
  val DISABLE_REASON_FN     = "r"

  /** Сериализация и десериализация JSON. */
  implicit val FORMAT: OFormat[MNodeCommon] = (
    (__ \ NODE_TYPE_FN).format[MNodeType] and
    (__ \ IS_DEPEND_FN).format[Boolean] and
    (__ \ IS_ENABLED_FN).formatNullable[Boolean]
      .inmap [Boolean] (_.getOrElseTrue, someF) and
    (__ \ DISABLE_REASON_FN).formatNullable[String]
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  /** ES-схема полей модели. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldKeyword(NODE_TYPE_FN, index = true, include_in_all = false),
      FieldBoolean(IS_DEPEND_FN, index = true, include_in_all = false),
      FieldBoolean(IS_ENABLED_FN, index = true, include_in_all = false),
      FieldText(DISABLE_REASON_FN, index = false, include_in_all = false)
    )
  }

  val isEnabled = GenLens[MNodeCommon](_.isEnabled)

}


/**
 * Контейнер common-данных всех узлов графа N2.
 * @param ntype Тип узла.
 * @param isDependent Является ли узел зависимым?
 *                    Если true, то можно удалять узел, когда на него никто не указывает.
 * @param isEnabled Включен ли узел? Отключение узла приводит к блокировке некоторых функций.
 * @param disableReason Причина отключения узла, если есть.
 */
case class MNodeCommon(
  ntype             : MNodeType,
  isDependent       : Boolean,
  isEnabled         : Boolean               = true,
  disableReason     : Option[String]        = None
)
