package io.suggest.ym.model

import io.suggest.model.{EsModelEmpty, EsModelStaticEmpty}
import io.suggest.util.SioEsUtil._
import io.suggest.model.common._
import com.fasterxml.jackson.annotation.JsonIgnore
import common._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 19:16
 * Description: Узел рекламной сети. Он приходит на смену всем MMart, MShop и т.д.
 * Модель узла конструируется компилятором из кучи кусков, вроде бы не связанных между собой.
 */
object MAdnNode
  extends EsModelStaticEmpty[MAdnNode]
  with EMCompanyIdStatic[MAdnNode]
  with EMPersonIdsStatic[MAdnNode]
  with EMLegalEntityStatic[MAdnNode]
  with EMAdNetMemberStatic[MAdnNode]
  with EMAdnMVisualStatic[MAdnNode]
  with EMAdnMPubSettingsStatic[MAdnNode]
  with EMAdnMMetadataStatic[MAdnNode]
{
  val ES_TYPE_NAME: String = "adnNode"

  protected def dummy(id: String) = MAdnNode(
    companyId = null,
    personIds = Set.empty,
    adnMemberInfo = null,
    legalInfo = null,
    visual = null,
    pubSettings = null,
    metadata = null,
    id = Option(id)
  )

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
    val delFut = super.deleteById(id)
    delFut onSuccess { case isDeleted =>
      sn publish AdnNodeDeletedEvent(id, isDeleted)
    }
    delFut
  }
}


case class MAdnNode(
  var companyId     : CompanyId_t,
  var personIds     : Set[String],
  var adnMemberInfo : AdNetMemberInfo,
  var legalInfo     : AdnLegalEntityInfo,
  var visual        : AdnVisual,
  var pubSettings   : AdnMPubSettings,
  var metadata      : AdnMMetadata,
  var id            : Option[String] = None
)
  extends EsModelEmpty[MAdnNode]
  with EMCompanyId[MAdnNode]
  with EMPersonIds[MAdnNode]
  with EMLegalEntity[MAdnNode]
  with EMAdNetMember[MAdnNode]
  with EMAdnMVisual[MAdnNode]
  with EMAdnMPubSettings[MAdnNode]
  with EMAdnMMetadata[MAdnNode]
{
  @JsonIgnore
  def companion = MAdnNode


  /** Перед сохранением можно проверять состояние экземпляра (полей экземпляра). */
  @JsonIgnore
  override def isFieldsValid: Boolean = {
    super.isFieldsValid &&
      companyId != null && personIds != null && adnMemberInfo != null && legalInfo != null &&
      visual != null && pubSettings != null && metadata != null
  }


  /**
   * Сохранить экземпляр в хранилище модели.
   * При успехе будет отправлено событие [[io.suggest.event.AdnNodeSavedEvent]] в шину событий.
   * @return Фьючерс с новым/текущим id.
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val saveFut = super.save
    saveFut onSuccess { case adnId =>
      sn publish AdnNodeSavedEvent(adnId, this, isCreated = id.isEmpty)
    }
    saveFut
  }

}

