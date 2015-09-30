package util.compat.img3

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.media.storage.CassandraStorage
import io.suggest.model.n2.media.{MPictureMeta, MFileMeta}
import io.suggest.util.JMXBase
import models.im.{ImgFileUtil, MImg}
import models.mfs.FileUtil
import models.{MAdnNode, MEdge, MNode, MNodeCommon, MNodeTypes, MNodeMeta, MMedia}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import util.img.DynImgUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.15 17:47
 * Description: Система обновления картинок на новую архитектуру: N2, seaweedfs.
 */
object Migration {

  /** Пройтись по узлам ADN, логотипы сохранить через MMedia, создать необходимые MEdge. */
  def adnLogosToN2(): Future[LogosAcc] = {
    // Обойти все узлы ADN, прочитав оттуда данные по логотипам.
    MAdnNode.foldLeftAsync( LogosAcc(0, 0) ) { (acc0Fut, madnNode) =>
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
            mmOpt.fold("png")(_.getExtension)
          }

          // Заново определяем image width/height для надежности, НЕ используем logoImg.meta
          val imgWhFut = mLocImgFut.flatMap {
            _.getImageWH
          }

          val imgNodeId = mimg.rowKeyStr

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

          val mnodeSavedFut = mnodeFut.flatMap(_.save)

          val adnNodeId = madnNode.id.get


          // Создать own-эдж владения этой картинкой от узла к картинке.
          val ownEdge = MEdge(
            fromId    = adnNodeId,
            predicate = MPredicates.Owns,
            toId      = imgNodeId
          )
          val ownEdgeSaveFut = ownEdge.save

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

          // Создать logo-эдж на node картинки.
          val logoEdge = MEdge(
            fromId    = adnNodeId,
            predicate = MPredicates.Logo,
            toId      = imgNodeId
          )

          val logoEdgeSaveFut = logoEdge.save

          for {
            _     <- mnodeSavedFut
            _     <- ownEdgeSaveFut
            _     <- mmediaSaveFut
            _     <- logoEdgeSaveFut
            acc0  <- acc0Fut
          } yield {
            acc0.copy(
              nodesDone = acc0.nodesDone + 1,
              logosDone = acc0.logosDone + 1
            )
          }

        // Нет логотипа у этого узла.
        case None =>
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
