package io.suggest.dev

import io.suggest.common.geom.d2.MSize2di
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:44
 * Description: JVM-поддержка модели, отражает доп. возможности модели параметров клиентского экрана.
 */

object MScreenJvm {

  /** Биндер для отработки значения screen из ссылки. */
  implicit def devScreenQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MScreen] = {
    new AbstractQueryStringBindable[MScreen] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScreen]] = {
        for {
          devScreenStrE <- strB.bind(key, params)
        } yield {
          devScreenStrE
            .flatMap { MScreen.maybeFromString }
        }
      }

      override def unbind(key: String, value: MScreen): String = {
        strB.unbind(key, value.toString)
      }
    }
  }


  import play.api.data.Forms._

  /** Form-mapping для обязательного задания [[MScreenJvm]].
    * Форма должна иметь отдельные поля для ширины, высоты и необязательной плотности пикселей экрана. */
  def mappingFat: Mapping[MScreen] = {
    val whm = number(min = 0, max = 8192)
    mapping(
      "width"   -> whm,
      "height"  -> whm,
      "pxRatio" -> MPxRatioJvm.mapping
    )
    { (w, h, pxRatio) => MScreen(MSize2di(w, h), pxRatio) }
    { m => Some((m.wh.width, m.wh.height, m.pxRatio)) }
  }

}
