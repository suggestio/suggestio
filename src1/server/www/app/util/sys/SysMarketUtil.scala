package util.sys

import io.suggest.adn.MAdnRight
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.model.n2.extra.domain.MDomainExtra
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.node.{MNode, MNodeTypesJvm}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MAddress, MBasicMeta, MBusinessInfo, MMeta}
import io.suggest.util.logs.MacroLogsDyn
import models.msys.{MSysNodeInstallFormData, NodeCreateParams}
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.FormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 21:37
 * Description: Утиль для контроллеров Sys-Market. Формы, код и т.д.
 */
class SysMarketUtil extends MacroLogsDyn {

  /** Форма для маппинг метаданных произвольного узла ADN. */
  private def adnNodeMetaM: Mapping[MMeta] = mapping(
    "name"          -> nameM,
    "nameShort"     -> optional(text(maxLength = 25))
      .transform [Option[String]] (emptyStrOptToNone, identity),
    "hiddenDescr"   -> publishedTextOptM,
    "town"          -> townOptM,
    "address"       -> addressOptM,
    "siteUrl"       -> urlStrOptM
  )
  {(name, nameShort, descr, town, address, siteUrl) =>
    MMeta(
      basic = MBasicMeta(
        nameOpt       = Some(name),
        nameShortOpt  = nameShort,
        hiddenDescr   = descr
      ),
      address = MAddress(
        town    = town,
        address = address
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
      meta.business.siteUrl
    ))
  }

  private def adnRightsM: Mapping[Set[MAdnRight]] = {
    import io.suggest.adn.MAdnRights._
    mapping(
      PRODUCER.longName -> boolean,
      RECEIVER.longName -> boolean
    )
    {(isProd, isRcvr) =>
      var acc: List[MAdnRight] = Nil
      if (isProd) acc ::= PRODUCER
      if (isRcvr) acc ::= RECEIVER
      acc.toSet
    }
    {rights =>
      val isProd = rights.contains( PRODUCER )
      val isRcvr = rights.contains( RECEIVER )
      Some((isProd, isRcvr))
    }
  }

  private def nodeCommonM: Mapping[MNodeCommon] = {
    mapping(
      "ntype"         -> MNodeTypesJvm.mappingM,
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
    "testNode"      -> boolean,
    "isUser"        -> boolean,
    "showInScNl"    -> boolean
  )
  {(shownTypeIdOpt, rights, isTestNode, isUser, showInScNl) =>
    MAdnExtra(
      rights          = rights,
      shownTypeIdOpt  = shownTypeIdOpt,
      testNode        = isTestNode,
      isUser          = isUser,
      showInScNl      = showInScNl
    )
  }
  {mae =>
    import mae._
    Some((shownTypeIdOpt, rights, testNode, isUser, showInScNl))
  }


  /** Накатить отмаппленные изменения на существующий интанс узла, породив новый интанс.*/
  def updateAdnNode(old: MNode, changes: MNode): MNode = {
    old.copy(
      common = old.common.copy(
        ntype       = changes.common.ntype,
        isEnabled   = changes.common.isEnabled,
        isDependent = changes.common.isDependent
      ),
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
      "id" -> optional(
          text(minLength = 3, maxLength = 128)
            .transform [String] ( strTrimSanitizeF, identity )
        )
        .transform [Option[String]] ( emptyStrOptToNone, identity )
    )
    { NodeCreateParams.apply }
    { NodeCreateParams.unapply }
  }

  def nodeCreateParamsFormM = Form(nodeCreateParamsM)


  def _setMapping[T](strMapping: Mapping[String])(f: Seq[String] => TraversableOnce[T]): Mapping[Set[T]] = {
    optional(
      strMapping
        .transform(strTrimSanitizeF, strIdentityF)
    )
      .transform[Option[String]] ( emptyStrOptToNone, identity )
      .transform[Set[T]] (
        {case None    => Set.empty
         case Some(s) => f(s.split("\\s*[,;]\\s*")).toSet
        },
        { ids =>
          if (ids.isEmpty) None else Some( ids.mkString(", ") )
        }
      )
  }

  /** Маппинг для списка id узлов (эджа). */
  def nodeIdsListM: Mapping[Set[String]] = {
    _setMapping[String] (text(maxLength = 512)) (identity)
  }

  def itemIdsListM: Mapping[Set[Long]] = {
    _setMapping[Long] (text(minLength = 1, maxLength = 128)) (_.iterator.map(_.toLong))
  }

  def tagsListM: Mapping[Set[String]] = {
    _setMapping[String] (text(maxLength = 512)) (identity)
  }

  /** Маппинг для поля info в эдже. */
  def edgeInfoM: Mapping[MEdgeInfo] = {
    mapping(
      "commentNi"  -> optional( text(maxLength = 256) ),
      "flag"       -> optional( boolean ),
      "tags"       -> tagsListM
    )
    { (commentNiOpt, flagOpt, tags) =>
      MEdgeInfo(
        commentNi   = commentNiOpt,
        flag        = flagOpt,
        tags        = tags
      )
    }
    { ei =>
      Some((ei.commentNi, ei.flag, ei.tags))
    }
  }

  /** Маппинг для эджа. */
  def edgeM: Mapping[MEdge] = {
    mapping(
      "predicate" -> FormUtil.predicateM,
      "nodeIds"   -> nodeIdsListM,
      "order"     -> optional( number ),
      "info"      -> edgeInfoM
    )
    { (predicate, nodeIds, orderOpt, eInfo) =>
      MEdge(
        predicate = predicate,
        nodeIds   = nodeIds,
        order     = orderOpt,
        info      = eInfo
      )
    }
    {e =>
      Some((e.predicate, e.nodeIds, e.order, e.info))
    }
  }

  def edgeFormM = Form(edgeM)


  /** Накатить результат формы edgeFormM на существующий эдж. */
  def updateEdge(mEdge0: MEdge, e: MEdge): MEdge = {
    mEdge0.copy(
      predicate = e.predicate,
      nodeIds   = e.nodeIds,
      order     = e.order,
      info      = {
        val i = e.info
        mEdge0.info.copy(
          commentNi   = i.commentNi,
          flag        = i.flag,
          tags        = i.tags
        )
      }
    )
  }

}


/** Интерфейс для DI-поля с инстансом [[SysMarketUtil]]. */
trait ISysMarketUtilDi {
  def sysMarketUtil: SysMarketUtil
}
