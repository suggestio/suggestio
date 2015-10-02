package util.compat.img3

import io.suggest.model.n2.media.storage.CassandraStorage
import io.suggest.model.n2.media.{MPictureMeta, MFileMeta}
import io.suggest.util.JMXBase
import models.im.{ImgFileUtil, MImg}
import models.mfs.FileUtil
import models.{MAdnNode, MNode, MNodeCommon, MNodeTypes, MNodeMeta, MMedia}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.PlayLazyMacroLogsImpl
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import util.img.DynImgUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.15 17:47
 * Description: Система обновления картинок на новую архитектуру: N2, seaweedfs.
 */
object Migration extends PlayLazyMacroLogsImpl {

  /** Пройтись по узлам ADN, логотипы сохранить через MMedia, создать необходимые MEdge. */
  def adnLogosToN2(): Future[LogosAcc] = {
    import LOGGER._

    // Обойти все узлы ADN, прочитав оттуда данные по логотипам.
    MAdnNode.foldLeftAsync( LogosAcc(0, 0) ) { (acc0Fut, madnNode) =>
      val adnNodeId = madnNode.id.get
      val logPrefix = s"[$adnNodeId]"
      trace(s"$logPrefix Processing ADN node: ${madnNode.meta.name} / ${madnNode.meta.town}")
      madnNode.logoImgOpt match {
        case Some(logoImg) =>
          val mimg = MImg(logoImg).original
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
              meta = MNodeMeta(
                nameOpt     = Some("logo-" + madnNode.id.getOrElse("") + "." + fext),
                dateCreated = perm.map(_.dateCreated)
                  .getOrElse { madnNode.meta.dateCreated }
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

          // Создать own-эдж владения этой картинкой от узла к картинке.
          /*val ownEdge = MEdge(
            fromId    = adnNodeId,
            predicate = MPredicates.Owns,
            toId      = imgNodeId
          )*/
          val ownEdgeSaveFut: Future[String] = ??? ///ownEdge.save

          ownEdgeSaveFut onComplete {
            case Success(oeId) =>
              trace(s"$logPrefix saved OWN edge as [$oeId]")
            case Failure(ex) =>
              error(s"$logPrefix failed to save OWN edge", ex)
          }

          val mediaId = MMedia.mkId(imgNodeId, None)

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
                  .orElse { logoImg.meta.map(_.width) }
                  .get,
                height = imgWh.map(_.height)
                  .orElse {  logoImg.meta.map(_.height) }
                  .get
              )),
              id = Some( mediaId )
            )
          }

          val mmediaSaveFut = mmedia.flatMap(_.save)

          mmediaSaveFut onComplete {
            case Success(mmId) =>
              trace(s"$logPrefix saved MMedia [$mmId]")
            case Failure(ex) =>
              error(s"$logPrefix failed to save MMedia", ex)
          }

          // Создать logo-эдж на node картинки.
          /*val logoEdge = MEdge(
            fromId    = adnNodeId,
            predicate = MPredicates.Logo,
            toId      = imgNodeId
          )*/

          val logoEdgeSaveFut: Future[String] = ??? //logoEdge.save

          logoEdgeSaveFut onComplete {
            case Success(leId) =>
              trace(s"$logPrefix Saved logo edge as [$leId]")
            case Failure(ex) =>
              error(s"$logPrefix Failed to save LOGO edge", ex)
          }

          for {
            _     <- mnodeSaveFut
            _     <- ownEdgeSaveFut
            _     <- mmediaSaveFut
            _     <- logoEdgeSaveFut
            acc0  <- acc0Fut
          } yield {
            info(s"$logPrefix ADN Node processing finished for logo [$imgNodeId]")
            acc0.copy(
              nodesDone = acc0.nodesDone + 1,
              logosDone = acc0.logosDone + 1
            )
          }

        // Нет логотипа у этого узла.
        case None =>
          trace(s"$logPrefix Skipping node: no logo")
          acc0Fut.map { acc0 =>
            acc0.copy(
              nodesDone = acc0.nodesDone + 1
            )
          }
      }
    }
  }

  /** Аккамулятор результатов при обходе узлов для портирования логотипов. */
  case class LogosAcc(
    nodesDone: Int,
    logosDone: Int
  )

}


trait MigrationJmxMBean {
  def adnLogosToN2(): String
}

class MigrationJmx extends JMXBase with MigrationJmxMBean {

  override def jmxName: String = "io.suggest.compat:type=img3,name=" + Migration.getClass.getSimpleName

  override def adnLogosToN2(): String = {
    val strFut = Migration.adnLogosToN2()
      .map { acc =>
        s"ADN Nodes processed: ${acc.nodesDone};  logos processed: ${acc.logosDone}"
      }
    awaitString(strFut)
  }

}
