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

  val COMPANY_ESFN        = "company"
  val AUDIENCE_DESCR_ESFN = "audDescr"
  val HUMAN_TRAFFIC_ESFN  = "humanTraffic"
  val ADDRESS_ESFN        = "addr"
  val SITE_URL_ESFN       = "siteUrl"
  val CONTACT_PHONE_ESFN  = "cPhone"
  val INFO_ESFN           = "info"

  val HAVE_WIFI_ESFN      = "haveWifi"
  val FULL_COVERAGE_ESFN  = "fullCover"
  val KNOWN_EQUIP_ESFN    = "knownEquip"
  val ALT_FW_ESFN         = "altFw"
  val IS_WRT_ESFN         = "isWrt"
  val LANDLINE_INET_ESFN  = "llInet"
  val SMALL_ROOM_ESFN     = "sr"
  val AUDIENCE_SZ_ESFN    = "audSz"

  val FLOOR_ESFN          = "floor"
  val SECTION_ESFN        = "section"
  val PAY_REQS_ESFN       = "payReq"

  override type T = MInviteRequest

  override val ES_TYPE_NAME = "invReq"

  override protected def dummy(id: String, version: Long): T = {
    MInviteRequest(id = Some(id), reqType = null, meta = null)
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = true),
    FieldSource(enabled = true)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(REQ_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldObject(META_ESFN, enabled = true, properties = Seq(
      FieldString(COMPANY_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(AUDIENCE_DESCR_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(HUMAN_TRAFFIC_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(ADDRESS_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(SITE_URL_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(CONTACT_PHONE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(INFO_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(FLOOR_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(SECTION_ESFN, index = FieldIndexingVariants.no, include_in_all = true)
    )),
    FieldObject(JOIN_ANSWERS_ESFN, enabled = true, properties = Seq(
      FieldBoolean(HAVE_WIFI_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(FULL_COVERAGE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(KNOWN_EQUIP_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(ALT_FW_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(IS_WRT_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(LANDLINE_INET_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(SMALL_ROOM_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(AUDIENCE_SZ_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    ))
  )

  import EsModel._
  override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    case (REQ_TYPE_ESFN, rtRaw)            => acc.reqType = InviteReqTypes.withName(stringParser(rtRaw))
    case (META_ESFN, metaRaw: ju.Map[_,_]) =>
      val meta2 = MirMeta.empty
      metaRaw.foreach {
        case (COMPANY_ESFN, companyRaw)           => meta2.company = stringParser(companyRaw)
        case (AUDIENCE_DESCR_ESFN, audDescrRaw)   => meta2.audienceDescr = Option(stringParser(audDescrRaw))
        case (HUMAN_TRAFFIC_ESFN, htRaw)          => meta2.humanTraffic = Option(stringParser(htRaw))
        case (ADDRESS_ESFN, addrRaw)              => meta2.address = stringParser(addrRaw)
        case (SITE_URL_ESFN, siteUrlRaw)          => meta2.siteUrl = Option(stringParser(siteUrlRaw))
        case (CONTACT_PHONE_ESFN, cphRaw)         => meta2.contactPhone = stringParser(cphRaw)
        case (INFO_ESFN, infoRaw)                 => meta2.info = Option(stringParser(infoRaw))
        case (PAY_REQS_ESFN, payReqsRaw)          => meta2.payReqs = Option(stringParser(payReqsRaw))
        case (FLOOR_ESFN, floorRaw)               => meta2.floor = Option(stringParser(floorRaw))
        case (SECTION_ESFN, sectionRaw)           => meta2.section = Option(stringParser(sectionRaw))
      }
      acc.meta = meta2
    case (JOIN_ANSWERS_ESFN, jaRaw: ju.Map[_,_]) =>
      val boolExtractorF = { k: String => Option(jaRaw.get(k)) map booleanParser }
      val ja2 = SMJoinAnswers(
        haveWifi = boolExtractorF(HAVE_WIFI_ESFN),
        fullCoverage = boolExtractorF(FULL_COVERAGE_ESFN),
        knownEquip = boolExtractorF(KNOWN_EQUIP_ESFN),
        altFw = boolExtractorF(ALT_FW_ESFN),
        isWrtFw = boolExtractorF(IS_WRT_ESFN),
        landlineInet = boolExtractorF(LANDLINE_INET_ESFN),
        smallRoom = boolExtractorF(AUDIENCE_SZ_ESFN),
        audienceSz = Option(jaRaw get AUDIENCE_SZ_ESFN) flatMap { auSzRaw => AudienceSizes.maybeWithName(stringParser(auSzRaw)) }
      )
      acc.joinAnswers = Some(ja2)
  }
}

import MInviteRequest._

case class MInviteRequest(
  var reqType: InviteReqType,
  var meta: MirMeta,
  var joinAnswers: Option[SMJoinAnswers] = None,
  id: Option[String] = None
) extends EsModelT {

  override type T = MInviteRequest

  override def versionOpt = None

  override def companion = MInviteRequest

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc =
      REQ_TYPE_ESFN -> JsString(reqType.toString) ::
      META_ESFN     -> meta.toPlayJson ::
      acc
    if (joinAnswers.isDefined)
      acc1 ::= JOIN_ANSWERS_ESFN -> joinAnswers.get.toPlayJson
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

  private def haveWifiSuf     = ".haveWifi"
  private def fullCoverSuf    = ".fullCover"
  private def knownEquipSuf   = ".knownEquip"
  private def altFwSuf        = ".altFw"
  private def wrtFwSuf        = ".wrtFw"
  private def landlineInetSuf = ".landlineInet"
  private def smallRoomSuf    = ".smallRoom"
  private def audSzSuf        = ".audSz"

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
  def empty = {
    val str0 = ""
    MirMeta(str0, str0, str0)
  }
}


/** Метаданные, введённые юзером. */
case class MirMeta(
  var company: String,
  var address: String,
  var contactPhone: String,
  var audienceDescr: Option[String] = None,
  var humanTraffic: Option[String] = None,
  var siteUrl: Option[String] = None,
  var info: Option[String] = None,
  var floor: Option[String] = None,
  var section: Option[String] = None,
  var payReqs: Option[String] = None
) {

  def toPlayJson: JsObject = {
    var acc1: FieldsJsonAcc = List(
      COMPANY_ESFN          -> JsString(company),
      ADDRESS_ESFN          -> JsString(address),
      CONTACT_PHONE_ESFN    -> JsString(contactPhone)
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