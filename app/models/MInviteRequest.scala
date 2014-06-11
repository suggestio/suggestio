package models

import _root_.util.qsb.QsbUtil
import io.suggest.model._
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import io.suggest.util.SioEsUtil.FieldAll
import io.suggest.util.SioEsUtil.FieldBoolean
import io.suggest.util.SioEsUtil.FieldString
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import io.suggest.util.SioEsUtil.FieldSource
import io.suggest.event.SioNotifierStaticClientI
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import play.api.mvc.QueryStringBindable
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import EsModel.{stringParser, date2JsStr, booleanParser, dateTimeParser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.14 13:25
 * Description: Запросы на инвайты.
 */

object MInviteRequest extends EsModelStaticT with PlayMacroLogsImpl {

  import LOGGER._

  /** Тип реквеста: рекламодатель или wifi. */
  val REQ_TYPE_ESFN       = "rt"

  val JOIN_ANSWERS_ESFN   = "ja"
  val META_ESFN           = "meta"

  // progress-поля хранят в себе ход обработки заявы.
  val PROGRESS_ESFN       = "p"

  override type T = MInviteRequest

  override val ES_TYPE_NAME = "invReq"

  override protected def dummy(id: String, version: Long): T = {
    MInviteRequest(id = Some(id), versionOpt = Some(version), reqType = null, meta = null)
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = true),
    FieldSource(enabled = true)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(REQ_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldObject(META_ESFN, enabled = true, properties = MirMeta.generateMappingProps),
    FieldObject(JOIN_ANSWERS_ESFN, enabled = true, properties = SMJoinAnswers.generateMappingProps),
    FieldObject(PROGRESS_ESFN, enabled = true, properties = MirProgress.generateMappingProps)
  )

  override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    case (REQ_TYPE_ESFN, rtRaw) =>
      acc.reqType = InviteReqTypes.withName(stringParser(rtRaw))
    case (META_ESFN, metaRaw: ju.Map[_,_]) =>
      acc.meta = MirMeta.deserialize(metaRaw)
    case (JOIN_ANSWERS_ESFN, jaRaw: ju.Map[_,_]) =>
      acc.joinAnswers = Option( SMJoinAnswers.deserialize(jaRaw) )
    case (PROGRESS_ESFN, jmap: ju.Map[_,_]) =>
      acc.progress = Option( MirProgress.deserialize(jmap) )
  }
}

import MInviteRequest._

case class MInviteRequest(
  var reqType: InviteReqType,
  var meta: MirMeta,
  var joinAnswers: Option[SMJoinAnswers] = None,
  var progress: Option[MirProgress] = None,
  id: Option[String] = None,
  versionOpt: Option[Long] = None
) extends EsModelT {

  override type T = MInviteRequest

  override def companion = MInviteRequest

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc =
      REQ_TYPE_ESFN -> JsString(reqType.toString) ::
      META_ESFN     -> meta.toPlayJson ::
      acc
    if (joinAnswers exists { _.isDefined })
      acc1 ::= JOIN_ANSWERS_ESFN -> joinAnswers.get.toPlayJson
    if (progress exists { _.isDefined })
      acc1 ::= PROGRESS_ESFN -> progress.get.toPlayJson
    acc1
  }
}


object InviteReqTypes extends Enumeration {
  type InviteReqType = Value
  val Adv = Value("a")
  val Wifi = Value("w")
}


trait MInviteRequestJmxMBean extends EsModelJMXMBeanCommon
class MInviteRequestJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MInviteRequestJmxMBean
{
  override def companion = MInviteRequest
}



/** Набор галочек-ответов надо биндить и разбиндивать на qs, чтобы можно было гулять между страницами. */
object SMJoinAnswers {

  val HAVE_WIFI_ESFN      = "haveWifi"
  val FULL_COVERAGE_ESFN  = "fullCover"
  val KNOWN_EQUIP_ESFN    = "knownEquip"
  val ALT_FW_ESFN         = "altFw"
  val IS_WRT_ESFN         = "isWrt"
  val LANDLINE_INET_ESFN  = "llInet"
  val SMALL_ROOM_ESFN     = "sr"
  val AUDIENCE_SZ_ESFN    = "audSz"

