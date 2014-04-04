package io.suggest.ym.model

import io.suggest.model.{MPict, EsModelEmpty, EsModelStaticEmpty}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.ym.model.common._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.model.common._
import org.joda.time.DateTime
import scala.util.{Failure, Success}
import io.suggest.util.MacroLogsImpl
import io.suggest.ym.model.ad.{EMAdOffersStatic, EMAdOffers, AdOfferT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 15:00
 * Description: Рекламные карточки второго поколения, пригодные для использования в рекламной сети.
 * Рефакторинг в MMartAd был бы слишком глубок, поэтому лучше было начать с чистого листа.
 */
object MAd
  extends MAdStaticT[MAd]
  with EMAdEntityStatic[MAd]
  with EMAdOffersStatic[MAd]
  with EMImgStatic[MAd]
  with EMLogoImgStatic[MAd]
  with EMTextAlignStatic[MAd]
  with EMAdPanelSettingsStatic[MAd]
  with EMPrioOptStatic[MAd]
  with EMShowLevelsStatic[MAd]
  with EMUserCatIdStatic[MAd]
  with EMDateCreatedStatic[MAd]
  with MacroLogsImpl
{

  import LOGGER._

  val ES_TYPE_NAME: String = "ad"

  protected def dummy(id: String): MAd = ???

  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val adOptFut = getById(id)
    adOptFut flatMap {
      case Some(ad) =>
        // Удаляем картинку рекламы
        val imgId = ad.img.id
        MPict.deleteFully(imgId) onComplete {
          case Success(_)  => trace("Successfuly erased main picture: " + imgId)
          case Failure(ex) => error("Failed to delete associated picture: " + imgId, ex)
        }
        // Одновременно удаляем логотип.
        ad.logoImgOpt.foreach { logoImg =>
          val logoImgId = logoImg.id
          MPict.deleteFully(logoImgId) onComplete {
            case Success(_)  => trace("Successfuly erased 2nd-logo picture: " + logoImgId)
            case Failure(ex) => error("Failed to delete 2nd-logo picture: " + logoImgId, ex)
          }
        }
        val resultFut = super.deleteById(id)
        resultFut onSuccess { case _ =>
          sn publish AdDeletedEvent(ad)
        }
        resultFut

      case None => Future successful false
    }
  }

}


/** Тут поддержка полей MAd, специфичная только для рекламных карточек. Пока что пустая.
  * Реализация вынесена из MAd для соблюдения stackable trait pattern. */
trait MAdStaticT[T <: MAdT] extends EsModelStaticEmpty[T]

trait MAdT[T <: MAdT[T]] extends EsModelEmpty[T]


case class MAd(
  var producerId : String,
  var receivers  : Set[AdReceiverInfo] = Set.empty,
  var producerType: AdNetMemberType,
  var offers     : List[AdOfferT],
  var img        : MImgInfo,
  var logoImgOpt : Option[MImgInfo] = None,
  var textAlign  : Option[TextAlign] = None,
  var panel      : Option[AdPanelSettings] = None,
  var prio       : Option[Int] = None,
  var id         : Option[String] = None,
  var showLevels : Set[AdShowLevel] = Set.empty,
  var userCatId  : Option[String] = None,
  var dateCreated : DateTime = DateTime.now
)
  extends MAdT[MAd]
  with EMAdEntity[MAd]
  with EMAdOffers[MAd]
  with EMImg[MAd]
  with EMLogoImg[MAd]
  with EMTextAlign[MAd]
  with EMAdPanelSettings[MAd]
  with EMPrioOpt[MAd]
  with EMShowLevels[MAd]
  with EMUserCatId[MAd]
  with EMDateCreated[MAd]
{
  @JsonIgnore
  def companion = MAd

  /**
   * Сохранить экземпляр в хранилище ES. Если всё ок, то породить событие успешного сохранения.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val resultFut = super.save
    resultFut onSuccess { case adId =>
      this.id = Option(adId)
      sn publish AdSavedEvent(this)
    }
    resultFut
  }

}


