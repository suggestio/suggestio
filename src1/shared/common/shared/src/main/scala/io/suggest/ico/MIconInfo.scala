package io.suggest.ico

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 18:18
  * Description: Клиент-ориентированная модель описания одной иконки.
  */
object MIconInfo {

  @inline implicit def univEq: UnivEq[MIconInfo] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  implicit def MPWA_ICON_WRITES: OWrites[MIconInfo] = (
    (__ \ "src").write[String] and
    (__ \ "sizes")
      .write[String]
      .contramap[Seq[MSize2di]] { szs =>
        szs
          .iterator
          .map { sz =>
            ISize2di.wxh(sz)
          }
          .mkString(" ")
      } and
    (__ \ "type").write[String]
  )(unlift(unapply))


  def src = GenLens[MIconInfo](_.src)

}


/** Класс описания одной иконки web-приложения.
  *
  * @param src Сорец.
  * @param sizes Размеры картинки.
  * @param mimeType MIME-тип файла-картинки.
  */
case class MIconInfo(
                      src      : String,
                      sizes    : Seq[MSize2di],
                      mimeType : String,
                    )