  // суффиксы названий аргрументов в url query string.
  def haveWifiSuf     = ".haveWifi"
  def fullCoverSuf    = ".fullCover"
  def knownEquipSuf   = ".knownEquip"
  def altFwSuf        = ".altFw"
  def wrtFwSuf        = ".wrtFw"
  def landlineInetSuf = ".landlineInet"
  def smallRoomSuf    = ".smallRoom"
  def audSzSuf        = ".audSz"

  /** Биндер экземпляра SMJoinAnswers из url query string и обратно. Вызывается прямо из routes. */
  implicit def queryStringBinder(implicit boolOptBinder: QueryStringBindable[Option[Boolean]], strOptBinder: QueryStringBindable[Option[String]]) = {
    import QsbUtil._
    new QueryStringBindable[SMJoinAnswers] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SMJoinAnswers]] = {
        for {
          maybeHaveWifi     <- boolOptBinder.bind(key + haveWifiSuf, params)
          maybeFullCover    <- boolOptBinder.bind(key + fullCoverSuf, params)
          maybeKnownEquip   <- boolOptBinder.bind(key + knownEquipSuf, params)
          maybeAltFw        <- boolOptBinder.bind(key + altFwSuf, params)
          maybeWrtFw        <- boolOptBinder.bind(key + wrtFwSuf, params)
          maybeLandlineInet <- boolOptBinder.bind(key + landlineInetSuf, params)
          maybeSmallRoom    <- boolOptBinder.bind(key + smallRoomSuf, params)
          maybeAudSz        <- strOptBinder.bind(key  + audSzSuf, params)
        } yield {
          val maybeAudSz1 = maybeAudSz.flatMap {
            audSzStr => AudienceSizes.maybeWithName(audSzStr)
          }
          Right(SMJoinAnswers(
            haveWifi = maybeHaveWifi,
            fullCoverage = maybeFullCover,
            knownEquip = maybeKnownEquip,
            altFw = maybeAltFw,
            isWrtFw = maybeWrtFw,
            landlineInet = maybeLandlineInet,
            smallRoom = maybeSmallRoom,
            audienceSz = maybeAudSz1
          ))
        }
      }

      override def unbind(key: String, value: SMJoinAnswers): String = {
        List(
          boolOptBinder.unbind(key + haveWifiSuf, value.haveWifi),
          boolOptBinder.unbind(key + fullCoverSuf, value.fullCoverage),
          boolOptBinder.unbind(key + knownEquipSuf, value.knownEquip),
          boolOptBinder.unbind(key + altFwSuf, value.altFw),
          boolOptBinder.unbind(key + wrtFwSuf, value.isWrtFw),
          boolOptBinder.unbind(key + landlineInetSuf, value.landlineInet),
          boolOptBinder.unbind(key + smallRoomSuf, value.smallRoom),
          strOptBinder.unbind(key + audSzSuf, value.audienceSz.map(_.toString))
        )
          .filter { q => !q.isEmpty && !q.endsWith("=") }
          .mkString("&")
      }
    }
  }

  def deserialize(jaRaw: ju.Map[_,_]): SMJoinAnswers = {
    val boolExtractorF = { k: String => Option(jaRaw.get(k)) map booleanParser }
    SMJoinAnswers(
      haveWifi      = boolExtractorF(HAVE_WIFI_ESFN),
      fullCoverage  = boolExtractorF(FULL_COVERAGE_ESFN),
      knownEquip    = boolExtractorF(KNOWN_EQUIP_ESFN),
      altFw         = boolExtractorF(ALT_FW_ESFN),
      isWrtFw       = boolExtractorF(IS_WRT_ESFN),
      landlineInet  = boolExtractorF(LANDLINE_INET_ESFN),
      smallRoom     = boolExtractorF(AUDIENCE_SZ_ESFN),
      audienceSz    = Option(jaRaw get AUDIENCE_SZ_ESFN) flatMap { auSzRaw => AudienceSizes.maybeWithName(stringParser(auSzRaw)) }
    )
  }

 def generateMappingProps: List[DocField] = List(
   FieldBoolean(HAVE_WIFI_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldBoolean(FULL_COVERAGE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldBoolean(KNOWN_EQUIP_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldBoolean(ALT_FW_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldBoolean(IS_WRT_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldBoolean(LANDLINE_INET_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldBoolean(SMALL_ROOM_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
   FieldString(AUDIENCE_SZ_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
 )
}

case class SMJoinAnswers(
  haveWifi        : Option[Boolean],
  fullCoverage    : Option[Boolean],
  knownEquip      : Option[Boolean],
  altFw           : Option[Boolean],
  isWrtFw         : Option[Boolean],
  landlineInet    : Option[Boolean],
  smallRoom       : Option[Boolean],
  audienceSz      : Option[AudienceSize]
) {
  import SMJoinAnswers._

  def isDefined: Boolean = {
    productIterator.exists {
      case x: Option[_] => x.isDefined
      case _            => true
    }
  }
  def isEmpty: Boolean = !isDefined

  def toPlayJson: JsObject = {
    var acc1: FieldsJsonAcc = Nil
    if (haveWifi.isDefined)
      acc1 ::= HAVE_WIFI_ESFN -> JsBoolean(haveWifi.get)
    if (fullCoverage.isDefined)
      acc1 ::= FULL_COVERAGE_ESFN -> JsBoolean(fullCoverage.get)
    if (knownEquip.isDefined)
      acc1 ::= KNOWN_EQUIP_ESFN -> JsBoolean(knownEquip.get)
    if (altFw.isDefined)
      acc1 ::= ALT_FW_ESFN -> JsBoolean(altFw.get)
    if (isWrtFw.isDefined)
      acc1 ::= IS_WRT_ESFN -> JsBoolean(isWrtFw.get)
    if (landlineInet.isDefined)
      acc1 ::= LANDLINE_INET_ESFN -> JsBoolean(landlineInet.get)
    if (smallRoom.isDefined)
      acc1 ::= SMALL_ROOM_ESFN -> JsBoolean(smallRoom.get)
    if (audienceSz.isDefined)
      acc1 ::= AUDIENCE_SZ_ESFN -> JsString(audienceSz.get.toString)
    JsObject(acc1)
  }
}



object MirMeta {

  // Названия ES-полей
  val COMPANY_ESFN        = "company"
  val AUDIENCE_DESCR_ESFN = "audDescr"
  val HUMAN_TRAFFIC_ESFN  = "humanTraffic"
  val ADDRESS_ESFN        = "addr"
  val SITE_URL_ESFN       = "siteUrl"
  val OFFICE_PHONE_ESFN   = "oPhone"
  val INFO_ESFN           = "info"
  val FLOOR_ESFN          = "floor"
  val SECTION_ESFN        = "section"
  val PAY_REQS_ESFN       = "payReq"
  val DATE_CREATED_ESFN   = "dateCreated"

  def empty = {
    val str0 = ""
    MirMeta(str0, str0, str0)
  }

  def generateMappingProps: List[DocField] = List(
    FieldString(COMPANY_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(AUDIENCE_DESCR_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(HUMAN_TRAFFIC_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(ADDRESS_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(SITE_URL_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(OFFICE_PHONE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(INFO_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(FLOOR_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldString(SECTION_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
  )

  def deserialize(metaRaw: ju.Map[_,_]): MirMeta = {
    val meta2 = MirMeta.empty
    metaRaw.foreach {
      case (COMPANY_ESFN, companyRaw)           => meta2.company = stringParser(companyRaw)
      case (AUDIENCE_DESCR_ESFN, audDescrRaw)   => meta2.audienceDescr = Option(stringParser(audDescrRaw))
      case (HUMAN_TRAFFIC_ESFN, htRaw)          => meta2.humanTraffic = Option(stringParser(htRaw))
      case (ADDRESS_ESFN, addrRaw)              => meta2.address = stringParser(addrRaw)
      case (SITE_URL_ESFN, siteUrlRaw)          => meta2.siteUrl = Option(stringParser(siteUrlRaw))
      case (OFFICE_PHONE_ESFN, cphRaw)          => meta2.officePhone = stringParser(cphRaw)
      case (INFO_ESFN, infoRaw)                 => meta2.info = Option(stringParser(infoRaw))
      case (PAY_REQS_ESFN, payReqsRaw)          => meta2.payReqs = Option(stringParser(payReqsRaw))
      case (FLOOR_ESFN, floorRaw)               => meta2.floor = Option(stringParser(floorRaw))
      case (SECTION_ESFN, sectionRaw)           => meta2.section = Option(stringParser(sectionRaw))
      case (DATE_CREATED_ESFN, dcRaw)           => meta2.dateCreated = dateTimeParser(dcRaw)
    }
    meta2
  }
}


/** Метаданные, введённые юзером. */
case class MirMeta(
  var company: String,
  var address: String,
  var officePhone: String,
  var audienceDescr: Option[String] = None,
  var humanTraffic: Option[String] = None,
  var siteUrl: Option[String] = None,
  var info: Option[String] = None,
  var floor: Option[String] = None,
  var section: Option[String] = None,
  var payReqs: Option[String] = None,
  var dateCreated: DateTime = DateTime.now()
) {
  import MirMeta._

  def toPlayJson: JsObject = {
    var acc1: FieldsJsonAcc = List(
      COMPANY_ESFN          -> JsString(company),
      ADDRESS_ESFN          -> JsString(address),
      OFFICE_PHONE_ESFN     -> JsString(officePhone),
      DATE_CREATED_ESFN     -> date2JsStr(dateCreated)
    )
    if (siteUrl.isDefined)
      acc1 ::= SITE_URL_ESFN -> JsString(siteUrl.get)
    if (audienceDescr.isDefined)
      acc1 ::= AUDIENCE_DESCR_ESFN -> JsString(audienceDescr.get)
    if (humanTraffic.isDefined)
      acc1 ::= HUMAN_TRAFFIC_ESFN -> JsString(humanTraffic.get)
    if (info.isDefined)
      acc1 ::= INFO_ESFN -> JsString(info.get)
    if (floor.isDefined)
      acc1 ::= FLOOR_ESFN -> JsString(floor.get)
    if (section.isDefined)
      acc1 ::= SECTION_ESFN -> JsString(section.get)
    JsObject(acc1)
  }

}




object MirProgress {

  val COMPANY_ID_ESFN     = "companyId"
  val ADN_ID_ESFN         = "adnId"
  val INVITE_SEND_AT_ESFN = "inviteSendAt"
  val NOTES_ESFN          = "notes"

  def generateMappingProps: List[DocField] = List(
    FieldString(COMPANY_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(ADN_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldDate(INVITE_SEND_AT_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(NOTES_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false)
  )
  
  def deserialize(jmap: ju.Map[_,_]): MirProgress = {
    MirProgress(
      companyIdOpt = Option(jmap get COMPANY_ID_ESFN) map stringParser,
      adnIdOpt     = Option(jmap get ADN_ID_ESFN) map stringParser,
      inviteSentAt = Option(jmap get INVITE_SEND_AT_ESFN) map dateTimeParser,
      notes        = Option(jmap get NOTES_ESFN) map stringParser
    )
  }
}

/**
 * Экземпляр класса собирает в себе отметки о ходе работы по оформлению инвайта.
 * @param companyIdOpt id компании, если создана.
 * @param adnIdOpt id узла рекламной сети, если существует.
 * @param inviteSentAt дата отсылки инвайта, если таков был.
 * @param notes Какие-то административные отметки в произвольной форме.
 */
case class MirProgress(
  companyIdOpt: Option[String] = None,
  adnIdOpt: Option[String] = None,
  inviteSentAt: Option[DateTime] = None,
  notes: Option[String] = None
) {
  import MirProgress._

  def isDefined: Boolean = {
    productIterator.exists {
      case x: Option[_] => x.isDefined
      case _ => true
    }
  }
  def isEmpty = !isDefined

  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = Nil
    if (companyIdOpt.isDefined)
      acc ::= COMPANY_ID_ESFN -> JsString(companyIdOpt.get)
    if (adnIdOpt.isDefined)
      acc ::= ADN_ID_ESFN -> JsString(adnIdOpt.get)
    if (inviteSentAt.isDefined)
      acc ::= INVITE_SEND_AT_ESFN -> date2JsStr(inviteSentAt.get)
    if (notes.isDefined)
      acc ::= NOTES_ESFN -> JsString(notes.get)
    JsObject(acc)
  }
}
