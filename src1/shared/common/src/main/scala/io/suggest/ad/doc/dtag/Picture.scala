package io.suggest.ad.doc.dtag

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    (__ \ "ei").format[Int]
      .inmap[Picture](apply, _.edgeOrderId)
  }

}


/** Тэг для рендера картинки с указанием id ресурса (эджа),
  * который содержит данные для доступа к этой самой картинке.
  *
  * @param edgeOrderId id эджа.
  */
case class Picture(
                    edgeOrderId: Int
                    // TODO Всобачивать сюда параметры для кропа сюда вместо edge.info.dynImgArgs?
                  )
  extends IDocTag {

  override def dtName = MDtNames.Picture

  override def children = Nil

}
