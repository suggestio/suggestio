package controllers.sysctl

import controllers.{IMailer, SioController}
import models._
import models.usr.EmailActivation
import play.api.data._, Forms._
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import util.FormUtil._
import io.suggest.ym.model.common.AdnMemberShowLevels.LvlMap_t
import io.suggest.ym.model.common.{NodeConf, AdnMemberShowLevels}
import util.PlayMacroLogsDyn
import util.acl.AbstractRequestWithPwOpt

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 21:37
 * Description: Утиль для контроллеров Sys-Market. Формы, код и т.д.
 */
object SysMarketUtil extends PlayMacroLogsDyn {

  /** Форма для маппинг метаданных произвольного узла ADN. */
  def adnNodeMetaM = mapping(
    "name"          -> nameM,
    "nameShort"     -> optional(text(maxLength = 25))
      .transform [Option[String]] (emptyStrOptToNone, identity),
    "hiddenDescr"   -> publishedTextOptM,
    "town"          -> townOptM,
    "address"       -> addressOptM,
    "phone"         -> phoneOptM,
    "floor"         -> floorOptM,
    "section"       -> sectionOptM,
    "siteUrl"       -> urlStrOptM,
    "color"         -> colorOptM
  )
  {(name, nameShort, descr, town, address, phone, floor, section, siteUrl, color) =>
    MNodeMeta(
      nameOpt = Some(name),
      nameShortOpt = nameShort,
      hiddenDescr = descr,
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
    Some((name, nameShortOpt, hiddenDescr, town, address, phone, floor, section, siteUrl, color))
  }

  def adnRightsM: Mapping[Set[AdnRight]] = {
    import AdnRights._
    mapping(
      PRODUCER.longName -> boolean,
      RECEIVER.longName -> boolean
    )
    {(isProd, isRcvr) =>
      var acc: List[AdnRight] = Nil
      if (isProd) acc ::= PRODUCER
      if (isRcvr) acc ::= RECEIVER
      acc.toSet
    }
    {rights =>
      val isProd = rights contains PRODUCER
      val isRcvr = rights contains RECEIVER
      Some((isProd, isRcvr))
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
    "isEnabled"     -> boolean,
    "shownTypeIdOpt" -> adnShownTypeIdOptM,
    "rights"        -> adnRightsM,
    "sls"           -> adnSlInfoM,
    "testNode"      -> boolean,
    "isUser"        -> boolean,
    "sink"          -> adnSinksM
  )
  {(isEnabled, shownTypeIdOpt, rights, sls, isTestNode, isUser, sinks) =>
    AdNetMemberInfo(
      isEnabled = isEnabled,
      rights    = rights,
      shownTypeIdOpt = shownTypeIdOpt,
      showLevelsInfo = sls,
      testNode  = isTestNode,
      isUser    = isUser,
      sinks     = sinks
    )
  }
  {anmi =>
    import anmi._
    Some((isEnabled, Some(shownTypeId), rights, showLevelsInfo, testNode, isUser, sinks))
  }


  /** Маппинг для конфига ноды. */
  def nodeConfM: Mapping[NodeConf] = {
    mapping(
      "showInScNodeList"    -> boolean,
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
      personIds = adnNode2.personIds,
      meta = adnNode.meta.copy(
        nameOpt = adnNode2.meta.nameOpt,
        nameShortOpt = adnNode2.meta.nameShortOpt,
        hiddenDescr = adnNode2.meta.hiddenDescr,
        town    = adnNode2.meta.town,
        address = adnNode2.meta.address,
        phone   = adnNode2.meta.phone,
        floor   = adnNode2.meta.floor,
        section = adnNode2.meta.section,
        siteUrl = adnNode2.meta.siteUrl,
        color   = adnNode2.meta.color
      ),
      adn = adnNode.adn.copy(
        rights      = adnNode2.adn.rights,
        shownTypeIdOpt = adnNode2.adn.shownTypeIdOpt,
        isEnabled   = adnNode2.adn.isEnabled,
        showLevelsInfo = adnNode2.adn.showLevelsInfo,
        testNode    = adnNode2.adn.testNode,
        isUser      = adnNode2.adn.isUser,
        sinks       = adnNode2.adn.sinks
      ),
      conf = adnNode.conf.copy(
        showInScNodesList = adnNode2.conf.showInScNodesList,
        showcaseVoidFiller = adnNode2.conf.showcaseVoidFiller
      )
    )
  }


  /** Маппер для поля, содержащего список id юзеров. */
  def personIdsM: Mapping[Set[String]] = {
    text(maxLength = 1024)
      .transform[Set[String]](
        {s =>
          s.trim
            .split("\\s*[,;]\\s*")
            .iterator
            .filter(!_.isEmpty)
            .toSet
        },
        { _.mkString(", ") }
      )
  }

  def adnKM  = "adn" -> adnMemberM
  def metaKM = "meta" -> adnNodeMetaM
  def confKM = "conf" -> nodeConfM
  def personIdsKM = "personIds" -> personIdsM

  /** Сборка маппинга для формы добавления/редактирования рекламного узла. */
  def adnNodeFormM: Form[MAdnNode] = {
    Form(mapping(
      adnKM, metaKM, confKM, personIdsKM
    )
    {(anmi, meta, conf, personIds) =>
      MAdnNode(
        meta = meta,
        adn = anmi,
        conf = conf,
        personIds = personIds
      )
    }
    {adnNode =>
      import adnNode._
      Some((adn, meta, conf, personIds))
    })
  }


  def nodeOwnerInviteFormM = Form(
    "email" -> email
  )

}


/** Функционал контроллеров для отправки письма с доступом на узел. */
trait SmSendEmailInvite extends SioController with IMailer {

  /** Выслать письмо активации. */
  def sendEmailInvite(ea: EmailActivation, adnNode: MAdnNode)(implicit request: AbstractRequestWithPwOpt[AnyContent]) {
    // Собираем и отправляем письмо адресату
    val msg = mailer.instance
    val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
    msg.setSubject("Suggest.io | " + Messages("Your")(ctx.messages) + " " + Messages("amt.of.type." + adnNode.adn.shownTypeId)(ctx.messages))
    msg.setFrom("no-reply@suggest.io")
    msg.setRecipients(ea.email)
    msg.setHtml( views.html.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, ea)(ctx) )
    msg.send()
  }

}
