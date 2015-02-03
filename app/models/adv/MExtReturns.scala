package models.adv

import controllers.routes
import io.suggest.model.EnumMaybeWithName
import models.{Context, ScJsState}
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

  abstract protected class Val(val strId: String) extends super.Val(strId) {
    /**
     * Сгенерить экземпляр билдера для дальнейшей сборки ссылки на возврат.
     * @return Конкретная реализация [[ReturnToScBuilder]].
     */
    def builder(): ReturnToScBuilder

    /** Код в conf/messages. */
    def i18nCode: String = "adv.ext.ret." + strId
  }


  type MExtReturn = Val
  override type T = MExtReturn


  /** Юзер должен возвращаться на выдачу размещающего. */
  val ToShowCase: MExtReturn = new Val("sc") {
    override def builder(): ReturnToScBuilder = {
      new RetToNode with RetToFocusedAd {}
    }
  }

  /** Юзер должен возвращаться на открытую рекламную карточку. */
  val ToAd: MExtReturn = new Val("ad") {
    override def builder(): ReturnToScBuilder = {
      new RetToNode {}
    }
  }


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



/** Абстрактный билдер для генерации ссылок возврата. */
sealed trait ReturnToScBuilder {
  /**
   * Выставить id узла, на выдачу которого надо возвращаться.
   * @param adnId id узла-ресивера.
   * @return this
   */
  def setAdnId(adnId: String): this.type = {
    this
  }

  /**
   * Высавить id рекламной карточки, на которую надо возвращаться.
   * @param adId id рекламной карточки.
   * @return this
   */
  def setFocusedAdId(adId: String): this.type = {
    this
  }

  /**
   * Выставить id продьюсера открытой рекламной карточки.
   * @param adnId id узла-продьюсера.
   * @return this
   */
  def setFocusedProducerId(adnId: String): this.type = {
    this
  }

  /**
   * Сгенерить экземпляр [[models.ScJsState]] на основе накопленных данных.
   * @return Экземпляр js-состояния.
   */
  def toJsState: ScJsState = ScJsState.veryEmpty

  /**
   * Сгенерить относительную ссылку на выдачу для накопленных данных.
   * @return String: ссылка относительно корня suggest.io.
   */
  def toRelUrl: String = {
    routes.MarketShowcase.geoSite(toJsState).url
  }

  /**
   * Сгенерить абсолютную ссылку на выдачу для накопленных данных.
   * @return Абсолютная ссылка.
   */
  def toAbsUrl: String = {
    Context.SC_URL_PREFIX + toRelUrl
  }
}


/** Поддержка возврата на выдачу узла. По идее этот трейт всегда нужен. */
sealed trait RetToNode extends ReturnToScBuilder {
  protected var adnIdOpt: Option[String] = None

  override def setAdnId(adnId: String): this.type = {
    adnIdOpt = Option(adnId)
    super.setAdnId(adnId)
  }

  override def toJsState: ScJsState = {
    super.toJsState.copy(
      adnId = adnIdOpt
    )
  }
}


/** Генерация возврата на выдачу с раскрытой рекламной карточкой. */
sealed trait RetToFocusedAd extends ReturnToScBuilder {
  protected var focAdIdOpt: Option[String] = None
  protected var focProdIdOpt: Option[String] = None

  override def setFocusedAdId(adId: String): this.type = {
    focAdIdOpt = Option(adId)
    super.setFocusedAdId(adId)
  }

  override def setFocusedProducerId(adnId: String): this.type = {
    focProdIdOpt = Option(adnId)
    super.setFocusedProducerId(adnId)
  }

  override def toJsState: ScJsState = {
    super.toJsState.copy(
      fadsProdIdOpt = focProdIdOpt,
      fadOpenedIdOpt = focAdIdOpt
    )
  }
}

