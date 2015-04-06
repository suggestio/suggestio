package controllers.sysctl

import controllers.SioController
import models._
import models.usr.EmailActivation
import play.api.data._, Forms._
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import util.FormUtil._
import io.suggest.ym.model.common.AdnMemberShowLevels.LvlMap_t
import io.suggest.ym.model.common.{NodeConf, AdnMemberShowLevels}
import util.{TplFormatUtilT, PlayMacroLogsImpl}
import util.acl.AbstractRequestWithPwOpt
import util.mail.MailerWrapper


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 21:37
 * Description: Утиль для контроллеров Sys-Market. Формы, код и т.д.
 */
object SysMarketUtil extends PlayMacroLogsImpl {

  import LOGGER._
  
  /** Маппер для метаданных компании. */
  def companyMetaM = {
    mapping(
      "name" -> companyNameM
    )
    { name => MCompanyMeta.apply(name) }
    { meta => Some(meta.name) }
  }


  /** Маппинг для формы добавления/редактирования компании. */
  def companyFormM = {
    val m = mapping(
      "meta" -> companyMetaM
    )
    { meta => MCompany(meta = meta) }
    { mc   => Some(mc.meta) }
    Form(m)
  }

  /** Функция для обновления компании отмаппленными данными companyFormM.
    * @param mc Текущий (исходный) инстанс компании.
    * @param mc2 Результат маппинга companyFormM.
    * @return Новый инстанс, содержащий в себе былые данные, местами перезаписанные новыми.
    */
  def updateCompany(mc: MCompany, mc2: MCompany): MCompany = {
    mc.copy(
      meta = mc.meta.copy(
        name = mc2.meta.name
      )
    )
  }

  
  /** Форма для маппинг метаданных произвольного узла ADN. */
  def adnNodeMetaM = mapping(
    "name"      -> nameM,
    "nameShort" -> optional(text(maxLength = 25))
      .transform [Option[String]] (emptyStrOptToNone, identity),
    "descr"     -> publishedTextOptM,
    "town"      -> townOptM,
    "address"   -> addressOptM,
    "phone"     -> phoneOptM,
    "floor"     -> floorOptM,
    "section"   -> sectionOptM,
    "siteUrl"   -> urlStrOptM,
    "color"     -> colorOptM
  )
  {(name, nameShort, descr, town, address, phone, floor, section, siteUrl, color) =>
    AdnMMetadata(
      name    = name,
      nameShortOpt = nameShort,
      description = descr,
      town    = town,
      address = address,
      phone   = phone,
      floor   = floor,
      section = section,
      siteUrl = siteUrl,
      color   = color
    )
  }
  {meta =>
    import meta._
    Some((name, nameShortOpt, description, town, address, phone, floor, section, siteUrl, color))
  }

  def adnRightsM: Mapping[Set[AdnRight]] = {
    import AdnRights._
    mapping(
      PRODUCER.longName -> boolean,
      RECEIVER.longName -> boolean,
      SUPERVISOR.longName -> boolean
    )
    {(isProd, isRcvr, isSup) =>
      var acc: List[AdnRight] = Nil
      if (isProd) acc ::= PRODUCER
      if (isRcvr) acc ::= RECEIVER
      if (isSup)  acc ::= SUPERVISOR
      acc.toSet
    }
    {rights =>
      val isProd = rights contains PRODUCER
      val isRcvr = rights contains RECEIVER
      val isSup  = rights contains SUPERVISOR
      Some((isProd, isRcvr, isSup))
    }
  }

  def adnSlInfoM: Mapping[AdnMemberShowLevels] = {
    val slsStrOptM: Mapping[LvlMap_t] = default(slsStrM, Map.empty)
    mapping(
      "in"  -> slsStrOptM,
      "out" -> slsStrOptM
    )
    { AdnMemberShowLevels.apply }
    { AdnMemberShowLevels.unapply }
  }

  /** Доступная узлу пути рекламной выдачи. */
  def adnSinksM: Mapping[Set[AdnSink]] = {
    mapping(
      AdnSinks.SINK_WIFI.longName -> boolean,
      AdnSinks.SINK_GEO.longName  -> boolean
    )
    {(isWifi, isGeo) =>
      var acc = List.empty[AdnSink]
      if (isWifi)
        acc ::= AdnSinks.SINK_WIFI
      if (isGeo)
        acc ::= AdnSinks.SINK_GEO
      acc.toSet
    }
    {sinks =>
      val isWifi = sinks contains AdnSinks.SINK_WIFI
      val isGeo  = sinks contains AdnSinks.SINK_GEO
      Some((isWifi, isGeo))
    }
  }

  /** Маппинг для adn-полей формы adn-узла. */
  def adnMemberM: Mapping[AdNetMemberInfo] = mapping(
    "memberType"    -> adnMemberTypeM,
    "isEnabled"     -> boolean,
    "shownTypeIdOpt" -> adnShownTypeIdOptM,
    "rights"        -> adnRightsM,
    "sls"           -> adnSlInfoM,
    "supId"         -> optional(esIdM),
    "advDelegate"   -> optional(esIdM),
    "testNode"      -> boolean,
    "isUser"        -> boolean,
    "sink"          -> adnSinksM
  )
  {(mt, isEnabled, shownTypeIdOpt, rights, sls, supId, advDgOpt, isTestNode, isUser, sinks) =>
    mt.getAdnInfoDflt.copy(
      isEnabled = isEnabled,
      rights    = rights,
      shownTypeIdOpt = shownTypeIdOpt,
      showLevelsInfo = sls,
      supId     = supId,
      advDelegate = advDgOpt,
      testNode  = isTestNode,
      isUser    = isUser,
      sinks     = sinks
    )
  }
  {anmi =>
    import anmi._
    Some((memberType, isEnabled, Some(shownTypeId), rights, showLevelsInfo, supId, advDelegate, testNode, isUser, sinks))
  }


