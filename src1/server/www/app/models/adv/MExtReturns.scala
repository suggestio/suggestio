package models.adv

import controllers.routes
import play.api.data.Mapping
import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import io.suggest.sc.sc3.Sc3Pages

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.15 13:01
 * Description: Юзер имеет возможность выбирать, куда юзер должен возвращаться.
 * Тут варианты настроек перехода на suggest.io.
 */

object MExtReturns extends StringEnum[MExtReturn] {

  /** Юзер должен возвращаться на выдачу размещающего. */
  case object ToShowCase extends MExtReturn("sc") {
    override def builder(): ReturnToScBuilder = {
      new RetToNode {}
    }
  }

  /** Юзер должен возвращаться на открытую рекламную карточку. */
  case object ToAd extends MExtReturn("ad") {
    override def builder(): ReturnToScBuilder = {
      new RetToNode with RetToFocusedAd {}
    }
  }


  override def values = findValues

  def default = ToShowCase

  // Утиль для описания парсеров и мапперов для значений этой модели.
  private def strIdLengths = values.iterator.map(_.value.length)
  def strIdLenMin = strIdLengths.min
  def strIdLenMax = strIdLengths.max


  // Утиль для построения мапперов форм.

  /** Form mapping для опционального поля со значением [[MExtReturn]]. */
  def optMapping: Mapping[Option[MExtReturn]] =
    EnumeratumJvmUtil.stringIdOptMapping( MExtReturns )

  /** Form mapping для обязательного поля со значением [[MExtReturn]]. */
  def mapping: Mapping[MExtReturn] =
    FormMappingUtil.optMapping2required( optMapping )

}


sealed abstract class MExtReturn(override val value: String) extends StringEnumEntry {
  /**
    * Сгенерить экземпляр билдера для дальнейшей сборки ссылки на возврат.
    * @return Конкретная реализация [[ReturnToScBuilder]].
    */
  def builder(): ReturnToScBuilder

  /** Код в conf/messages. */
  def i18nCode: String = "adv.ext.ret." + value


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
   * Сгенерить экземпляр [[MainScreen]] на основе накопленных данных.
    *
    * @return Экземпляр js-состояния.
   */
  def toJsState: Sc3Pages.MainScreen = Sc3Pages.MainScreen.empty

  def toCall = routes.Sc.geoSite(toJsState)

  /**
   * Сгенерить относительную ссылку на выдачу для накопленных данных.
   * @return String: ссылка относительно корня suggest.io.
   */
  def toRelUrl: String = {
    toCall.url
  }

}


/** Поддержка возврата на выдачу узла. По идее этот трейт всегда нужен. */
sealed trait RetToNode extends ReturnToScBuilder {
  protected var adnIdOpt: Option[String] = None

  override def setAdnId(adnId: String): this.type = {
    adnIdOpt = Option(adnId)
    super.setAdnId(adnId)
  }

  override def toJsState: Sc3Pages.MainScreen = {
    super.toJsState.copy(
      nodeId = adnIdOpt
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

  override def toJsState: Sc3Pages.MainScreen = {
    super.toJsState.copy(
      nodeId = focProdIdOpt,
      focusedAdId = focAdIdOpt
    )
  }
}

