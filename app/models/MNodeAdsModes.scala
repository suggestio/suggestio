package models

import io.suggest.model.EnumValue2Val
import io.suggest.util.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.14 15:54
 * Description: Модель, описывающая допустимые режимы отображения списка рекламных карточек узла.
 */
object MNodeAdsModes extends Enumeration with MacroLogsImplLazy with EnumValue2Val {

  import LOGGER._

  /**
   * Экзепляр модели.
   * @param shortId Уникальный строковой id экземпляра.
   */
  protected class Val(val shortId: String) extends super.Val(shortId) {
    /**
     * Значение модели обязателено в контроллере, но опционально в query string.
     * И при unbind() надо обрабатывать указанное значение указанным образом.
     * @return None для дефолтового значения, т.е. если qsb.unbind() текущего экземпляра нужно маппить на "".
     *         Some(), когда выбран недефолтовый режим.
     */
    def qsbOption: Option[String] = Some(shortId)
  }

  override type T = Val


  /** Режим отображения всех карточек. */
  val ALL_ADS: T = new Val("a") {
    override def qsbOption: Option[String] = None
  }

  /** Режим отображения только карточек, которые находятся на рассмотрении у других узлов. */
  val ADV_REQ_ADS: T = new Val("r")

  /** Отображение принятых к отображению на узлах карточек. */
  val ADV_OK_ADS: T = new Val("o")


  /** qsb-маппер, линкуемый в routes. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
      import util.qsb.QsbUtil._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          maybeModeStr <- strOptB.bind(key, params)
        } yield {
          val result = (maybeModeStr : Option[String]).fold(ALL_ADS) { shortId =>
            values.find(_.shortId == shortId) getOrElse {
              warn(s"qsb(): Unknown ads mode: $shortId. Fallbacking to ALL_ADS.")
              ALL_ADS
            }
          }
          Right(result)
        }
      }

      override def unbind(key: String, value: T): String = {
        strOptB.unbind(key, value.qsbOption)
      }
    }
  }

}
