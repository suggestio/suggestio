package util.img

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import io.suggest.es.model.{EsModel, IMust, MEsNestedSearch}
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.media.{MEdgeMedia, MPictureMeta}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.JmxBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import javax.inject.Inject
import models.im.{MImg3, MLocalImgs}
import play.api.inject.Injector
import util.up.UploadUtil
import util.up.ctx.IImgUploadCtxFactory
import japgolly.univeq._
import monocle.Traversal
import scalaz.std.option._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.11.2020 13:09
  * Description: Утиль для поддержания коллекции картинок в корректном виде.
  */
final class ImgMaintainUtil @Inject()(
                                       injector: Injector,
                                     )
  extends MacroLogsImpl
{

  private lazy val uploadUtil = injector.instanceOf[UploadUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mLocalImgs = injector.instanceOf[MLocalImgs]
  private lazy val imgUploadCtxFactory = injector.instanceOf[IImgUploadCtxFactory]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  implicit private lazy val mat = injector.instanceOf[Materializer]


  /** Найти картинки и их пересчитать рамеры из файлов, сохранив изменившиеся размеры в базу.
    * Поиск только на текущем media-узле.
    *
    * @param onlyWithWhValues Некорректные размеры. Например, ищем записанный 0 в базе, тогда List(0).
    *                         Можно задавать от 0 до 10 черзе List(0)
    * @param nodeIds id узлов-картинок, которые надо обновлять.
    * @return Кол-во прочитанных и записанных узлов.
    */
  def refreshNodeSavedWhs(onlyWithWhValues: Iterable[Int] = Nil,
                          nodeIds: Seq[String] = Nil): Future[(Int, Int)] = {

    import esModel.api._
    import mNodes.Implicits._

    val logPrefix = s"refreshNodeSavedWhs()#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix onlyWH=[${onlyWithWhValues.mkString("|")}] nodeIds=[${nodeIds.mkString(", ")}]")

    val countRead = new AtomicInteger( 0 )
    val countWrite = new AtomicInteger( 0 )

    lazy val mnode_edges_out_LENS = MNode.edges
      .composeLens( MNodeEdges.out )

    lazy val edge_media_picture_wh_LENS = MEdge.media
      .composeTraversal( Traversal.fromTraverse[Option, MEdgeMedia] )
      .composeLens( MEdgeMedia.picture )
      .composeLens( MPictureMeta.whPx )

    mNodes
      .source[MNode](
        searchQuery = (new MNodeSearch {
          override def withIds = nodeIds
          override val nodeTypes = MNodeTypes.Media.Image :: Nil
          override val outEdges: MEsNestedSearch[Criteria] = {
            // nodeIds должен содержать id текущего-сервера узла sio, на котором и размещены файлы.
            val nodeIds = uploadUtil.MY_NODE_PUBLIC_HOST :: Nil
            val preds = MPredicates.Blob.File :: Nil
            val should = IMust.SHOULD
            val onlyWithWhValuesList = onlyWithWhValues.toList
            val someTrue = Some(true)
            var crsAcc = Criteria( nodeIds, preds, must = should, pictureWidthPx = onlyWithWhValuesList, fileIsOriginal = someTrue ) :: Nil
            if (onlyWithWhValuesList.nonEmpty)
              crsAcc ::= Criteria( nodeIds, preds, must = should, pictureHeightPx = onlyWithWhValuesList, fileIsOriginal = someTrue )
            MEsNestedSearch( crsAcc )
          }
        })
          .toEsQuery,
      )
      .mapAsyncUnordered(4) { mnode =>
        val _read = countRead.incrementAndGet()
        val nodeId = mnode.id.get
        LOGGER.trace(s"$logPrefix (${_read}) Found node#$nodeId")

        (for {
          fileEdge <- mnode.edges
            .withPredicateIter( MPredicates.Blob.File )
            .nextOption()
          edgeMedia <- fileEdge.media
          wh0 <- edgeMedia.picture.whPx
          mimg <- MImg3.fromEdge( nodeId, fileEdge )
        } yield {
          // Запустить выкачивание файла в MLocalImg, чтобы file оказался на руках.
          (for {
            localImg <- dynImgUtil.ensureLocalImgReady( mimg, cacheResult = false )
            localFile = mLocalImgs.fileOf( localImg )
            uploadCtx = {
              LOGGER.trace(s"$logPrefix Localized img\n mimg = $mimg\n local file => $localFile")
              imgUploadCtxFactory.make( localFile.toPath )
            }
            wh2Opt = uploadCtx.imageWh
            if wh2Opt.fold {
              LOGGER.error(s"$logPrefix Node#$nodeId WH not detected")
              false
            } { wh2 =>
              val r = wh2 !=* wh0
              if (!r) LOGGER.warn(s"$logPrefix Node#$nodeId WH not changed: $wh2")
              r
            }
            // Изменился размер картинки по сравнению с текущим, сохранённым в узле.
            // Необходимо обновить file-эдж новым размером.
            _ <- mNodes.tryUpdate(mnode) {
              mnode_edges_out_LENS
                .modify { edges0 =>
                  for (e0 <- edges0) yield {
                    (for {
                      media <- e0.media
                      if (e0.predicate ==>> MPredicates.Blob.File) &&
                         media.picture.whPx.nonEmpty
                    } yield {
                      val edgeMod = edge_media_picture_wh_LENS set wh2Opt
                      edgeMod( e0 )
                    })
                      .getOrElse( e0 )
                  }
                }
            }
          } yield {
            val i = countWrite.incrementAndGet()
            LOGGER.trace( s"$logPrefix ($i/${_read}) Node#$nodeId updated WH: $wh0 => ${wh2Opt.orNull}" )
            ()
          })
            .recover { case nsee: NoSuchElementException =>
              LOGGER.trace(s"$logPrefix Skip node #$nodeId")
              ()
            }
        })
          .getOrElse {
            LOGGER.warn(s"$logPrefix Node#$nodeId filtered early.")
            Future.successful(())
          }
      }
      .run()
      .map { _ =>
        (countRead.get(), countWrite.get())
      }
  }

}


sealed trait ImgMaintainUtilJmxMBean {

  def refreshNodeSavedWhs( onlyWithWhValues: String,
                           nodeIds: String,
                         ): String

}

final class ImgMaintainUtilJmx @Inject() (injector: Injector)
  extends JmxBase
  with ImgMaintainUtilJmxMBean
  with MacroLogsDyn
{

  private def imgMaintainUtil = injector.instanceOf[ImgMaintainUtil]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  override def _jmxType = "img"

  override def refreshNodeSavedWhs(onlyWithWhValuesRaw: String, nodeIdsRaw: String): String = {
    val delimRE = "[, ]+".r
    val onlyWithWhValues = Option.when( onlyWithWhValuesRaw.nonEmpty ) {
      delimRE
        .split( onlyWithWhValuesRaw )
        .iterator
        .map(_.toInt)
        .toList
    }
      .getOrElse( Nil )

    val nodeIds = Option.when( nodeIdsRaw.nonEmpty ) {
      delimRE
        .split( nodeIdsRaw )
        .toSeq
    }
      .getOrElse( Nil )

    lazy val logPrefix = s"refreshNodeSavedWhs($onlyWithWhValues, $nodeIds)#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix Called.")

    val strFut = for {
      (read, write) <- imgMaintainUtil.refreshNodeSavedWhs( onlyWithWhValues, nodeIds )
    } yield {
      s"Done, $read nodes read, $write nodes written"
    }

    JmxBase.awaitString( strFut )
  }

}
