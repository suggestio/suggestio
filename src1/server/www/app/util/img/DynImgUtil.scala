package util.img

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.routes
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.img.{MImgFormat, MImgFormats}
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.jd.tags.{JdTag, MJdProps1}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.n2.extra.doc.MNodeDoc
import io.suggest.n2.media.storage.IMediaStorages
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.playx.CacheApiUtil
import io.suggest.url.MHostInfo
import io.suggest.util.JmxBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.im._
import org.im4java.core.{ConvertCmd, IMOperation}
import play.api.mvc.Call
import util.cdn.CdnUtil
import japgolly.univeq._
import monocle.Traversal
import play.api.cache.AsyncCacheApi
import play.api.inject.Injector
import scalaz.EphemeralStream
import scalaz.std.option._

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.concurrent.blocking
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 12:25
 * Description: Утиль для работы с динамическими картинками.
 * В основном -- это прослойка между img-контроллером и моделью orig img и смежными ей.
 */
final class DynImgUtil @Inject() (
                                   injector                  : Injector,
                                 )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mImgs3 = injector.instanceOf[MImgs3]
  private lazy val mLocalImgs = injector.instanceOf[MLocalImgs]
  private lazy val im4jAsyncUtil = injector.instanceOf[Im4jAsyncUtil]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  // Для каких-то регламентных операций в MNodes:
  private lazy val mNodes = injector.instanceOf[MNodes]
  // Только для удаления и чистки базы:
  private lazy val iMediaStorages = injector.instanceOf[IMediaStorages]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  private lazy val cache = injector.instanceOf[AsyncCacheApi]
  implicit private lazy val mat = injector.instanceOf[Materializer]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import esModel.api._

  /** Сколько времени кешировать результат подготовки картинки?
    * Кеш используется для подавления параллельных запросов. */
  private def ENSURE_DYN_CACHE_TTL = 10.seconds

  /** Если true, то производные от оригинала картники будут дублироваться в распределённое хранилище.
    * Если false, то производные будут только в файловом кэше. */
  private def SAVE_DERIVATIVES_TO_PERMANENT = true

  /** Активен ли префетчинг, т.е. опережающая подготовка картинки?
    * Префетчинг начинается асинхронно в момент генерации ссылки на картинку. */
  // TODO Из-за распределения картинок по узлам, возникла ситуация, что prefetch исполняется не на том узле
  // TODO Нужно сделать prefetch для локальных картинок, или слать HTTP Head запрос на ноду, где картинка должна бы быть.
  private def PREFETCH_ENABLED = false


  /**
   * Враппер для вызова routes.Img.dynImg(). Нужен чтобы навешивать сайд-эффекты и трансформировать результат вызова.
   * @param dargs Аргументы генерации картинки.
   * @return Экземпляр Call, пригодный к употреблению.
   */
  def imgCall(dargs: MImgT): Call = {
    if (PREFETCH_ENABLED) {
      Future {
        ensureLocalImgReady(dargs, cacheResult = true)
          .failed
          .foreach { ex =>
            LOGGER.error("Failed to prefetch dyn.image: " + dargs.dynImgId.fileName, ex)
          }
      }
    }
    routes.Img.dynImg(dargs)
  }
  def imgCall(filename: String): Call = {
    val img = MImg3(filename)
    imgCall(img)
  }


  /** Аналог [[imgCall()]], но работающая в контексте dist-cdn. */
  def distCdnImgCall(mimg: MImgT, mediaHostsMap: Map[String, Seq[MHostInfo]]): Call = {
    distCdnImgCall( imgCall(mimg), mimg, mediaHostsMap )
  }
  def distCdnImgCall(call: Call, mimg: MImgT, mediaHostsMap: Map[String, Seq[MHostInfo]]): Call = {
    cdnUtil.forMediaCall1(
      call          = call,
      mediaHostsMap = mediaHostsMap,
      mediaIds      = mimg.dynImgId.mediaIdAndOrigMediaId
    )
  }


  /** Сборка media-hosts map для картинок. */
  def mkMediaHostsMap(imgs: Iterable[MImgT]): Future[Map[String, Seq[MHostInfo]]] = {
    if (imgs.isEmpty) {
      Future.successful( Map.empty )
    } else {
      for {
        // Получить на руки media-инстансы для оригиналов картинок:
        medias <- {
          val nodeIds = imgs
            .iterator
            .flatMap( _.dynImgId.mediaIdAndOrigMediaId )
            .toSet
          mNodes.multiGetCache( nodeIds )
        }

        // Узнать узлы, на которых хранятся связанные картинки.
        mediaHostsMap <- cdnUtil.mediasHosts( medias )

      } yield {
        LOGGER.trace(s"nodeMediaHostsMap: ${mediaHostsMap.valuesIterator.flatten.map(_.namePublic).toSet.mkString(", ")}")
        mediaHostsMap
      }
    }
  }


  /**
   * Найти в базе готовую картинку, ранее уже отработанную, и сохранить её в файл.
   * Затем накатить на неё необходимые параметры, пересжав необходимым образом.
   * @param args Данные запроса картинки. Ожидается, что набор параметров не пустой, и подходящей картинки нет в хранилище.
   * @return Фьючерс с файлом, содержащий результирующую картинку.
   *         Фьючерс с NoSuchElementException, если нет исходной картинки.
   *         Фьючерс с Throwable при иных ошибках.
   */
  def mkReadyImgToFile(args: MImgT): Future[MLocalImg] = {
    val fut = for {
      localImgOpt <- mImgs3.toLocalImg( args.original )
      localImg = localImgOpt.get
      // Есть исходная картинка в файле. Пора пережать её согласно настройкам.
      newLocalImg = args.toLocalInstance
      outFormat = newLocalImg.dynImgId.imgFormat.get
      // Запустить конвертацию исходной картинки
      _ <- convert(
        in     = mLocalImgs.fileOf(localImg),
        out    = mLocalImgs.fileOf(newLocalImg),
        outFmt = outFormat,
        imOps  = args.dynImgId.imgOps
      )
    } yield {
      // Вернуть финальную картинку, т.к. с оригиналом и так всё ясно.
      newLocalImg
    }

    for (ex <- fut.failed) {
      val logPrefix = s"mkReadyImgToFile($args):"
      ex match {
        case _: NoSuchElementException =>
          LOGGER.error(s"$logPrefix Image original does not exists in storage")
        case _ =>
          LOGGER.error(s"$logPrefix Unknown exception during image prefetch", ex)
      }
    }

    fut
  }


  /** Абстракция над ensureLocalImgReady(), когда это не требуется. */
  def getStream(args: MImgT): Source[ByteString, _] = {
    lazy val logPrefix = s"getStream(${args.dynImgId.fileName}):"

    val localInst = args.toLocalInstance
    // Поискать в локальном кэше картинок.
    if ( mLocalImgs.isExists(localInst) ) {
      // TODO А если файл ещё не дописан, и прямо сейчас обрабатывается? Надо разрулить это на уровне convert(), чтобы записывал промежуточный выхлоп convert во временный файл.
      LOGGER.trace(s"$logPrefix Will stream fs-local img: ${mLocalImgs.fileOf(localInst)}")
      mLocalImgs.getStream(localInst)
    } else {
      // Поискать в seaweedfs. Кэшировать для самообороны от флуда.
      val srcFutCached = cacheApiUtil.getOrElseFut( args.dynImgId.fileName + ":stream", expiration = ENSURE_DYN_CACHE_TTL ) {
        val src = mImgs3
          .getStream(args)
          .recoverWithRetries(1, { case ex =>
            LOGGER.debug(s"$logPrefix Img not found in SWFS (${ex.getClass.getSimpleName} ${ex.getMessage}). Will make new locally...")
            val streamFut = for {
              mLocImg <- ensureLocalImgReady(args, cacheResult = true)
            } yield {
              mLocalImgs.getStream(mLocImg)
            }
            Source.futureSource( streamFut )
              // TODO Тут хрень какая-то: конфликт между _ и NotUsed. _ приходит из play-ws.
              .asInstanceOf[Source[ByteString, NotUsed]]
          })
        Future.successful(src)
      }
      Source.futureSource( srcFutCached )
    }
  }

  /**
   * Убедиться, что картинка доступна локально для раздачи клиентам.
   * Для подавления параллельных запросов используется play.Cache, кеширующий фьючерсы результатов.
   * @param args Запрашиваемая картинка.
   * @param cacheResult Сохранять ли в кеш незакешированный результат этого действия?
   *                    true когда это опережающий запрос подготовки картинки.
   *                    Иначе надо false.
   * @return Фьючерс с экземпляром MLocalImg или экзепшеном получения картинки.
   *         Throwable, если не удалось начать обработку. Такое возможно, если какой-то баг в коде.
   */
  def ensureLocalImgReady(args: MImgT, cacheResult: Boolean): Future[MLocalImg] = {
    // Используем StringBuilder для сборки ключа, т.к. обычно на момент вызова этого метода fileName ещё не собран.
    val resultP = Promise[MLocalImg]()
    val resultFut = resultP.future
    val ck = args.dynImgId.fileName + ":eIR"

    // TODO Тут наверное можно задейстовать cacheApiUtil.
    // TODO Проверять MMedia, что текущий сервак соответствует нужному.
    cache.get [Future[MLocalImg]] (ck).foreach {
      // Результирующего фьючерс нет в кеше. Запускаем поиск/генерацию картинки:
      case None =>
        val localImgResult = mImgs3.toLocalImg( args )
        // Если настроено, фьючерс результата работы сразу кешируем, не дожидаясь результатов:
        if (cacheResult)
          cache.set(ck, resultFut, expiration = ENSURE_DYN_CACHE_TTL)
        // Готовим асинхронный результат работы:
        localImgResult.onComplete {
          case Success(imgOpt) =>
            imgOpt.fold {
              // Картинки в указанном виде нету. Нужно сделать её из оригинала:
              val localResultFut = mkReadyImgToFile(args)
              // В фоне запускаем сохранение полученной картинки в permanent-хранилище (если включено):
              if (SAVE_DERIVATIVES_TO_PERMANENT) {
                for (localImg2 <- localResultFut)
                  mImgs3.saveToPermanent( localImg2.toWrappedImg )
              }
              // Заполняем результат, который уже в кеше:
              resultP.completeWith( localResultFut )
            } { resultP.success }

          case Failure(ex) =>
            resultP.failure(ex)
        }

      case Some(cachedResFut) =>
        resultP.completeWith( cachedResFut )
        //trace(s"ensureImgReady(): cache HIT for $ck -> $cachedResFut")
    }
    resultFut
  }

  private def CONVERT_CACHE_TTL: FiniteDuration = 10.seconds


  /**
   * Конвертация из in в out согласно списку инструкций.
   * @param in Файл с исходным изображением.
   * @param out Файл для конечного изображения.
   * @param imOps Список инструкций, описывающий трансформацию исходной картинки.
   */
  def convert(in: File, out: File, outFmt: MImgFormat, imOps: Seq[ImOp]): Future[_] = {
    val op = new IMOperation

    op.addImage {
      // Надо конвертить без анимации для всего, кроме GIF. Иначе, будут десятки jpeg'ов на выходе согласно кол-ву фреймов в исходнике.
      var inAccTokes = List.empty[String]
      if (!outFmt.imCoalesceFrames)
        inAccTokes ::= "[0]"
      val absPath = in.getAbsolutePath
      // TODO В целях безопасности, надо in-формат тоже указывать, но формат оригинала может быть неправильный у нас. Надо будет внедрить его, когда всё более-менее стабилизируется.
      if (inAccTokes.isEmpty)
        absPath
      else
        (absPath :: inAccTokes).mkString
    }

    // Нужно заставить imagemagick компактовать фреймы между собой при сохранении:
    if (outFmt.imCoalesceFrames)
      op.coalesce()

    for (imOp <- imOps) {
      imOp.addOperation(op)
    }

    // Для gif'а нужно перестроить канву после операций, но перед сохранением:
    if (outFmt.imFinalRepage)
      op.p_repage()

    // Для gif костыли
    if (outFmt.layersOptimize)
      op.layers("Optimize")

    op.addImage(outFmt.imFormat + ":" + out.getAbsolutePath)
    val cmd = new ConvertCmd()
    cmd.setAsyncMode(true)
    val opStr = op.toString

    // Бывает, что происходят двойные одинаковые вызовы из-за слишком сильной параллельности в работе системы.
    // Пытаемся подавить двойные вызовы через короткий Cache.
    cacheApiUtil.getOrElseFut[Int](opStr, expiration = CONVERT_CACHE_TTL) {
      // Запускаем генерацию картинки.
      val listener = new im4jAsyncUtil.Im4jAsyncSuccessProcessListener
      cmd.addProcessEventListener(listener)
      blocking {
        cmd.run(op)
      }
      val resFut = listener.future
      if (LOGGER.underlying.isTraceEnabled()) {
        val logPrefix = s"convert($in=>$out)#${System.currentTimeMillis()}:"
        val tstamp = System.currentTimeMillis() * imOps.hashCode() * in.hashCode()
        LOGGER.trace(s"$logPrefix [$tstamp]\n $$ ${cmd.getCommand.iterator().asScala.mkString(" ")} $opStr")
        for (res <- resFut)
          LOGGER.trace(s"$logPrefix [$tstamp] returned $res, result ${out.length} bytes")
      }
      resFut
    }
  }


  /** Сгенерить превьюшку размера не более 256х256. */
  def thumb256Call(fileName: String, fillArea: Boolean): Call = {
    val img = MImg3(fileName)
    thumb256Call(img, fillArea)
  }
  def thumb256Call(img: MImgT, fillArea: Boolean): Call = {
    val flags = if (fillArea) Seq(ImResizeFlags.FillArea) else Nil
    val op = AbsResizeOp(MSize2di(256, 256), flags)
    val imgThumb = img.withDynOps(
      img.dynImgId.imgOps ++ Seq(op)
    )
    imgCall(imgThumb)
  }


  /** Пройтись по MMedia, найти все записи о картинках-деривативах и произвести для них процедуру стирания.
    *
    * @param deleteEvenStorageMissing Удалять, если файл не найден в хранилище?
    *                                 true означает, что вызывающий этот метод ручается, что все шарды хранилищ активны,
    *                                 и отсутствующие файлы действительно отсутствуют.
    *                                 false - пропускать mmedia для несуществующих файлов.
    * @return Фьючерс с кол-вом обработанных (удалённых) элементов.
    */
  def deleteAllDerivatives(deleteEvenStorageMissing: Boolean): Future[Int] = {
    import mNodes.Implicits._

    // Нельзя использовать deleteByQuery, ведь надо media-storage прочистить.
    // Поэтому рубим по хитрому:
    // 0. Организуем BulkProcessor.
    // 1. Запускаем scroll-проход для поточного чтения всех интересующих MMedia.
    // 2. Удаляем элемент из media-стораджа
    // 3. Отправляем удаление элемента в BulkProcessor.

    val logPrefix = s"deleteAllDerivatives()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix Started. deleteEvenStorageMissing=$deleteEvenStorageMissing")

    // Создаём и запускаем BulkProcessor:
    val bp = mNodes.bulkProcessorLog( this, logPrefix )

    mNodes
      .source[MNode] {
        // Сборка поисковых аргументов для файла.
        new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              predicates      = MPredicates.Blob.File :: Nil,
              fileIsOriginal  = OptionUtil.SomeBool.someFalse,
              fileMimes       = MImgFormats.allMimesIter.toSeq,
            )
            MEsNestedSearch.plain( cr )
          }
          override def limit = 20
        }
          .toEsQuery
      }
      .runFoldAsync( 0 ) { (counter0, fileNode) =>
        val futs = (for {
          fileEdge  <- fileNode.edges.withPredicateIter( MPredicates.Blob.File )
          nodeId    <- fileNode.id
          edgeMedia <- fileEdge.media
          storage   <- edgeMedia.storage
        } yield {
          // Убедиться, что пришёл ожидавшийся элемент:
          if (edgeMedia.file.isOriginal) {
            // should never happen: оригинал картинки пришёл на вход
            LOGGER.warn( s"$logPrefix Unexpected original mmedia#${nodeId}, skipping it:\n $fileNode" )
            Future.successful(counter0)

          } else if (edgeMedia.file.imgFormatOpt.isEmpty) {
            // should never happen: Пришла не-картинка, а что-то другое.
            LOGGER.warn( s"$logPrefix Media#$nodeId not an image: ${edgeMedia.file.mime getOrElse ""}:\n $edgeMedia #$nodeId" )
            Future.successful(counter0)

          } else {
            for {
              // Удалить из хранилища:
              _ <- {
                LOGGER.debug(s"$logPrefix [$counter0] Will erase mmedia#$nodeId of type ${edgeMedia.file.mime getOrElse ""} size=${edgeMedia.file.sizeB.fold("?")(_.toString)}b wh=${edgeMedia.picture.whPx.orNull} storage=${edgeMedia.storage}")
                iMediaStorages
                  .client( storage.storage )
                  .delete( storage.data )
                  .recover { case _: NoSuchElementException if deleteEvenStorageMissing =>
                    LOGGER.warn(s"$logPrefix Not found file $storage in storage")
                    // TODO А что делать если вдруг шарда отвалилась?
                  }
              }
            } yield {
              // Удалить из MMedia:
              bp.add( mNodes.deleteRequestBuilder( nodeId ).request() )
              LOGGER.info(s"$logPrefix [$counter0] done with mmedia#$nodeId | ${edgeMedia.storage}")

              counter0 + 1
            }
          }
        })
          .toList
        Future.foldLeft(futs)( counter0 )(_ + _)
      }
      .andThen { case tryRes =>
        LOGGER.info(s"$logPrefix Completed with $tryRes")
        bp.close()
      }
  }


  /** Определить размер картинки, а если в операциях записан кроп/ресайз/экстент, то использовать такой размер.
    * Сюда лучше направлять уже существующую картинку, либо кропнутую картинку.
    *
    * @param dynImgId id операции.
    * @return Фьючерс с опциональным размером.
    */
  def getImgWh(dynImgId: MDynImgId): Future[Option[ISize2di]] = {
    lazy val logPrefix = s"getImgWh(${dynImgId.fileName})#${System.currentTimeMillis()}:"

    ImOp.getWhFromOps( dynImgId.imgOps ) match {
      case Some(whOpt @ Some(wh)) =>
        // Размер картинки уже очевиден из кропа или ресайза картинки-дериватива.
        LOGGER.trace(s"$logPrefix Extracted WH from dynOps: $wh")
        Future.successful(whOpt)

      case other =>

        case class ResultingExceptionSz2dOpt( szOpt: Option[ISize2di] ) extends RuntimeException

        (for {
          // Поиск узлов
          fileNodes <- mNodes.multiGetCache {
            // Если getWhFromOps() позволяет, то попытаться скопировать WH с оригинала.
            if (other.isEmpty) {
              LOGGER.trace(s"$logPrefix Ok to use orig.img#${dynImgId.origNodeId} as fallback. Will fetch both orig. and derivative...")
              dynImgId.mediaIdAndOrigMediaId
            } else {
              LOGGER.trace(s"$logPrefix Cannot copy wh from orig, because it differs. Try to fetch only derivative MMedia...")
              dynImgId.mediaId :: Nil
            }
          }

          if fileNodes.nonEmpty || {
            LOGGER.warn(s"$logPrefix Requested file media Nodes (orig&dyn) not exist.")
            throw ResultingExceptionSz2dOpt( None )
          }

          _ = {
            LOGGER.trace(s"$logPrefix Found ${fileNodes.size} medias-nodes:\n ${fileNodes.iterator.flatMap(_.id).mkString("\n ")}")
            (for {
              fileNode  <- fileNodes.iterator
              e         <- fileNode.edges.withPredicateIter( MPredicates.Blob.File )
              media     <- e.media.iterator
              whPx      <- media.picture.whPx.iterator
            } yield {
              val isDerivedImg = !media.file.isOriginal
              isDerivedImg -> whPx
            })
              .toSeq
              .sortBy( _._1 )
              .headOption
              .foreach { wh =>
                throw ResultingExceptionSz2dOpt( Some(wh._2) )
              }
          }

          // Защита от запуска действий по доступу к несуществующей картинке.
          mimg  <- ensureLocalImgReady( MImg3(dynImgId), cacheResult = true )
          whOpt <- mLocalImgs.getImageWH( mimg )

        } yield {
          LOGGER.trace(s"$logPrefix WH derived from handly-converted img: $whOpt")
          whOpt
        })
          .recover {
            case ResultingExceptionSz2dOpt( whOpt ) =>
              whOpt
          }
    }
  }


  import mNodes.Implicits._

  /** Пройтись по всем узлам, которые ссылаются на картинки, поковырятся там в jdEdge, выставив
    * dynFormat согласно оригиналам картинок, на которые они ссылаются.
    *
    * @param nodeIds id узлов, которые требуется отработать.
    *                Если пусто, то все узлы.
    * @return Фьючерс с кол-вом обработанных узлов.
    */
  def resetJdImgDynFormatsToOrigOnNodes(nodeIds: Seq[String] = Nil): Future[Int] = {
    val logPrefix = s"resetJdImgDynFormatsToOrigOnNodes()#${System.currentTimeMillis()}:"

    val nodesPerTime = 10

    val msearch = new MNodeSearch {
      override val outEdges: MEsNestedSearch[Criteria] = {
        val cr = Criteria(
          predicates = MPredicates.JdContent.Image :: Nil
        )
        MEsNestedSearch.plain( cr )
      }
      override def withIds = nodeIds
      override def limit = nodesPerTime
    }

    val bp = mNodes.bulkProcessorLog( this, logPrefix )

    val countProcessed = new AtomicInteger(0)

    val mnode_extras_doc_template_LENS = MNode.extras
      .composeLens( MNodeExtras.doc )
      .composeTraversal( Traversal.fromTraverse[Option, MNodeDoc] )
      .composeLens( MNodeDoc.template )

    val jdt_p1_bgImg_LENS = JdTag.props1
      .composeLens( MJdProps1.bgImg )

    val jdt_qdProps_edgeInfo_LENS = JdTag.qdProps
      .composeTraversal( Traversal.fromTraverse[Option, MQdOp] )
      .composeLens( MQdOp.edgeInfo )
    val mnode_extras_adn_resView_LENS = MNode.extras
      .composeLens( MNodeExtras.adn )
      .composeTraversal( Traversal.fromTraverse[Option, MAdnExtra] )
      .composeLens( MAdnExtra.resView )

    mNodes
      .source[MNode]( msearch.toEsQuery )
      .mapAsyncUnordered(nodesPerTime) { mnode =>
        val edgesByUid = mnode.edges.edgesByUid
        val logPrefix2 = s"$logPrefix Node#${mnode.idOrNull}:"

        val jdImgEdgeIdsIter: IterableOnce[MJdEdgeId] = mnode.common.ntype match {
          // Рекламная карточка. Растрясти шаблон документа карточки:
          case MNodeTypes.Ad =>
            for {
              doc <- mnode.extras.doc.iterator
              jdTag <- EphemeralStream.toIterable( doc.template.flatten ).iterator
              imgEdge <- jdTag.imgEdgeUids
            } yield {
              imgEdge
            }

          // Adn-узел. Проверить MAdnResView:
          case MNodeTypes.AdnNode =>
            // Собрать данные для запроса по id к MMedia:
            mnode.extras.adn
              .iterator
              .flatMap(_.resView.edgeUids)

          // Should never happen.
          case _ =>
            ???
        }

        val mediaId2edgeUid = (for {
          jdEdgeId  <- jdImgEdgeIdsIter.iterator
          medge     <- edgesByUid.get( jdEdgeId.edgeUid ).iterator
          if medge.predicate ==* MPredicates.JdContent.Image
          imgNodeId <- medge.nodeIds
        } yield {
          val mediaId = MDynImgId(imgNodeId).mediaId
          jdEdgeId.edgeUid -> mediaId
        })
          .toMap

        LOGGER.trace(s"$logPrefix2 mediaId2edgesUid:\n ${mediaId2edgeUid.mkString(", \n")}")

        for {
          // Поискать оригинал картинки через media-кэш. Это даст ускорение.
          mmediasMap <- mNodes.multiGetMapCache( mediaId2edgeUid.values.toSet )
        } yield {
          def _upgradeJdIdOpt(jdIdOpt: Option[MJdEdgeId]): Option[MJdEdgeId] = {
            (for {
              jdId        <- jdIdOpt
              jdMediaId   <- mediaId2edgeUid.get( jdId.edgeUid )
              mNode       <- mmediasMap.get( jdMediaId )
              fileEdge    <- mNode.edges
                .withPredicateIter( MPredicates.Blob.File )
                .nextOption()
              edgeMedia   <- fileEdge.media
            } yield {
              val jdId2 = (MJdEdgeId.outImgFormat set edgeMedia.file.imgFormatOpt)(jdId)
              LOGGER.debug(s"$logPrefix2 UPGRADED edge: $jdId2")
              jdId2
            })
              .orElse {
                LOGGER.trace(s"$logPrefix2 Ignored edge $jdIdOpt")
                jdIdOpt
              }
          }

          val mnode2: MNode = mnode.common.ntype match {
            case MNodeTypes.Ad =>
              mnode_extras_doc_template_LENS
                .modify { template0 =>
                  for (jdTag0 <- template0) yield {
                    // обновить bgImg
                    var jdTag2 = jdTag0

                    if ( jdt_p1_bgImg_LENS.get(jdTag2).nonEmpty )
                      jdTag2 = jdt_p1_bgImg_LENS.modify( _upgradeJdIdOpt )(jdTag2)

                    // обновить qd edgeid
                    if (jdt_qdProps_edgeInfo_LENS.exist( _.nonEmpty )(jdTag2) )
                      jdTag2 = jdt_qdProps_edgeInfo_LENS.modify( _upgradeJdIdOpt )(jdTag2)

                    // Вернуть обновлённый тег
                    jdTag2
                  }
                }(mnode)

            case MNodeTypes.AdnNode =>
              mnode_extras_adn_resView_LENS
                .modify { rv =>
                  rv.copy(
                    logo = _upgradeJdIdOpt(rv.logo),
                    wcFg = _upgradeJdIdOpt(rv.wcFg),
                    galImgs = rv.galImgs
                      .flatMap { galImg => _upgradeJdIdOpt(Some(galImg)) }
                  )
                }(mnode)

            // Should never happen
            case unsupportedNtype =>
              throw new UnsupportedOperationException(s"$logPrefix Not supported node[${mnode.idOrNull}] type: $unsupportedNtype\n | Node info = ${mnode.guessDisplayNameOrIdOrQuestions}")
          }

          bp.add( mNodes.prepareIndex(mnode2).request() )
          countProcessed.incrementAndGet()
          mnode2
        }
      }
      .runForeach { mnode2 =>
        LOGGER.trace(s"$logPrefix Processed ${mnode2.common.ntype} #${mnode2.idOrNull}")
      }
      .map { _ =>
        bp.close()
        countProcessed.get()
      }
  }

}


