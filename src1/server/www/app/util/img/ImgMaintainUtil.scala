package util.img

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import io.suggest.es.model.{EsModel, IMust, MEsNestedSearch}
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.media.storage.swfs.SwfsVolumeCache
import io.suggest.n2.media.storage.{MStorage, MStorageInfo, MStorageInfoData, MStorages}
import io.suggest.n2.media.{MEdgeMedia, MPictureMeta}
import io.suggest.n2.node.{MNode, MNodeFields, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.swfs.fid.SwfsVolumeId_t
import io.suggest.util.JmxBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import javax.inject.Inject
import models.im.{MImg3, MLocalImgs}
import play.api.inject.Injector
import util.up.UploadUtil
import util.up.ctx.IImgUploadCtxFactory
import japgolly.univeq._
import monocle.Traversal
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scalaz.std.option._

import scala.jdk.CollectionConverters._
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
  private[img] lazy val swfsVolumeCache = injector.instanceOf[SwfsVolumeCache]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  implicit private lazy val mat = injector.instanceOf[Materializer]

  def MY_HOST = uploadUtil.MY_NODE_PUBLIC_HOST


  /** Нужно для поиска картинок на текущем узле использовать swfs volume ids. */
  def getSwfsVolumeIds(query: Option[MNodeSearch] = None ): Future[Map[SwfsVolumeId_t, Long]] = {
    import esModel.api._
    lazy val logPrefix = s"getAllSwfsVolumeIds()#${System.currentTimeMillis()}:"
    LOGGER.debug(s"$logPrefix Started, query = ${query.map(_.toEsQuery.toString)}")

    // Используем nested aggregation для сбора всех volume ids.
    val EDGES_AGG = "edges"
    val SHARDS_AGG = "shards"

    for {
      rawRes <- query
        .fold( mNodes.prepareSearch() )( mNodes.prepareSearch1 )
        .setSize(0)
        .addAggregation {
          AggregationBuilders
            .nested( EDGES_AGG, MNodeFields.Edges.E_OUT_FN )
            .subAggregation {
              AggregationBuilders
                .terms( SHARDS_AGG )
                .field( MNodeFields.Edges.EO_MEDIA_STORAGE_DATA_SHARDS_FN )
            }
        }
        .executeFut()
    } yield {
      val res = rawRes
        .getAggregations
        .get[Nested]( EDGES_AGG )
        .getAggregations
        .get[Terms]( SHARDS_AGG )
        .getBuckets
        .iterator()
        .asScala
        .map { bucket =>
          // Ошибки внутри bucket не логгируем, т.к. show_terms_doc_count_error is false
          LOGGER.trace(s"$logPrefix Agg: ${bucket.getKey} -> ${bucket.getDocCount}")

          val k: SwfsVolumeId_t = bucket
            .getKey
            .toString
            .toInt

          val v = bucket.getDocCount

          k -> v
        }
        .toMap

      if (res.nonEmpty) {
        LOGGER.debug(s"$logPrefix Found ${res.size} volumes [${res.keysIterator.min}..${res.keysIterator.max}] of total ${res.valuesIterator.sum} file-edges")
      } else {
        LOGGER.warn(s"$logPrefix Found NO file-edges")
      }

      res
    }
  }


  /** Оставить только volume id, относящиеся к указанному хостнейму.
    *
    * @param allVolumesIds Нефильтрованные volume ids.
    *                      Выхлоп getSwfsVolumeIds().keys, например.
    * @param myHost Имя хостнейма, для которого собираются volume ids.
    * @return Фьючерс с отфильтрованным множеством volume ids под текущий хост.
    */
  def onlyForNodeSwfsVolumeIds(allVolumesIds: Set[SwfsVolumeId_t],
                               myHost: String = MY_HOST): Future[Set[SwfsVolumeId_t]] = {
    lazy val logPrefix = s"onlyMyNodeSwfsVolumeIds(${allVolumesIds.size}vols, $myHost)#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix Starting with ${allVolumesIds.toSeq.sorted} for node $myHost")

    (Future.traverse( allVolumesIds: Iterable[SwfsVolumeId_t] ) { volumeId =>
      for (locs <- swfsVolumeCache.getLocations( volumeId )) yield {
        if (locs.exists { volLoc =>
          (volLoc.url equalsIgnoreCase myHost) ||
            (volLoc.publicUrl equalsIgnoreCase myHost)
        }) {
          LOGGER.trace(s"$logPrefix Found swfs volume#$volumeId - related to my host#$myHost")
          volumeId :: Nil
        } else {
          LOGGER.trace(s"$logPrefix NOT relevant swfs volume#$volumeId skipped, [${locs.mkString(", ")}] not contains host#$myHost")
          Nil
        }
      }
    })
      .map( _.iterator.flatten.toSet )
  }


  /** Найти картинки и их пересчитать рамеры из файлов, сохранив изменившиеся размеры в базу.
    * Поиск только на текущем media-узле.
    *
    * @param onlyWithWhValues Некорректные размеры.
    *                         Например, ищем записанный 0 в базе, тогда List(0).
    *                         Можно задавать от 0 до 10 через List(0, 10)
    * @param nodeIds id узлов-картинок, которые надо обновлять.
    * @return Кол-во прочитанных и записанных узлов.
    */
  def refreshNodeSavedWhs(onlyWithWhValues: Iterable[Int] = Nil,
                          nodeIds: Seq[String] = Nil): Future[(Int, Int)] = {

    import esModel.api._
    import mNodes.Implicits._

    val logPrefix = s"refreshNodeSavedWhs()#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix onlyWH=[${onlyWithWhValues.mkString("|")}] nodeIds=[${nodeIds.mkString(", ")}]")

    val someTrue = Some(true)
    val preds = MPredicates.Blob.File :: Nil
    val should = IMust.SHOULD
    val onlyWithWhValuesList = onlyWithWhValues.toList
    val _nodeTypes = MNodeTypes.Media #:: MNodeTypes.Media.children

    def __mkSearch(crsAcc0: List[Criteria] = Nil): MNodeSearch = {
      new MNodeSearch {
        override def withIds = nodeIds
        override val nodeTypes = _nodeTypes
        override val outEdges: MEsNestedSearch[Criteria] = {
          MEsNestedSearch( crsAcc0 )
        }
      }
    }

    // Чтобы найти volume, относящиеся к узлу, надо собрать все необходимые volumeIds.
    for {
      allVolumesIdsInfo <- getSwfsVolumeIds(
        query = Some( __mkSearch() ),
      )

      allVolumesIds = {
        val r = allVolumesIdsInfo.keySet
        LOGGER.debug(s"$logPrefix Found ${r.size} SWFS volume ids in file-edges.")
        r
      }

      // Надо собрать только volume_ids, относящиеся к текущей ноде:
      myNodeVolumeIds <- onlyForNodeSwfsVolumeIds( allVolumesIds )

      countRead = new AtomicInteger( 0 )
      countWrite = new AtomicInteger( 0 )

      mnode_edges_out_LENS = MNode.edges
        .composeLens( MNodeEdges.out )

      edge_media_picture_wh_LENS = MEdge.media
        .composeTraversal( Traversal.fromTraverse[Option, MEdgeMedia] )
        .composeLens( MEdgeMedia.picture )
        .composeLens( MPictureMeta.whPx )

      // Запуск массового обновления.
      _ <- mNodes
        .source[MNode](
          searchQuery = {
            LOGGER.info(s"$logPrefix My node is ${uploadUtil.MY_NODE_PUBLIC_HOST}, my storage have ${myNodeVolumeIds.size} shards with ${allVolumesIdsInfo.view.filterKeys(myNodeVolumeIds.contains).values.sum} files.\n Shard ids are: [${myNodeVolumeIds.toSeq.sorted.mkString(" ")}] ")
            var crsAcc0 = List.empty[Criteria]
            if (myNodeVolumeIds.nonEmpty) {
              val fileStorTypes = Set.empty[MStorage] + MStorages.SeaWeedFs
              val fStorShards = Some( myNodeVolumeIds.map(_.toString) )
              crsAcc0 ::= Criteria(
                predicates      = preds,
                must            = should,
                pictureWidthPx  = onlyWithWhValuesList,
                fileIsOriginal  = someTrue,
                fileStorType    = fileStorTypes,
                fileStorShards  = fStorShards,
              )

              if (onlyWithWhValuesList.nonEmpty)
                crsAcc0 ::= Criteria(
                  predicates      = preds,
                  must            = should,
                  pictureHeightPx = onlyWithWhValuesList,
                  fileIsOriginal  = someTrue,
                  fileStorType    = fileStorTypes,
                  fileStorShards  = fStorShards,
                )
            }
            __mkSearch( crsAcc0 ).toEsQuery
          },
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

    } yield {
      val r = (countRead.get(), countWrite.get())
      LOGGER.trace(s"$logPrefix Done. Read/written => $r")
      r
    }
  }


  /** Поле MStorageInfoData().hosts исторически было пустым. Однако, туда надо вписать volumeId seaweedfs-файлов.
    *
    *
    * @param onlyShardsMissing true - обработать только элементы, где поле shards пустое.
    *                          false - обработать все узлы, перезаписать существующие значения поля shards.
    * @param nodeIds id узлов для обработки.
    *                Seq.empty означает все подходящие узлы.
    * @return
    */
  def resetSwfsStorageVolumeIds( onlyShardsMissing: Boolean, nodeIds: String* ): Future[Int] = {
    import esModel.api._
    import mNodes.Implicits._

    val logPrefix = "refillSwfsStorageVolumeIds():"
    LOGGER.info(s"$logPrefix Starting")

    val filePred = MPredicates.Blob.File

    val countProcessed = new AtomicInteger( 0 )
    val node_edges_out_LENS = MNode.edges
      .composeLens( MNodeEdges.out )

    val edge_media_storage_data_LENS = MEdge.media
      .composeTraversal( Traversal.fromTraverse[Option, MEdgeMedia] )
      .composeLens( MEdgeMedia.storage )
      .composeTraversal( Traversal.fromTraverse[Option, MStorageInfo] )
      .composeLens( MStorageInfo.data )

    for {
      _ <- mNodes
        .source[MNode](
          searchQuery = (new MNodeSearch {
            override def withIds = nodeIds
            override val nodeTypes = MNodeTypes.Media #:: MNodeTypes.Media.children
            override val outEdges: MEsNestedSearch[Criteria] = {
              // nodeIds должен содержать id текущего-сервера узла sio, на котором и размещены файлы.
              var crsAcc = Criteria(
                predicates      = filePred :: Nil,
                fileStorType    = Set.empty + MStorages.SeaWeedFs,
                fileStorShards  = Option.when( onlyShardsMissing )( Set.empty ),
              ) :: Nil
              MEsNestedSearch( crsAcc )
            }
          }).toEsQuery,
        )
        .mapAsyncUnordered(4) { mnode =>
          countProcessed.incrementAndGet()
          mNodes.tryUpdate(mnode) {
            node_edges_out_LENS.modify { edges0 =>
              for (e <- edges0) yield {
                if (e.predicate ==* filePred) {
                  edge_media_storage_data_LENS.modify { mStorInfoData =>
                    mStorInfoData.swfsFid.fold {
                      LOGGER.warn(s"$logPrefix Missing SwfsFid for node#${mnode.idOrNull} store=$mStorInfoData\n file-edge = $e")
                      mStorInfoData
                    } { swfsFid =>
                      LOGGER.trace(s"$logPrefix Updating shards on node#${mnode.idOrNull} from [${mStorInfoData.shards.mkString(" ")}] to => ${swfsFid.volumeId} (fid#$swfsFid)")
                      MStorageInfoData.shards
                        .set( Set.empty + swfsFid.volumeId.toString )(mStorInfoData)
                    }
                  }(e)
                } else e
              }
            }
          }
        }
        .run()
    } yield {
      val totalCount = countProcessed.get()
      LOGGER.info(s"$logPrefix Updated $totalCount nodes.")
      totalCount
    }
  }

}


