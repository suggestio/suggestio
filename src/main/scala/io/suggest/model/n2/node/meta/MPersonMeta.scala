package io.suggest.model.n2.node.meta

import io.suggest.common.EmptyProduct
import io.suggest.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 10:15
 * Description: Модель инфы о человеке (фио и т.д.), живёт внутри [[MNodeMeta]].
 */
object MPersonMeta extends IGenEsMappingProps {

  /** Вернуть пустой экземпляр модели, используется очень часто. */
  def empty: MPersonMeta = _Empty

  /** Всегда пустой экземпляр модели. */
  private object _Empty extends MPersonMeta {
    override def nonEmpty = false
  }

  val NAME_FIRST_FN   = "f"
  val NAME_LAST_FN    = "l"
  val EXT_AVA_URL_FN  = "a"


  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MPersonMeta] = (
    (__ \ NAME_FIRST_FN).formatNullable[String] and
    (__ \ NAME_LAST_FN).formatNullable[String] and
    (__ \ EXT_AVA_URL_FN).formatNullable[List[String]]
      .inmap[List[String]](
        { _ getOrElse Nil },
        { urls => if (urls.isEmpty) None else Some(urls) }
      )
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_FIRST_FN,    index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(NAME_LAST_FN,     index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(EXT_AVA_URL_FN,   index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

}


case class MPersonMeta(
  nameFirst     : Option[String]    = None,
  nameLast      : Option[String]    = None,
  extAvaUrls    : List[String]      = List.empty
)
  extends EmptyProduct

