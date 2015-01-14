package models.adv

import io.suggest.model.EnumMaybeWithName
import play.api.data.Mapping
import util.FormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.15 13:01
 * Description: Юзер имеет возможность выбирать, куда юзер должен возвращаться.
 * Тут варианты настроек перехода на suggest.io.
 */

object MExtReturns extends Enumeration with EnumMaybeWithName {

  protected sealed class Val(val strId: String) extends super.Val(strId)

  type MExtReturn = Val
  override type T = MExtReturn

  /** Юзер должен возвращаться на открытую рекламную карточку. */
  val ToAd: MExtReturn = new Val("ad")

  /** Юзер должен возвращаться на выдачу размещающего. */
  val ToShowCase: MExtReturn = new Val("sc")


  def default = ToShowCase

  // Утиль для описания парсеров и мапперов для значений этой модели.
  private def strIdLengths = values.iterator.map(_.strId.length)
  def strIdLenMin = strIdLengths.min
  def strIdLenMax = strIdLengths.max


  // Утиль для построения мапперов форм.
  import play.api.data.Forms._

  /** Form mapping для опционального поля со значением [[MExtReturn]]. */
  def optMapping: Mapping[Option[MExtReturn]] = {
    val m = text(minLength = strIdLenMin, maxLength = strIdLenMax * 3)
    FormUtil.toStrOptM(m)
      .transform [Option[MExtReturn]] (_.flatMap(maybeWithName), _.map(_.strId))
  }

  /** Form mapping для обязательного поля со значением [[MExtReturn]]. */
  def mapping: Mapping[MExtReturn] = {
    optMapping
      .verifying("error.required", _.isDefined)
      .transform[MExtReturn] (_.get, Some.apply)
  }

}

