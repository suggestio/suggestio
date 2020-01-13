package io.suggest.n2.node.common

import io.suggest.n2.node.MNodeType
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.es.{IEsMappingProps, MappingDsl}
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 14:41
 * Description: Модель общих для всех N2-узлов полей [[io.suggest.n2.node.MNode]].
 */
object MNodeCommon extends IEsMappingProps {

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

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    Json.obj(
      NODE_TYPE_FN -> FKeyWord.indexedJs,
      IS_DEPEND_FN -> FBoolean.indexedJs,
      IS_ENABLED_FN -> FBoolean.indexedJs,
      DISABLE_REASON_FN -> FText.notIndexedJs,
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
