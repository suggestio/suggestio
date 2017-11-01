package util.ad

import javax.inject.{Inject, Named, Singleton}

import io.suggest.common.geom.d2.ISize2di
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.{EdgeUid_t, MEdge}
import japgolly.univeq._
import models.im.make.{IMaker, MakeArgs, MakeResult}
import models.im.{AbsCropOp, CompressModes, DevScreen, MImg3}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.17 18:52
  * Description: Утиль для рекламных карточек.
  */
@Singleton
class AdUtil @Inject() (
                         @Named("blk") blkImgMaker   : IMaker,
                         implicit val ec             : ExecutionContext
                       ) {


  def renderAdDocImgs(jdDoc         : Tree[JdTag],
                      imgsEdges     : Iterable[(MEdge, MImg3)],
                      devScreenOpt  : Option[DevScreen]): Future[Map[EdgeUid_t, MakeResult]] = {
    val futsIter = for {
      (medge, mimg) <- imgsEdges.iterator
      edgeUid       <- medge.doc.uid
      jdLoc         <- jdDoc
        .loc
        .find { jdTagTree =>
          jdTagTree
            .getLabel
            .edgeUids
            .exists(_.edgeUid ==* edgeUid)
        }

      jdTag = jdLoc.getLabel
      qdEmbedSzOpt = jdTag.qdProps
        .flatMap(_.attrsEmbed)
        .flatMap[ISize2di](_.size2dOpt)

      contSz2d <- qdEmbedSzOpt.orElse {
        // Не найдено подходящего размера в qd-контенте. Поискать в strip props.
        jdTag.props1.bm
      }

    } yield {
      // Если есть кроп у текущей картинки, то запихнуть его в dynImgOps
      val mimg2 = jdTag.props1.bgImg
        .flatMap(_.crop)
        .fold(mimg) { crop =>
          mimg.withDynOps(
            Seq( AbsCropOp(crop) )
          )
        }

      // Есть картинка и jd-тег, ей соответствующий.
      val imakeResFut = blkImgMaker.icompile(
        MakeArgs(
          img           = mimg2,
          blockMeta     = contSz2d,
          szMult        = 1.0f,
          devScreenOpt  = devScreenOpt,
          compressMode  = Some(
            if (qdEmbedSzOpt.isEmpty) CompressModes.Bg else CompressModes.Fg
          )
        )
      )

      // Дописать в результат инфу по оригинальной картинке
      for (imakeRes <- imakeResFut) yield {
        edgeUid -> imakeRes
      }
    }

    Future
      .sequence(futsIter)
      .map(_.toMap)
  }

}
