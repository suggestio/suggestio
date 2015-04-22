package models.im

import io.suggest.ym.model.common.MImgSizeT
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable
import util.img.{PicSzParsers, DevScreenParsers}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:44
 * Description: Модель, отражающая параметры клиентского экрана.
 *
 * 2015.apr.22: Тут следует обходится без лишних интерфейсов экземпляров, чтобы не было двоякости в биндингах/маппингах.
 * Эта модель путешествует по многим уровням проекта, поэтому трейт DevScreenT был выпилен для жесткой унификации.
 */

object DevScreen extends DevScreenParsers {

  def maybeFromString(s: String) = parse(devScreenP, s)

  /** Биндер для отработки значения screen из ссылки. */
  implicit val qsb = {
    new QueryStringBindable[DevScreen] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DevScreen]] = {
        params.get(key)
          .flatMap(_.headOption)
          .map { v =>
            val pr = maybeFromString(v)
            if (pr.successful) {
              Right(pr.get)
            } else {
              Left(pr.toString)
            }
          }
      }

      override def unbind(key: String, value: DevScreen): String = {
        value.toString
      }
    }
  }


  /** Дефолтовое значение, используемое когда значения нет, но очень нужно. */
  def default = DevScreen(
    width         = 1024,
    height        = 768,
    pixelRatioOpt = Some(DevPixelRatios.default)
  )


  import play.api.data.Forms._

  /** Form-mapping для обязательного задания [[DevScreen]].
    * Форма должна иметь отдельные поля для ширины, высоты и необязательной плотности пикселей экрана. */
  def mappingFat: Mapping[DevScreen] = {
    val whm = number(min = 0, max = 8192)
    mapping(
      "width"   -> whm,
      "height"  -> whm,
      "pxRatio" -> DevPixelRatios.mappingOpt
    )
    { DevScreen.apply }
    { DevScreen.unapply }
  }

}


/**
 * Данные по экрану.
 * @param width Ширина в css-пикселях.
 * @param height Высота в css-пикселях.
 * @param pixelRatioOpt Плотность пикселей, если известна.
 */
case class DevScreen(
  width         : Int,
  height        : Int,
  pixelRatioOpt : Option[DevPixelRatio]
) 
  extends MImgSizeT with ImgOrientationT
{

  def pixelRatio: DevPixelRatio = {
    pixelRatioOpt getOrElse DevPixelRatios.default
  }

  /** Найти базовое разрешение окна по соотв.модели. */
  def maybeBasicScreenSize = BasicScreenSizes.includesSize(this)

  override def toString: String = {
    val sb = new StringBuilder(32)
    sb.append( width )
      .append( 'x' )
      .append( height )
    pixelRatioOpt.foreach { dpr =>
      sb.append( PicSzParsers.IMG_RES_DPR_DELIM )
        .append( dpr.pixelRatio )
    }
    sb.toString()
  }
}