  /** Маппинг для конфига ноды. */
  def nodeConfM: Mapping[NodeConf] = {
    val intSetM = text(maxLength = 1024)
      .transform [Set[Int]] (
        {raw =>
          raw.trim
            .split("\\s*,\\s*")
            .foldLeft [List[Int]] (Nil) { (acc, vRaw) =>
              val vOpt: Option[Int] = try {
                val blockId = vRaw.toInt
                BlocksConf.apply(blockId)   // Проверяем, есть ли блок с указанным id.
                Some(blockId)
              } catch {
                case ex: NumberFormatException =>
                  warn("Cannot parse block id as integer: " + vRaw)
                  None
                case ex: NoSuchElementException =>
                  warn("Block id not found: " + vRaw)
                  None
              }
              vOpt match {
                case Some(blockId) => blockId :: acc
                case None => acc
              }
            }
            .toSet
        },
        { _.mkString(", ") }
      )
    mapping(
      "showInScNodeList"    -> boolean,
      "withBlocks"          -> default [Set[Int]] (intSetM, Set.empty),
      "showcaseVoidFiller"  -> {
        optional(
          text(maxLength = 255).transform(strTrimF, strIdentityF)
        ).transform[Option[String]] (emptyStrOptToNone, identity)
      }
    )
    { NodeConf.apply }
    { NodeConf.unapply }
  }
  
  
  /** Накатить отмаппленные изменения на существующий интанс узла, породив новый интанс.*/
  def updateAdnNode(adnNode: MAdnNode, adnNode2: MAdnNode): MAdnNode = {
    adnNode.copy(
      companyId = adnNode2.companyId,
      personIds = adnNode2.personIds,
      meta = adnNode.meta.copy(
        name    = adnNode2.meta.name,
        nameShortOpt = adnNode2.meta.nameShortOpt,
        description = adnNode2.meta.description,
        town    = adnNode2.meta.town,
        address = adnNode2.meta.address,
        phone   = adnNode2.meta.phone,
        floor   = adnNode2.meta.floor,
        section = adnNode2.meta.section,
        siteUrl = adnNode2.meta.siteUrl,
        color   = adnNode2.meta.color
      ),
      adn = adnNode.adn.copy(
        memberType  = adnNode2.adn.memberType,
        rights      = adnNode2.adn.rights,
        shownTypeIdOpt = adnNode2.adn.shownTypeIdOpt,
        isEnabled   = adnNode2.adn.isEnabled,
        showLevelsInfo = adnNode2.adn.showLevelsInfo,
        supId       = adnNode2.adn.supId,
        advDelegate = adnNode2.adn.advDelegate,
        testNode    = adnNode2.adn.testNode,
        isUser      = adnNode2.adn.isUser,
        sinks       = adnNode2.adn.sinks
      ),
      conf = adnNode.conf.copy(
        showInScNodesList = adnNode2.conf.showInScNodesList,
        withBlocks = adnNode2.conf.withBlocks,
        showcaseVoidFiller = adnNode2.conf.showcaseVoidFiller
      )
    )
  }


  /** Маппер для поля, содержащего список id юзеров. */
  def personIdsM: Mapping[Set[String]] = {
    text(maxLength = 1024)
      .transform[Set[String]](
        {s => s.trim.split("\\s*[,;]\\s*").filter(!_.isEmpty).toSet },
        { _.mkString(", ") }
      )
  }

  def adnKM  = "adn" -> adnMemberM
  def metaKM = "meta" -> adnNodeMetaM
  def confKM = "conf" -> nodeConfM
  def personIdsKM = "personIds" -> personIdsM

  /** Генератор маппингов для формы добавления/редактирования рекламного узла. */
  def getAdnNodeFormM(companyM: Mapping[String] = esIdM): Form[MAdnNode] = {
    Form(mapping(
      "companyId" -> optional(companyM), adnKM, metaKM, confKM, personIdsKM
    )
    {(companyId, anmi, meta, conf, personIds) =>
      MAdnNode(
        meta = meta,
        companyId = companyId,
        adn = anmi,
        conf = conf,
        personIds = personIds
      )
    }
    {adnNode =>
      import adnNode._
      Some((companyId, adn, meta, conf, personIds))
    })
  }
  def adnNodeFormM = getAdnNodeFormM()


  def nodeOwnerInviteFormM = Form(
    "email" -> email
  )

}


/** Функционал контроллеров для отправки письма с доступом на узел. */
trait SmSendEmailInvite extends SioController {

  /** Выслать письмо активации. */
  def sendEmailInvite(ea: EmailActivation, adnNode: MAdnNode)(implicit request: AbstractRequestWithPwOpt[AnyContent]) {
    // Собираем и отправляем письмо адресату
    val msg = MailerWrapper.instance
    val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
    msg.setSubject("Suggest.io | " + Messages("Your")(ctx.lang) + " " + Messages("amt.of.type." + adnNode.adn.shownTypeId)(ctx.lang))
    msg.setFrom("no-reply@suggest.io")
    msg.setRecipients(ea.email)
    msg.setHtml( views.html.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, ea)(ctx) )
    msg.send()
  }

}