/** Интерфейс поддержки JMX для [[DynImgUtil]]. */
trait DynImgUtilJmxMBean {
  def deleteAllDerivatives(deleteEvenStorageMissing: Boolean): String
  def resetJdImgDynFormatsToOrig(): String
  def resetJdImgDynFormatsToOrigOnNode(nodeId: String): String
}

/** Реализация поддержки JMX для [[DynImgUtil]]. */
final class DynImgUtilJmx @Inject() (
                                      injector                  : Injector,
                                    )
  extends JmxBase
  with DynImgUtilJmxMBean
  with MacroLogsDyn
{

  private def dynImgUtil = injector.instanceOf[DynImgUtil]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  import JmxBase._

  override def _jmxType = Types.IMG

  override def deleteAllDerivatives(deleteEvenStorageMissing: Boolean): String = {
    val logPrefix = s"deleteAllDerivatives():"
    LOGGER.warn(s"$logPrefix Starting")
    val fut = for {
      countDeleted <- dynImgUtil.deleteAllDerivatives(deleteEvenStorageMissing)
    } yield {
      s"$logPrefix Deleted $countDeleted items."
    }
    awaitString(fut)
  }

  override def resetJdImgDynFormatsToOrig(): String = {
    val fut = for {
      countProcessed <- dynImgUtil.resetJdImgDynFormatsToOrigOnNodes()
    } yield {
      s"Done. $countProcessed nodes"
    }
    awaitString(fut)
  }

  override def resetJdImgDynFormatsToOrigOnNode(nodeId: String): String = {
    val fut = for {
      countProcessed <- dynImgUtil.resetJdImgDynFormatsToOrigOnNodes( nodeId :: Nil )
    } yield {
      s"Done. $countProcessed nodes: $nodeId"
    }
    awaitString(fut)
  }

}
