package controllers.sysctl

import controllers.SioController
import io.suggest.model.n2.extra.{MNodeExtras, MSlInfo, MAdnExtra}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBusinessInfo, MAddress, MBasicMeta}
import models._
import models.usr.EmailActivation
import play.api.data._, Forms._
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import util.FormUtil._
import io.suggest.ym.model.common.ShowLevelLimits.LvlMap_t
import io.suggest.ym.model.common.ShowLevelLimits
import util.PlayMacroLogsDyn
import util.acl.AbstractRequestWithPwOpt
import util.mail.IMailerWrapperDi

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 21:37
 * Description: Утиль для контроллеров Sys-Market. Формы, код и т.д.
 */
class SysMarketUtil extends PlayMacroLogsDyn {

  /** Форма для маппинг метаданных произвольного узла ADN. */
  def adnNodeMetaM: Mapping[MMeta] = mapping(
    "name"          -> nameM,
    "nameShort"     -> optional(text(maxLength = 25))
      .transform [Option[String]] (emptyStrOptToNone, identity),
    "hiddenDescr"   -> publishedTextOptM,
    "town"          -> townOptM,
    "address"       -> addressOptM,
    "phone"         -> phoneOptM,
    "floor"         -> floorOptM,
    "section"       -> sectionOptM,
    "siteUrl"       -> urlStrOptM
  )
  {(name, nameShort, descr, town, address, phone, floor, section, siteUrl) =>
    MMeta(
      basic = MBasicMeta(
        nameOpt       = Some(name),
        nameShortOpt  = nameShort,
        hiddenDescr   = descr
      ),
      address = MAddress(
        town    = town,
        address = address,
        phone   = phone,
        floor   = floor,
        section = section
      ),
      business = MBusinessInfo(
        siteUrl = siteUrl
      )
    )
  }
  {meta =>
    Some((
      meta.basic.nameOpt.getOrElse(""),
      meta.basic.nameShortOpt,
      meta.basic.hiddenDescr,
      meta.address.town,
      meta.address.address,
      meta.address.phone,
      meta.address.floor,
      meta.address.section,
      meta.business.siteUrl
    ))
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

  def lvlMapOrEmpty: Mapping[LvlMap_t] = {
    default(slsStrM, Map.empty)
  }

  /** Маппинг карты доступных ShowLevels для узлов N2. */
  def mSlInfoMapM: Mapping[Map[AdShowLevel, MSlInfo]] = {
    // TODO Тут из одной карты делается другая. Может стоит реализовать напрямую?
    lvlMapOrEmpty
      .transform[Map[AdShowLevel,MSlInfo]](
        {lvlMap =>
          lvlMap.iterator
            .map { case (sl, limit) =>
              val msli = MSlInfo(sl, limit)
              sl -> msli
            }
            .toMap
        },
        {mslMap =>
          mslMap.iterator
            .map { case (sl, msli) =>
              sl -> msli.limit
            }
            .toMap
        }
      )
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

  def nodeCommonM: Mapping[MNodeCommon] = {
    mapping(
      "isEnabled"     -> boolean,
      "isDependent"   -> boolean
    )
    {(isEnabled, isDependent) =>
      MNodeCommon(
        ntype       = MNodeTypes.AdnNode,     // TODO Это заглушка, рождённая во время переезда на N2. Нужно дать возможность менять тип.
        isEnabled   = isEnabled,
        isDependent = isDependent
      )
    }
    {mnc =>
      Some((mnc.isEnabled, mnc.isDependent))
    }
  }

  /** Маппинг для adn-полей формы adn-узла. */
  def adnExtraM: Mapping[MAdnExtra] = mapping(
    "shownTypeIdOpt" -> adnShownTypeIdOptM,
    "rights"        -> adnRightsM,
    "sls"           -> mSlInfoMapM,
    "testNode"      -> boolean,
    "isUser"        -> boolean,
    "sink"          -> adnSinksM,
    "showInScNl"    -> boolean
  )
  {(shownTypeIdOpt, rights, sls, isTestNode, isUser, sinks, showInScNl) =>
    MAdnExtra(
      rights          = rights,
      shownTypeIdOpt  = shownTypeIdOpt,
      outSls          = sls,
      testNode        = isTestNode,
      isUser          = isUser,
      sinks           = sinks,
      showInScNl      = showInScNl
    )
  }
  {mae =>
    import mae._
    Some((shownTypeIdOpt, rights, outSls, testNode, isUser, sinks, showInScNl))
  }


  /** Накатить отмаппленные изменения на существующий интанс узла, породив новый интанс.*/
  def updateAdnNode(old: MNode, changes: MNode): MNode = {
    old.copy(
      meta = old.meta.copy(
        basic = {
          import changes.meta.basic._
          old.meta.basic.copy(
            nameOpt       = nameOpt,
            nameShortOpt  = nameShortOpt,
            hiddenDescr   = hiddenDescr
          )
        },
        address = old.meta.address,
        business = old.meta.business.copy(
          siteUrl = changes.meta.business.siteUrl
        )
      ),
      extras = old.extras.copy(
        adn = changes.extras.adn
      )
    )
  }

  def adnExtraKM  = "adn"     -> adnExtraM
  def metaKM      = "meta"    -> adnNodeMetaM
  def commonKM    = "common"  -> nodeCommonM

  /** Сборка маппинга для формы добавления/редактирования рекламного узла. */
  def adnNodeFormM: Form[MNode] = {
    Form(mapping(
      commonKM, adnExtraKM, metaKM
    )
    {(common, adnExtra, meta) =>
      MNode(
        common  = common,
        meta    = meta,
        extras  = MNodeExtras(
          adn = Some(adnExtra)
        )
      )
    }
    {mnode =>
      Some((
        mnode.common,
        mnode.extras.adn.getOrElse(MAdnExtra()),
        mnode.meta
      ))
    })
  }


  def nodeOwnerInviteFormM = Form(
    "email" -> email
  )

}


/** Функционал контроллеров для отправки письма с доступом на узел. */
trait SmSendEmailInvite extends SioController with IMailerWrapperDi {

  /** Выслать письмо активации. */
  def sendEmailInvite(ea: EmailActivation, adnNode: MNode)(implicit request: AbstractRequestWithPwOpt[AnyContent]) {
    // Собираем и отправляем письмо адресату
    val msg = mailer.instance
    val ctx = implicitly[Context]
    val ast = adnNode.extras.adn
      .flatMap( _.shownTypeIdOpt )
      .flatMap( AdnShownTypes.maybeWithName )
      .getOrElse( AdnShownTypes.default )
    msg.setSubject("Suggest.io | " +
      Messages("Your")(ctx.messages) + " " +
      Messages(ast.singular)(ctx.messages)
    )
    msg.setFrom("no-reply@suggest.io")
    msg.setRecipients(ea.email)
    msg.setHtml( views.html.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, ea)(ctx) )
    msg.send()
  }

}
