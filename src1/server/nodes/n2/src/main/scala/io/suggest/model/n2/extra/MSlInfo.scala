package io.suggest.model.n2.extra

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.sc.common.AdShowLevel
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 18:48
 * Description: Инфа об одном уровне отображения для узла.
 */
object MSlInfo extends IGenEsMappingProps {

  val SL_FN     = "l"
  val LIMIT_FN  = "i"

  /** Поддержка JSON для экземпляров модели. */
  implicit val FORMAT: OFormat[MSlInfo] = (
    (__ \ SL_FN).format[AdShowLevel] and
    (__ \ LIMIT_FN).format[Int]
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  /** Сборка настроек ES-индекса. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(SL_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldNumber(LIMIT_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

}


/** Интерфейс и логика экземпляров модели реализована в этом трейте. */
trait ISlInfo {

  def sl    : AdShowLevel
  def limit : Int

  def allowed = limit > 0

}


/**
 * Экземпляр модели.
 * @param sl Уровень отображения, к которому относяться остальные настройки.
 * @param limit Лимит кол-ва карточек узла на этом уровне.
 */
case class MSlInfo(
  sl    : AdShowLevel,
  limit : Int
)
  extends ISlInfo
