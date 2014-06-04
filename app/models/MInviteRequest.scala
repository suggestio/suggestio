package models

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.14 13:25
 * Description: Запросы на инвайты.
 */
object MInviteRequest extends EsModelStaticT with PlayMacroLogsImpl {

  import LOGGER._

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

  override type T = MInviteRequest

  override val ES_TYPE_NAME = "invReq"

  override protected def dummy(id: String, version: Long): T = {
    MInviteRequest(
      company = "", audienceDescr = "", humanTraffic = "", address = "", siteUrl = None, contactPhone = ""
    )
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = true),
    FieldSource(enabled = true)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(COMPANY_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(AUDIENCE_DESCR_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(HUMAN_TRAFFIC_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(ADDRESS_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(SITE_URL_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(CONTACT_PHONE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(INFO_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldBoolean(HAVE_WIFI_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(FULL_COVERAGE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(KNOWN_EQUIP_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(ALT_FW_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(IS_WRT_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(LANDLINE_INET_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(SMALL_ROOM_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldBoolean(AUDIENCE_SZ_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  )

  import EsModel._
  override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    case (COMPANY_ESFN, companyRaw)           => acc.company = stringParser(companyRaw)
    case (AUDIENCE_DESCR_ESFN, audDescrRaw)   => acc.audienceDescr = stringParser(audDescrRaw)
    case (HUMAN_TRAFFIC_ESFN, htRaw)          => acc.humanTraffic = stringParser(htRaw)
    case (ADDRESS_ESFN, addrRaw)              => acc.address = stringParser(addrRaw)
    case (SITE_URL_ESFN, siteUrlRaw)          => acc.siteUrl = Option(stringParser(siteUrlRaw))
    case (CONTACT_PHONE_ESFN, cphRaw)         => acc.contactPhone = stringParser(cphRaw)
    case (INFO_ESFN, infoRaw)                 => acc.info = Option(stringParser(infoRaw))
    case (HAVE_WIFI_ESFN, hwRaw)              => acc.haveWifi = Option(booleanParser(hwRaw))
    case (FULL_COVERAGE_ESFN, fcRaw)          => acc.fullCoverage = Option(booleanParser(fcRaw))
    case (KNOWN_EQUIP_ESFN, knowEquipRaw)     => acc.knownEquip = Option(booleanParser(knowEquipRaw))
    case (ALT_FW_ESFN, altFwRaw)              => acc.altFw = Option(booleanParser(altFwRaw))
    case (IS_WRT_ESFN, isWrtRaw)              => acc.isWrtFw = Option(booleanParser(isWrtRaw))
    case (LANDLINE_INET_ESFN, llInetRaw)      => acc.landlineInet = Option(booleanParser(llInetRaw))
    case (SMALL_ROOM_ESFN, srRaw)             => acc.smallRoom = Option(booleanParser(srRaw))
    case (AUDIENCE_SZ_ESFN, auSzRaw)          => acc.audienceSz = AudienceSizes.maybeWithName(stringParser(auSzRaw))
  }
}

import MInviteRequest._

case class MInviteRequest(
  var company: String,
  var audienceDescr: String,
  var humanTraffic: String,
  var address: String,
  var siteUrl: Option[String] = None,
  var contactPhone: String,
  var info: Option[String] = None,
  var haveWifi: Option[Boolean] = None,
  var fullCoverage: Option[Boolean] = None,
  var knownEquip: Option[Boolean] = None,
  var altFw: Option[Boolean] = None,
  var isWrtFw: Option[Boolean] = None,
  var landlineInet: Option[Boolean] = None,
  var smallRoom: Option[Boolean] = None,
  var audienceSz: Option[AudienceSize] = None,
  id: Option[String] = None
) extends EsModelT {

  override type T = MInviteRequest

  override def versionOpt = None

  override def companion = MInviteRequest

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc =
      COMPANY_ESFN          -> JsString(company) ::
      AUDIENCE_DESCR_ESFN   -> JsString(audienceDescr) ::
      HUMAN_TRAFFIC_ESFN    -> JsString(humanTraffic) ::
      ADDRESS_ESFN          -> JsString(address) ::
      CONTACT_PHONE_ESFN    -> JsString(contactPhone) ::
      acc
    if (siteUrl.isDefined)
      acc1 ::= SITE_URL_ESFN -> JsString(siteUrl.get)
    if (info.isDefined)
      acc1 ::= INFO_ESFN -> JsString(info.get)
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
    acc1
  }
}
