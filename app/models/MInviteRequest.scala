package models

import _root_.util.qsb.QsbUtil
import com.google.inject.Inject
import io.suggest.common.menum.EnumValue2Val
import io.suggest.model.common.{EMNameStaticMut, EMDateCreatedStatic, EMNameMut, EMDateCreatedMut}
import io.suggest.model.es._
import models.usr.EmailActivation
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import EsModelUtil.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import io.suggest.util.SioEsUtil.FieldAll
import io.suggest.util.SioEsUtil.FieldBoolean
import io.suggest.util.SioEsUtil.FieldString
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import io.suggest.util.SioEsUtil.FieldSource
import io.suggest.event.SioNotifierStaticClientI
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import play.api.mvc.QueryStringBindable
import java.{util => ju}
import scala.collection.JavaConversions._
import EsModelUtil.{stringParser, booleanParser, intParser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.14 13:25
 * Description: Запросы на инвайты.
 */

class MInviteRequest_
  extends EsModelStaticMutAkvEmptyT
  with EsModelStaticT
  with EMInviteRequestStatic
  with EMDateCreatedStatic
  with EMNameStaticMut
  with PlayMacroLogsImpl
{

  override type T = MInviteRequest

  override val ES_TYPE_NAME = "invReq"

  override def dummy(id: Option[String], version: Option[Long]): T = {
    MInviteRequest(
      id = id,
      versionOpt = version,
      reqType = null,
      company = null,
      name = "",
      companion = this
    )
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = true),
    FieldSource(enabled = true)
  )

  // Служебные фунции модели.
  // Генерации json-объектов, содержащих поле _id
  private def strIdObj(id: String) = idObj( JsString(id) )
  private def intIdObj(id: Int) = idObj( JsNumber(id) )
  private def idObj(id: JsValue) = JsObject(Seq(ID_ESFN -> id))

  /** Генерация play JSON на основе того или иного состояния поля с моделью, у которой строковые id. */
  def strModel2json(esm: Either[ToPlayJsonObj, String]) = model2json(esm, strIdObj)
  /** Генерация play JSON на основе того или иного состояния поля с моделью, у которой числовые id. */
  def intModel2json(sqm: Either[ToPlayJsonObj, Int]) = model2json(sqm, intIdObj)
  def model2json[IdT](m: Either[ToPlayJsonObj, IdT],  idObjF: IdT => JsObject): JsObject = {
    m.fold[JsObject](_.toPlayJsonWithId, idObjF)
  }


  /** Стереть ресурсы, которые прилинкованы к Left()-шаблонам моделей. */
  def withEraseLeftResources(fut0: Future[_], next: Either[EraseResources, _])
                                    (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    next.fold(
      { m => m.eraseResources flatMap { _ => fut0 } },
      { _ => fut0 }
    )
  }
}


/** Экземпляр модели, хранящий данные по запросу подключения и состояние его обработки. */
final case class MInviteRequest(
  var name      : String,
  var reqType   : InviteReqType,
  override val companion: MInviteRequest_,
  var company   : Either[MCompany, String],
  var waOpt     : Option[Either[MWelcomeAd, String]] = None,
  var adnNode   : Option[Either[MNode, String]] = None,
  var contract  : Option[Either[MBillContract, Int]] = None,
  var mmp       : Option[Either[MBillMmpDaily, Int]] = None,
  var balance   : Option[Either[MBillBalance, String]] = None,
  var emailAct  : Option[Either[EmailActivation, String]] = None,
  var joinAnswers: Option[SMJoinAnswers] = None,
  var dateCreated: DateTime = DateTime.now(),
  var payReqsRaw : Option[String] = None,
  id            : Option[String] = None,
  versionOpt    : Option[Long] = None
)
  extends EsModelPlayJsonEmpty
  with EsModelT
  with EMInviteRequestMut
  with EMDateCreatedMut
  with EMNameMut
{
  override type T = MInviteRequest

  /** Стирание ресурсов, относящихся к этой модели. */
  override def doEraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    var fut = super.doEraseResources
    fut = companion.withEraseLeftResources(fut, company)
    fut = adnNode.fold(fut) { adnNodeEith =>
      companion.withEraseLeftResources(fut, adnNodeEith)
    }
    fut = emailAct.fold(fut) { emailActEith =>
      companion.withEraseLeftResources(fut, emailActEith)
    }
    fut = waOpt.fold(fut) {
      _waOpt => companion.withEraseLeftResources(fut, _waOpt)
    }
    fut
  }
}



