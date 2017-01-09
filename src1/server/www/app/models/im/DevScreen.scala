package models.im

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.stat.m.MViewPort
import io.suggest.ym.model.common.MImgSizeT
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable
import util.img.{DevScreenParsers, PicSzParsers}

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
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[DevScreen] = {
    new QueryStringBindableImpl[DevScreen] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DevScreen]] = {
        for {
          devScreenStrE <- strB.bind(key, params)
        } yield {
          devScreenStrE
            .right
            .flatMap { devScreenStr =>
              val pr = maybeFromString(devScreenStr)
              if (pr.successful) {
                Right(pr.get)
              } else {
                Left(pr.toString)
              }
            }
        }
      }

      override def unbind(key: String, value: DevScreen): String = {
        strB.unbind(key, value.toString)
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

  /** Конвертация модели в инстанс MViewPort для моделей статистики. */
  def toStatViewPort: MViewPort = {
    MViewPort(
      widthPx   = width,
      heightPx  = height,
      pxRatio   = pixelRatioOpt.map(_.pixelRatio)
    )
  }

}
