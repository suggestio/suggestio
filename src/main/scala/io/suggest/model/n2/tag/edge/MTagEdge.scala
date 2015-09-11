package io.suggest.model.n2.tag.edge

import java.{util => ju}

import io.suggest.model.PrefixedFn
import io.suggest.util.SioConstants
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.15 9:37
 * Description: Модель одного тега узла.
 */

object MTagEdge extends PrefixedFn {

  /** Название поля с нормализованным именем тега.
    * val потому что нет смысла делать def -- строка всегда в памяти из-за val READS/WRITES. */
  val ID_FN         = "id"
  def ID_ESFN       = _fullFn(ID_FN)

  /** Название под-поля тега c поддержкой полнотекстового поиска. */
  val RAW_NAME_FN   = "raw"
  def RAW_NAME_ESFN = _fullFn(RAW_NAME_FN)

  override protected def _PARENT_FN = EMTagsEdge.TAGS_FN


  /** Десериализатор из JSON. */
  implicit val READS: Reads[MTagEdge] = (
    (__ \ ID_FN).read[String] and
    (__ \ RAW_NAME_FN).read[String]
  )(apply _)

  /** Сериализатор в JSON. */
  implicit val WRITES: Writes[MTagEdge] = (
    (__ \ ID_FN).write[String] and
    (__ \ RAW_NAME_FN).write[String]
  )(unlift(unapply))


  /** legacy-десериализация из выхлопов jackson'а. */
  def fromJackson(rawMap: Any): MTagEdge = {
    val m = rawMap.asInstanceOf[ ju.Map[String, String] ]
    MTagEdge(
      id  = m.get(MTagEdge.ID_FN),
      raw = m.get(MTagEdge.RAW_NAME_FN)
    )
  }

  import io.suggest.util.SioEsUtil._

  /** Сборка ES-маппинга для nested-object тегов. Тут перечисляются поля каждого nested-object. */
  def generateMappingProps: List[DocField] = {
    List(
      FieldString(
        id              = ID_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = true,
        analyzer        = SioConstants.KW_LC_AN
      ),
      FieldString(
        id              = RAW_NAME_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = true
      )
    )
  }

}


/** Интерфейс экземпляров модели. */
trait ITagEdge {
  /** Почищенный от мусора тег, в нижнем регистре. */
  def id  : String
  /** Сырое значение тега, почищенное в общих чертах. */
  def raw : String
}


/** Дефолтовая реализация модели одного тега узла. */
case class MTagEdge(
  override val id  : String,
  override val raw : String
)
  extends ITagEdge