/** Типы запросов на подключение. */
object InviteReqTypes extends EnumValue2Val {

  protected class Val(val name: String)
    extends super.Val(name)
  {
    def l10n = "mir.type." + name
  }

  override type T = Val

  val Adv   : T = new Val("a")
  val Wifi  : T = new Val("w")

}



/** mbean-интерфейс для JMX. */
trait MInviteRequestJmxMBean extends EsModelJMXMBeanI
/** Реализация mbean'a: */
class MInviteRequestJmx @Inject() (
  override val companion          : MInviteRequest_,
  override implicit val ec        : ExecutionContext,
  override implicit val client    : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends EsModelJMXBase
  with MInviteRequestJmxMBean




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
            haveWifi      = maybeHaveWifi,
            fullCoverage  = maybeFullCover,
            knownEquip    = maybeKnownEquip,
            altFw         = maybeAltFw,
            isWrtFw       = maybeWrtFw,
            landlineInet  = maybeLandlineInet,
            smallRoom     = maybeSmallRoom,
            audienceSz    = maybeAudSz1
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

/** Блок с ответами на вопросы. Относится обычно к wifi-подключениям. */
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


// Если import вынести наружу, то будет "illegal cyclic reference involving object MInviteRequest"

/** Аддон для статической части модели [[MInviteRequest]]. */
sealed trait EMInviteRequestStatic extends EsModelStaticMutAkvT {

  override type T <: EMInviteRequestMut

  val REQ_TYPE_ESFN       = "rt"

  val ID_ESFN             = "_id"
  val COMPANY_ESFN        = "co"
  val ADN_NODE_ESFN       = "an"
  val CONTRACT_EFSN       = "mbc"
  val DAILY_MMP_ESFN      = "mmpd"
  val BALANCE_ESFN        = "mbb"
  val EMAIL_ACT_ESFN      = "ea"
  val PAY_REQS_RAW_ESFN   = "prqRaw"
  val WELCOME_AD_ESFN     = "wa"

  val JOIN_ANSWERS_ESFN   = "ja"


  abstract override def generateMappingProps: List[DocField] = {
    val acc = super.generateMappingProps
    val idField = FieldString(ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    FieldString(REQ_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false) ::
      // ES-модели индексируем, чтоб можно было искать потом среди инвайтов.
      FieldObject(COMPANY_ESFN, enabled = true, properties = idField :: MCompany.generateMappingProps) ::
      FieldObject(ADN_NODE_ESFN, enabled = true, properties = idField :: MNode.generateMappingProps) ::
      // SQL-модели: не индексируем.
      FieldObject(CONTRACT_EFSN, enabled = false, properties = Nil) ::
      FieldObject(DAILY_MMP_ESFN, enabled = false, properties = Nil) ::
      FieldObject(BALANCE_ESFN, enabled = false, properties = Nil) ::
      FieldObject(EMAIL_ACT_ESFN, enabled = false, properties = Nil) ::
      // Поле объекта с текущими данными по ответам на вопросы.
      FieldObject(JOIN_ANSWERS_ESFN, enabled = true, properties = SMJoinAnswers.generateMappingProps) ::
      acc
  }

  def deserializeEsModel[X](companion: EsModelStaticT { type T = X }, jmap: ju.Map[_,_]): Either[X, String] = {
    jmap.get(ID_ESFN) match {
      case null =>
        val docId = Option(jmap get "id") map stringParser
        val m = jmap
          .asInstanceOf[ju.Map[String, AnyRef]]
          .filterKeys(_ != "id")
        val r = companion.deserializeOne(docId, m, version = None)
        Left(r)
      case idRaw =>
        Right(stringParser(idRaw))
    }
  }

  def deseralizeSqlModel[X, IdT](companion: FromJson { type T = X },  jmap: ju.Map[_,_],  idParser: Any => IdT): Either[X, IdT] = {
    jmap.get(ID_ESFN) match {
      case null =>
        val r = companion.fromJson(jmap.asInstanceOf[ju.Map[String, AnyRef]])
        Left(r)
      case idRaw =>
        Right(idParser(idRaw))
    }
  }

  def deseralizeSqlIntModel[X](companion: FromJson { type T = X },  jmap: ju.Map[_,_]): Either[X, Int] = {
    deseralizeSqlModel(companion, jmap, intParser)
  }

  def deseralizeSqlStrModel[X](companion: FromJson { type T = X },  jmap: ju.Map[_,_]): Either[X, String] = {
    deseralizeSqlModel(companion, jmap, stringParser)
  }


  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (REQ_TYPE_ESFN, rtRaw) =>
        acc.reqType = InviteReqTypes.withName(stringParser(rtRaw))
      case (COMPANY_ESFN, jmap: ju.Map[_, _]) =>
        acc.company = deserializeEsModel(MCompany, jmap)
      case (ADN_NODE_ESFN, jmap: ju.Map[_, _]) =>
        acc.adnNode = Some( deserializeEsModel(MNode, jmap) )
      case (CONTRACT_EFSN, jmap: ju.Map[_, _]) =>
        acc.contract = Some( deseralizeSqlIntModel(MBillContract, jmap) )
      case (DAILY_MMP_ESFN, jmap: ju.Map[_, _]) =>
        val result = deseralizeSqlIntModel(MBillMmpDaily, jmap)
        acc.mmp = Option(result)
      case (BALANCE_ESFN, jmap: ju.Map[_, _]) =>
        acc.balance = Some( deseralizeSqlStrModel(MBillBalance, jmap) )
      case (EMAIL_ACT_ESFN, jmap: ju.Map[_, _]) =>
        acc.emailAct = Some( deserializeEsModel(EmailActivation, jmap) )
      case (JOIN_ANSWERS_ESFN, jaRaw: ju.Map[_, _]) =>
        acc.joinAnswers = Option(SMJoinAnswers.deserialize(jaRaw))
      case (PAY_REQS_RAW_ESFN, prqRaw) =>
        acc.payReqsRaw = Option(prqRaw) map stringParser
      case (WELCOME_AD_ESFN, waOptRaw: ju.Map[_,_]) =>
        acc.waOpt = Option(waOptRaw) map { deserializeEsModel(MWelcomeAd, _) }
    }
  }

}

