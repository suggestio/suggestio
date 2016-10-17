package util.sys

import io.suggest.model.n2.extra.domain.MDomainExtra
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras, MSlInfo}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MAddress, MBasicMeta, MBusinessInfo}
import io.suggest.model.sc.common.LvlMap_t
import models._
import models.msys.{MSysNodeInstallFormData, NodeCreateParams}
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.PlayMacroLogsDyn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 21:37
 * Description: Утиль для контроллеров Sys-Market. Формы, код и т.д.
 */
class SysMarketUtil extends PlayMacroLogsDyn {

  /** Форма для маппинг метаданных произвольного узла ADN. */
  private def adnNodeMetaM: Mapping[MMeta] = mapping(
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

  private def adnRightsM: Mapping[Set[AdnRight]] = {
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

  private def lvlMapOrEmpty: Mapping[LvlMap_t] = {
    default(slsStrM, Map.empty)
  }

  /** Маппинг карты доступных ShowLevels для узлов N2. */
  private def mSlInfoMapM: Mapping[Map[AdShowLevel, MSlInfo]] = {
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

  private def nodeCommonM: Mapping[MNodeCommon] = {
    mapping(
      "ntype"         -> MNodeTypes.mappingM,
      "isEnabled"     -> boolean,
      "isDependent"   -> boolean
    )
    {(ntype, isEnabled, isDependent) =>
      MNodeCommon(
        // Выставляем null, т.к. при create оно будет перезаписано в контроллере, а при edit -- проигнорировано.
        ntype       = ntype,
        isEnabled   = isEnabled,
        isDependent = isDependent
      )
    }
    {mnc =>
      Some((mnc.ntype, mnc.isEnabled, mnc.isDependent))
    }
  }

  /** Маппинг для adn-полей формы adn-узла. */
  private def adnExtraM: Mapping[MAdnExtra] = mapping(
    "shownTypeIdOpt" -> adnShownTypeIdOptM,
    "rights"        -> adnRightsM,
    "sls"           -> mSlInfoMapM,
    "testNode"      -> boolean,
    "isUser"        -> boolean,
    "showInScNl"    -> boolean
  )
  {(shownTypeIdOpt, rights, sls, isTestNode, isUser, showInScNl) =>
    MAdnExtra(
      rights          = rights,
      shownTypeIdOpt  = shownTypeIdOpt,
      outSls          = sls,
      testNode        = isTestNode,
      isUser          = isUser,
      showInScNl      = showInScNl
    )
  }
  {mae =>
    import mae._
    Some((shownTypeIdOpt, rights, outSls, testNode, isUser, showInScNl))
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


  /** Сборка маппинга для формы добавления/редактирования рекламного узла. */
  def adnNodeFormM: Form[MNode] = {
    Form(mapping(
      "common"  -> nodeCommonM,
      "adn"     -> adnExtraM,
      "meta"    -> adnNodeMetaM
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


  /** Маппинг формы для инсталляции узла. */
  def nodeInstallForm: Form[MSysNodeInstallFormData] = {
    Form(
      mapping(
        "count" -> number(0, max = 50),
        "lang"  -> uiLangM()
      )
      { MSysNodeInstallFormData.apply }
      { MSysNodeInstallFormData.unapply }
    )
  }

  /** Маппинг формы данных по домену. */
  def mDomainExtraFormM: Form[MDomainExtra] = {
    Form( MDomainExtra.mappingM )
  }

  /** Маппинг для форм, содержащих [[NodeCreateParams]]. */
  def nodeCreateParamsM: Mapping[NodeCreateParams] = {
    val b = boolean
    mapping(
      "billInit"      -> b,
      "extTgsInit"    -> b,
      "withDfltMads"  -> b,
      // Для админов допускаем более свободное задание id'шников. При edit это дело игнорируется внутри update-экшена.
      "id" -> optional(text(minLength = 3, maxLength = 128))
        .transform [Option[String]] ( emptyStrOptToNone, identity )
    )
    { NodeCreateParams.apply }
    { NodeCreateParams.unapply }
  }

  def nodeCreateParamsFormM = Form(nodeCreateParamsM)


}


/** Интерфейс для DI-поля с инстансом [[SysMarketUtil]]. */
trait ISysMarketUtilDi {
  def sysMarketUtil: SysMarketUtil
}
