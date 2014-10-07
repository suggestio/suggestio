package models.im

import io.suggest.ym.model.common.MImgSizeT
import play.api.mvc.QueryStringBindable
import util.img.{PicSzParsers, DevScreenParsers}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:44
 * Description: Модель, отражающая параметры клиентского экрана.
 */

object DevScreen extends DevScreenParsers {

  /** Биндер для отработки значения screen из ссылки. */
  implicit def qsb: QueryStringBindable[DevScreen] = {
    new QueryStringBindable[DevScreen] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DevScreen]] = {
        params.get(key)
          .flatMap(_.headOption)
          .map { v =>
            val pr = parse(devScreenP, v)
            if (pr.successful) {
              Right(pr.get)
            } else {
              Left(pr.toString)
            }
          }
      }

      override def unbind(key: String, value: DevScreen): String = {
        val sb = new StringBuilder(32)
        sb.append( value.width )
          .append( 'x' )
          .append( value.height )
        value.pixelRatioOpt.foreach { dpr =>
          sb.append( PicSzParsers.IMG_RES_DPR_DELIM )
            .append( dpr.pixelRatio )
        }
        sb.toString()
      }
    }
  }

}


/** Интерфейс модели. Для большинства случаев его достаточно. */
trait DevScreenT extends MImgSizeT {

  /** Пиксельная плотность экрана. */
  def pixelRatio: DevPixelRatio

  /** Найти базовое разрешение окна по соотв.модели. */
  def maybeBasicScreenSize = BasicScreenSizes.includesSize(this)
}


case class DevScreen(
  width: Int,
  height: Int,
  pixelRatioOpt: Option[DevPixelRatio]
)
  extends DevScreenT
{

  override def pixelRatio: DevPixelRatio = {
    pixelRatioOpt getOrElse DevPixelRatios.default
  }

}