/** Интерфейс для JMX. */
sealed trait ImgMaintainUtilJmxMBean {

  def getSwfsVolumeIds(): String

  def getHostSwfsVolumeIds(hostName: String): String

  def getSwfsVolumeIdsForNodes(nodeIds: String): String

  def refreshNodeSavedWhs( onlyWithWhValues: String,
                           nodeIds: String,
                         ): String

  def resetSwfsStorageVolumeIds(onlyShardsMissing: Boolean,
                                nodeIds: String,
                                ): String

  def swfsVolumesDisplay(volumeIds: String): String

}

final class ImgMaintainUtilJmx @Inject() (injector: Injector)
  extends JmxBase
  with ImgMaintainUtilJmxMBean
  with MacroLogsDyn
{

  private def imgMaintainUtil = injector.instanceOf[ImgMaintainUtil]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  override def _jmxType = "img"


  override def getSwfsVolumeIds(): String = {
    val strFut = for {
      buckets <- imgMaintainUtil.getSwfsVolumeIds()
    } yield {
      buckets.mkString("\n#v | count\n-------\n", ", \n", "")
    }
    JmxBase.awaitString( strFut )
  }


  override def getHostSwfsVolumeIds(hostName: String): String = {
    val _imgMaintainUtil = imgMaintainUtil
    val strFut = for {
      allBuckets <- _imgMaintainUtil.getSwfsVolumeIds()
      filteredVolumeIds <- _imgMaintainUtil.onlyForNodeSwfsVolumeIds(
        allBuckets.keySet,
        myHost = if (hostName.nonEmpty) hostName else _imgMaintainUtil.MY_HOST,
      )
    } yield {
      allBuckets
        .view
        .filterKeys( filteredVolumeIds.contains )
        .mkString( "\n#v | count\n-------\n", ", \n", "" )
    }

    JmxBase.awaitString( strFut )
  }


  override def getSwfsVolumeIdsForNodes(nodeIdsRaw: String): String = {
    val delimRE = "[, ]+".r
    val nodeIds = Option.when( nodeIdsRaw.nonEmpty ) {
      delimRE
        .split( nodeIdsRaw )
        .toSeq
    }
      .getOrElse( Nil )

    val strFut = for {
      buckets <- imgMaintainUtil.getSwfsVolumeIds(
        query = Some {
          new MNodeSearch {
            override def withIds = nodeIds
          }
        }
      )
    } yield {
      buckets.mkString(", \n")
    }

    JmxBase.awaitString( strFut )
  }


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


  override def resetSwfsStorageVolumeIds(onlyShardsMissing: Boolean, nodeIdsRaw: String ): String = {
    LOGGER.warn(s"resetSwfsStorageVolumeIds( onlyMissing?$onlyShardsMissing, nodeIds = [$nodeIdsRaw])")

    val delimRE = "[, ]+".r
    val nodeIds = Option.when( nodeIdsRaw.nonEmpty ) {
      delimRE
        .split( nodeIdsRaw )
        .toSeq
    }
      .getOrElse( Nil )

    val strFut = for {
      res <- imgMaintainUtil.resetSwfsStorageVolumeIds( onlyShardsMissing, nodeIds: _* )
    } yield {
      s"Done $res nodes."
    }
    JmxBase.awaitString( strFut )
  }


  override def swfsVolumesDisplay(volumeIdsRaw: String): String = {
    val _imgMaintainUtil = imgMaintainUtil

    val strFut = for {

      volumeIds <- Option
        .when( volumeIdsRaw.nonEmpty ) {
          "[, ]+".r
            .split( volumeIdsRaw )
            .iterator
            .map(_.toInt: SwfsVolumeId_t)
            .toSet
        }
        .fold [Future[Set[SwfsVolumeId_t]]] {
          _imgMaintainUtil
            .getSwfsVolumeIds()
            .map(_.keySet)
        }( Future.successful )

      locsStrs <- Future.traverse( volumeIds: Iterable[SwfsVolumeId_t] ) { volumeId =>
        for {
          locs <- _imgMaintainUtil.swfsVolumeCache.getLocations(volumeId)
        } yield {
          s"$volumeId -> ${locs.mkString(" | ")}"
        }
      }

    } yield {
      locsStrs
        .mkString("\n", "\n", "")
    }

    JmxBase.awaitString( strFut )
  }

}
