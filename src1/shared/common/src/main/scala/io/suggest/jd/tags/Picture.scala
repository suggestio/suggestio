package io.suggest.jd.tags

import io.suggest.model.n2.edge.EdgeUid_t
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 17:50
  * Description: Тэг картики, изначально - фоновой.
  * Данные по картинке хранятся в эдже, а тут -- только явная отсылка к эджу.
  */
object Picture {

  /** Поддержка play-json. */
  implicit val PICTURE_FORMAT: OFormat[Picture] = {
    (__ \ "ei").format[EdgeUid_t]
      .inmap[Picture](apply, _.edgeUid)
  }

}


/** Тэг для рендера картинки с указанием id ресурса (эджа),
  * который содержит данные для доступа к этой самой картинке.
  *
  * @param edgeUid id эджа.
  */
case class Picture(
                    edgeUid: EdgeUid_t
                    // TODO Всобачивать сюда параметры для кропа сюда вместо edge.info.dynImgArgs?
                  )
  extends IDocTag {

  override def jdTagName = MJdTagNames.PICTURE

  override def children = Nil

  override def deepEdgesUidsIter = Iterator(edgeUid)

}