/** Аддон для динамической части модели [[MInviteRequest]]. */
sealed trait EMInviteRequestMut extends EsModelPlayJsonT {

  override type T <: EMInviteRequestMut

  var reqType   : InviteReqType
  var company   : Either[MCompany, String]
  var adnNode   : Option[Either[MNode, String]]
  var contract  : Option[Either[MBillContract, Int]]
  var mmp       : Option[Either[MBillMmpDaily, Int]]
  var balance   : Option[Either[MBillBalance, String]]
  var emailAct  : Option[Either[EmailActivation, String]]
  var joinAnswers: Option[SMJoinAnswers]
  var payReqsRaw: Option[String]
  var waOpt     : Option[Either[MWelcomeAd, String]]


  override val companion: MInviteRequest_

  import companion._

  abstract override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc0)
    var acc =
      REQ_TYPE_ESFN   -> JsString(reqType.name) ::
      COMPANY_ESFN    -> strModel2json(company) ::
      acc1
    if (adnNode.isDefined)
      acc ::= ADN_NODE_ESFN -> strModel2json(adnNode.get)
    if (contract.isDefined)
      acc ::= CONTRACT_EFSN -> intModel2json(contract.get)
    if (balance.isDefined)
      acc ::= BALANCE_ESFN -> strModel2json(balance.get)
    if (emailAct.isDefined)
      acc ::= EMAIL_ACT_ESFN -> strModel2json(emailAct.get)
    if (waOpt.isDefined)
      acc ::= WELCOME_AD_ESFN -> strModel2json(waOpt.get)
    if (joinAnswers exists { _.isDefined })
      acc ::= JOIN_ANSWERS_ESFN -> joinAnswers.get.toPlayJson
    if (mmp.isDefined)
      acc ::= DAILY_MMP_ESFN -> intModel2json(mmp.get)
    if (payReqsRaw.isDefined)
      acc ::= PAY_REQS_RAW_ESFN -> JsString(payReqsRaw.get)
    acc
  }
}

