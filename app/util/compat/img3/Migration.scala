package util.compat.img3

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.edge.{MNodeEdges, MEdgeInfo}
import io.suggest.model.n2.geo.MGeoShape
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.util.JMXBase
import models.im.{MImg3, MImg3_, MImg}
import models.mfs.FileUtil
import models._
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import util.PlayLazyMacroLogsImpl
import util.img.DynImgUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.15 17:47
 * Description: Система обновления картинок на новую архитектуру: N2, seaweedfs.
 */
class Migration @Inject() (
  mImg3                 : MImg3_,
  implicit val ec       : ExecutionContext,
  implicit val esClient : Client,
  implicit val sn       : SioNotifierStaticClientI
)
  extends PlayLazyMacroLogsImpl
{

  import LOGGER._

  /** Пройтись по узлам ADN, логотипы сохранить через MMedia, создать необходимые MEdge. */
  def adnNodesToN2(): Future[LogosAcc] = {

    // Обойти все узлы ADN, прочитав оттуда данные по логотипам.
    MAdnNode.foldLeftAsync( LogosAcc() ) { (acc0Fut, madnNode) =>

      val adnNodeId = madnNode.id.get
      val logPrefix = s"[$adnNodeId]"
      trace(s"$logPrefix Processing ADN node: ${madnNode.meta.name} / ${madnNode.meta.town}")

      // Запуск обработки логотипа узла.
      val logoEdgeOptFut = FutureUtil.optFut2futOpt(madnNode.logoImgOpt) { logoImg =>
        val mimg = mImg3( MImg(logoImg) ).original
        portOneImage(mimg, adnNodeId, madnNode.meta.dateCreated, logoImg.meta)
          .map { imgNodeId =>
            Some( MEdge(MPredicates.Logo, imgNodeId) )
          }
      }

      val galEdgesFut = Future.traverse( madnNode.gallery.zipWithIndex ) {
        case (galImgFileName, i) =>
          val mimg = mImg3( MImg( galImgFileName ) ).original
          portOneImage(mimg, adnNodeId, madnNode.meta.dateCreated)
            .map { imgNodeId =>
              MEdge( MPredicates.GalleryItem, imgNodeId, order = Some(i), info = MEdgeInfo(
                dynImgArgs = mimg.qOpt
              ))
            }
      }

      // В n2-архитектуре гео-шейпы узла хранятся прямо в узле, т.е. nested doc вместо parent-child doc.
      val nodeGssFut = for {
        nodeGss <- MAdnNodeGeo.findByNode(adnNodeId, maxResults = 50)
      } yield {
        nodeGss
          .iterator
          .zipWithIndex
          .map { case (madnGeo, i) =>
            MGeoShape(
              id          = i,
              glevel      = madnGeo.glevel,
              shape       = madnGeo.shape,
              fromUrl     = madnGeo.url,
              dateEdited  = madnGeo.lastModified
            )
          }
          .toSeq
      }

      val mnode0 = madnNode.toMNode

      val mnode1Fut = for {
        logoEdgeOpt <- logoEdgeOptFut
        galEdges    <- galEdgesFut
        nodeGss     <- nodeGssFut
      } yield {
        mnode0.copy(
          edges = mnode0.edges.copy(
            out = mnode0.edges.out ++ MNodeEdges.edgesToMapIter(
              logoEdgeOpt.iterator ++ galEdges.iterator
            )
          ),
          geo = mnode0.geo.copy(
            shapes = nodeGss
          )
        )
      }

      val mnode1SaveFut = mnode1Fut
        .flatMap(_.save)

      val logosCountFut = logoEdgeOptFut.map(_.size)
      val galImgsCountFut = galEdgesFut.map(_.size)
      val gssCountFut = nodeGssFut.map(_.size)

      for {
        logosCount    <- logosCountFut
        galImgsCount  <- galImgsCountFut
        gssCount      <- gssCountFut
        mnode1id      <- mnode1SaveFut
        acc0          <- acc0Fut
      } yield {
        acc0.copy(
          adnNodes = acc0.adnNodes  + 1,
          logos    = acc0.logos     + logosCount,
          galImgs  = acc0.galImgs   + galImgsCount,
          gss      = acc0.gss       + gssCount
        )
      }
    }
  }

  /** Аккамулятор результатов при обходе узлов для портирования логотипов. */
  case class LogosAcc(
    adnNodes  : Int = 0,
    logos     : Int = 0,
    galImgs   : Int = 0,
    gss       : Int = 0
  ) {

    def toReport: String = {
      s"ADN Nodes: $adnNodes;\nLogos: $logos;\nGal imgs: $galImgs\nGeoShapes: $gss"
    }
  }


  /** Портирование одного оригинала картинки. */
  def portOneImage(mimg: MImg3, ownNodeId: String, dateCreatedDflt: DateTime, imetaOpt: Option[ISize2di] = None): Future[String] = {

    lazy val logPrefix = s"portOneImage(${mimg.rowKeyStr}):"

    trace(s"$logPrefix Starting: ownNodeId=$ownNodeId")

    val mLocImgFut = DynImgUtil.ensureImgReady(mimg, cacheResult = false)

    val mmFut = for (mLocImg <- mLocImgFut) yield {
      FileUtil.getMimeMatch( mLocImg.file )
    }

    // Узнать file extension
    val fextFut = mmFut map { mmOpt =>
      mmOpt.fold {
        LOGGER.warn("Logo extension is unknown, guessing PNG")
        "png"
      } { _.getExtension }
    }

    val imgNodeId = mimg.rowKeyStr
    trace(s"$logPrefix Processing img: $mimg")

    // Создать node для картинки с какими-то минимальными данными.
    val mnodeFut = for {
      perm    <- mimg.permMetaCached
      fext    <- fextFut
    } yield {
      MNode(
        id = Some( imgNodeId ),
        common = MNodeCommon(
          ntype         = MNodeTypes.Media.Image,
          isDependent   = true
        ),
        meta = MMeta(
          basic = MBasicMeta(
            nameOpt     = Some("logo-" + ownNodeId + "." + fext),
            dateCreated = perm.map(_.dateCreated)
              .getOrElse { dateCreatedDflt }
          )
        )
      )
    }

    val mnodeSaveFut = mnodeFut.flatMap(_.save)

    mnodeSaveFut onComplete {
      case Success(lid) =>
        trace(s"$logPrefix created logo MNode as [$lid]")
      case Failure(ex) =>
        error(s"$logPrefix failed to save logo MNode", ex)
    }

    val imgSaveFut = mimg.saveToPermanent

    imgSaveFut onComplete {
      case Success(mmId) =>
        trace(s"$logPrefix saved permanently MImg: " + mimg)
      case Failure(ex) =>
        error(s"$logPrefix failed to save MMedia", ex)
    }

    for {
      _     <- mnodeSaveFut
      _     <- imgSaveFut
    } yield {
      info(s"$logPrefix Logo done: [$imgNodeId]")
      imgNodeId
    }
  }

}


trait MigrationJmxMBean {
  def adnNodesToN2(): String
}

class MigrationJmx @Inject() (
  migration       : Migration,
  implicit val ec : ExecutionContext
)
  extends JMXBase
  with MigrationJmxMBean
{

  override def jmxName: String = "io.suggest.compat:type=img3,name=" + migration.getClass.getSimpleName

  override def adnNodesToN2(): String = {
    val strFut = migration.adnNodesToN2()
      .map { acc =>
        "Total processed\n\n" + acc.toReport
      }
    awaitString(strFut)
  }

}
