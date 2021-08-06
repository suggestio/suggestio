package io.suggest.n2.node.meta

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 10:15
 * Description: Модель инфы о человеке (фио и т.д.), живёт внутри MNodeMeta.
 */
object MPersonMeta
  extends IEsMappingProps
  with IEmpty
{

  override type T = MPersonMeta

  object Fields {
    val NAME_FIRST_FN   = "firstName"
    val NAME_LAST_FN    = "lastName"
    val EXT_AVA_URL_FN  = "avatarUrl"
  }

  /** Вернуть пустой экземпляр модели, используется очень часто. */
  override def empty = MPersonMeta()


  import Fields._

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


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.NAME_FIRST_FN   -> FText.indexedJs,
      F.NAME_LAST_FN    -> FText.indexedJs,
      F.EXT_AVA_URL_FN  -> FText.notIndexedJs,
    )
  }

}


final case class MPersonMeta(
  nameFirst     : Option[String]    = None,
  nameLast      : Option[String]    = None,
  extAvaUrls    : List[String]      = Nil,
)
  extends EmptyProduct

