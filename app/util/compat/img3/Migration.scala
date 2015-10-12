package util.compat.img3

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.edge.{MNodeEdges, MEdgeInfo}
import io.suggest.model.n2.media.storage.CassandraStorage
import io.suggest.model.n2.media.{MMedia_, MPictureMeta, MFileMeta}
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.util.JMXBase
import models.im.{ImgFileUtil, MImg}
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
  mMedia                : MMedia_,
  implicit val ec       : ExecutionContext,
  implicit val esClient : Client,
  implicit val sn       : SioNotifierStaticClientI
) extends PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Пройтись по узлам ADN, логотипы сохранить через MMedia, создать необходимые MEdge. */
  def adnNodesToN2(): Future[LogosAcc] = {

    // Обойти все узлы ADN, прочитав оттуда данные по логотипам.
    MAdnNode.foldLeftAsync( LogosAcc() ) { (acc0Fut, madnNode) =>

      val adnNodeId = madnNode.id.get
      val logPrefix = s"[$adnNodeId]"
      trace(s"$logPrefix Processing ADN node: ${madnNode.meta.name} / ${madnNode.meta.town}")

      // Запуск обработки логотипа узла.
      val logoEdgeOptFut = madnNode.logoImgOpt.fold {
        Future successful Option.empty[MEdge]
      } { logoImg =>
        val mimg = MImg(logoImg).original
        portOneImage(mimg, adnNodeId, madnNode.meta.dateCreated, logoImg.meta)
          .map { imgNodeId =>
            Some( MEdge(MPredicates.Logo, imgNodeId) )
          }
      }

      val galEdgesFut = Future.traverse( madnNode.gallery.zipWithIndex ) {
        case (galImgFileName, i) =>
          val mimg = MImg( galImgFileName )
          portOneImage(mimg, adnNodeId, madnNode.meta.dateCreated)
            .map { imgNodeId =>
              MEdge( MPredicates.GalleryItem, imgNodeId, order = Some(i), info = MEdgeInfo(
                dynImgArgs = mimg.qOpt
              ))
            }
      }

      val mnode0 = madnNode.toMNode

      val mnode1Fut = for {
        logoEdgeOpt <- logoEdgeOptFut
        galEdges    <- galEdgesFut
      } yield {
        mnode0.copy(
          edges = mnode0.edges.copy(
            out = mnode0.edges.out ++ MNodeEdges.edgesToMapIter(
              logoEdgeOpt.iterator ++ galEdges.iterator
            )
          )
        )
      }

      val mnode1SaveFut = mnode1Fut
        .flatMap(_.save)

      for {
        logosCount    <- logoEdgeOptFut.map(_.size)
        galImgsCount  <- galEdgesFut.map(_.size)
        mnode1id      <- mnode1SaveFut
        acc0          <- acc0Fut
      } yield {
        acc0.copy(
          adnNodes = acc0.adnNodes  + 1,
          logos    = acc0.logos     + logosCount,
          galImgs  = acc0.galImgs   + galImgsCount
        )
      }
    }
  }

  /** Аккамулятор результатов при обходе узлов для портирования логотипов. */
  case class LogosAcc(
    adnNodes  : Int = 0,
    logos     : Int = 0,
    galImgs   : Int = 0
  ) {

    def toReport: String = {
      s"ADN Nodes: $adnNodes;\nLogos: $logos;\nGal imgs: $galImgs"
    }
  }


  /** Портирование одного оригинала картинки. */
  def portOneImage(mimg: MImg, ownNodeId: String, dateCreatedDflt: DateTime, imetaOpt: Option[ISize2di] = None): Future[String] = {

    lazy val logPrefix = s"portOneImage(${mimg.rowKeyStr}):"

    trace(s"$logPrefix Starting: ownNodeId=$ownNodeId")

    val mLocImgFut = DynImgUtil.ensureImgReady(mimg, cacheResult = false)

    // Собираем необходимые данные для картинки.
    // Узнать хеш-сумму файла
    val sha1Fut = for (mLocImg <- mLocImgFut) yield {
      FileUtil.sha1(mLocImg.file)
    }

    val mmFut = for (mLocImg <- mLocImgFut) yield {
      FileUtil.getMimeMatch( mLocImg.file )
    }

    // Узнать MIME файла
    val mimeFut = for (mm <- mmFut) yield {
      val mimeOpt = ImgFileUtil.getMime( mm.get )
      ImgFileUtil.orUnknown(mimeOpt)
    }

    // Узнать байтовый размер файла.
    val sizeBFut = for (mLocImg <- mLocImgFut) yield {
      mLocImg.file.length()
    }

    // Узнать file extension
    val fextFut = mmFut map { mmOpt =>
      mmOpt.fold {
        LOGGER.warn("Logo extension is unknown, guessing PNG")
        "png"
      } { _.getExtension }
    }

    // Заново определяем image width/height для надежности, НЕ используем logoImg.meta
    val imgWhFut = mLocImgFut.flatMap {
      _.getImageWH
    }

    val imgNodeId = mimg.rowKeyStr
    trace(s"$logPrefix Processing logo img: $mimg")

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

    val mediaId = mMedia.mkId(imgNodeId, None)

    // Сохранить в MMedia накопившуюся инфу по картинке.
    val mmedia = for {
      mime  <- mimeFut
      sizeB <- sizeBFut
      sha1  <- sha1Fut
      imgWh <- imgWhFut
    } yield {
      MMedia(
        nodeId = imgNodeId,
        file = MFileMeta(
          mime        = mime,
          sizeB       = sizeB,
          isOriginal  = true,   // Копируются только оригиналы, деривативы должны быть сгенерены на ходу.
          sha1        = Some(sha1)
        ),
        storage = CassandraStorage(
          rowKey  = mimg.rowKey,
          qOpt    = None           // Оригиналы у нас без qualifier, точнее там статический qualifier подставляется вместо None в MImg.
        ),
        picture = Some(MPictureMeta(
          width  = imgWh.map(_.width)
            .orElse { imetaOpt.map(_.width) }
            .get,
          height = imgWh.map(_.height)
            .orElse { imetaOpt.map(_.height) }
            .get
        )),
        id = Some( mediaId ),
        companion = mMedia
      )
    }

    val mmediaSaveFut = mmedia.flatMap(_.save)

    mmediaSaveFut onComplete {
      case Success(mmId) =>
        trace(s"$logPrefix saved MMedia [$mmId]")
      case Failure(ex) =>
        error(s"$logPrefix failed to save MMedia", ex)
    }

    for {
      _     <- mnodeSaveFut
      _     <- mmediaSaveFut
    } yield {
      info(s"$logPrefix Logo done: [$imgNodeId]")
      imgNodeId
    }
  }

}


trait MigrationJmxMBean {
  def adnNodesToN2(): String
}

class MigrationJmx @Inject() (migration: Migration, implicit val ec: ExecutionContext)
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
