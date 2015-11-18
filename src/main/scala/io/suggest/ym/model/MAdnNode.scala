package io.suggest.ym.model

import io.suggest.model.es._
import io.suggest.model.n2.edge.{MPredicates, MEdge, NodeEdgesMap_t, MNodeEdges}
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.{MNodeTypes, MNode}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.util.SioEsUtil._
import io.suggest.model.common._
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.ym.model.common._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.util.SioEsUtil.FieldAll
import io.suggest.util.SioEsUtil.FieldSource
import io.suggest.util.MacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 19:16
 * Description: Узел рекламной сети. Он приходит на смену всем MMart, MShop и т.д.
 * Модель узла конструируется компилятором из кучи кусков, вроде бы не связанных между собой.
 */
object MAdnNode
  extends EsModelStaticMutAkvEmptyT
  with EMPersonIdsStatic
  with EMAdNetMemberStatic
  with EMLogoImgStatic
  with EMAdnMMetadataStatic
  with EMNodeConfStatic
  with EMImgGalleryStatic
  with EMAdnNodeGeoStatic
  with EsModelStaticMutAkvIgnoreT
  with MacroLogsImpl
  with AdnNodesSearch
{
  override val ES_TYPE_NAME = "adnNode"

  override type T = MAdnNode

  override protected def dummy(id: Option[String], version: Option[Long]) = {
    MAdnNode(
      adn = null,
      id = id,
      versionOpt = version
    )
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )

}


final case class MAdnNode(
  var adn           : AdNetMemberInfo,
  var meta          : MNodeMeta         = MNodeMeta(),
  var personIds     : Set[String]       = Set.empty,
  var logoImgOpt    : Option[MImgInfoT] = None,   // TODO Перенести в conf.logoImg
  var conf          : NodeConf          = NodeConf.DEFAULT,
  var gallery       : List[String]      = Nil,
  var geo           : AdnNodeGeodata    = AdnNodeGeodata.empty,
  var id            : Option[String]    = None,
  versionOpt        : Option[Long]      = None
)
  extends EsModelPlayJsonEmpty
  with EsModelT
  with EMAdNetMember
  with EMPersonIds
  with EMLogoImgMut
  with EMAdnMMetadata
  with EMNodeConfMut
  with EMImgGalleryMut
  with EMAdnNodeGeoMut
{
  override type T = MAdnNode

  @JsonIgnore
  override def companion = MAdnNode

  /** Перед сохранением можно проверять состояние экземпляра (полей экземпляра). */
  @JsonIgnore
  override def isFieldsValid: Boolean = {
    super.isFieldsValid &&
      personIds != null && adn != null && meta != null &&
      meta != null && meta != null
  }


  /** Конвертация экземпляра этой модели в N2 MNode.
    * Карта эджей требует заливки в неё данных по картинкам. */
  def toMNode: MNode = {
    MNode(
      common = MNodeCommon(
        ntype           = MNodeTypes.AdnNode,
        isDependent     = false,
        isEnabled       = adn.isEnabled,
        disableReason   = adn.disableReason
      ),
      meta = meta.toMMeta,
      extras = MNodeExtras(
        adn = Some( adn.toMAdnExtra.copy(
          showInScNl = conf.showInScNodesList
        ))
      ),
      edges = MNodeEdges(
        out = getBasicEdges
      ),
      geo = MNodeGeo(
        point = geo.point
      ),
      id = id
    )
  }

  def getBasicEdges: NodeEdgesMap_t = {
    val edges0 = {
      meta.welcomeAdId
        .iterator
        .map { wcId => MEdge(MPredicates.WcLogo, wcId) } ++
      personIds.iterator
        .map { personId => MEdge(MPredicates.OwnedBy, personId) } ++
      geo.directParentIds
        .iterator
        .map { pi => MEdge(MPredicates.GeoParent.Direct, pi) } ++
      (geo.allParentIds -- geo.directParentIds)
        .iterator
        .map { pi => MEdge(MPredicates.GeoParent, pi) }
    }

    MNodeEdges.edgesToMap1( edges0 )
  }

}


/** JMX MBean интерфейс */
trait MAdnNodeJmxMBean extends EsModelJMXMBeanI

/** JMX MBean реализация. */
final class MAdnNodeJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdnNodeJmxMBean {
  def companion = MAdnNode
}
