package io.suggest.ym.model.tag

import io.suggest.model.PrefixedFn
import io.suggest.util.SioConstants
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.15 9:37
 * Description: Модель одного тега узла.
 */

object MNodeTag extends PrefixedFn {

  /** Название поля с нормализованным именем тега.
    * val потому что нет смысла делать def -- строка всегда в памяти из-за val READS/WRITES. */
  val ID_FN = "n"
  def ID_ESFN = _fullFn(ID_FN)

  /** Название под-поля тега c поддержкой полнотекстового поиска. */
  def FTS_SUBFN = SioConstants.SUBFIELD_FTS

  override protected def _PARENT_FN = EMTags.TAGS_FN


  /** Десериализатор из JSON. */
  implicit val READS: Reads[MNodeTag] = {
    (__ \ ID_FN).read[String]
      .map(MNodeTag.apply)
  }

  /** Сериализатор в JSON. */
  implicit val WRITES: Writes[MNodeTag] = {
    (__ \ ID_FN).write[String]
      .contramap( _.id )
  }


  import io.suggest.util.SioEsUtil._

  /** Сборка ES-маппинга для nested-object тегов. Тут перечисляются поля каждого nested-object. */
  def generateMappingProps: List[DocField] = {
    List(
      FieldString(
        id    = ID_FN,
        index = FieldIndexingVariants.analyzed,
        include_in_all = true,
        analyzer = SioConstants.TAG_AN,
        fields = Seq(
          FieldString(
            id        = FTS_SUBFN,
            index     = FieldIndexingVariants.analyzed,
            analyzer  = SioConstants.DFLT_AN,
            include_in_all = true
          )
        )
      )
    )
  }

}


/**
 * Модель одного тега узла.
 * @param id Почищенный от мусора тег.
 */
case class MNodeTag(
  id: String
)
