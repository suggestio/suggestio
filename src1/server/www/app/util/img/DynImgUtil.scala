package util.img

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import javax.inject.{Inject, Singleton}
import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.routes
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.img.{MImgFmt, MImgFmts}
import io.suggest.jd.MJdEdgeId
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.media.{MMedia, MMedias, MMediasCache}
import io.suggest.model.n2.media.search.MMediaSearchDfltImpl
import io.suggest.model.n2.media.storage.IMediaStorages
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.url.MHostInfo
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.mproj.ICommonDi
import org.im4java.core.{ConvertCmd, IMOperation}
import play.api.mvc.Call
import util.cdn.CdnUtil
import japgolly.univeq._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 12:25
 * Description: Утиль для работы с динамическими картинками.
 * В основном -- это прослойка между img-контроллером и моделью orig img и смежными ей.
 */
@Singleton
class DynImgUtil @Inject() (
                             mImgs3                    : MImgs3,
                             mLocalImgs                : MLocalImgs,
                             im4jAsyncUtil             : Im4jAsyncUtil,
                             cdnUtil                   : CdnUtil,
                             // Для каких-то регламентных операций в MNodes:
                             mNodes                    : MNodes,
                             // Только для удаления и чистки базы:
                             iMediaStorages            : IMediaStorages,
                             mMedias                   : MMedias,
                             mMediasCache              : MMediasCache,
                             mCommonDi                 : ICommonDi
                           )
  extends MacroLogsImpl
{

  import mCommonDi._

  /** Сколько времени кешировать результат подготовки картинки?
    * Кеш используется для подавления параллельных запросов. */
  private val ENSURE_DYN_CACHE_TTL = 10.seconds

  /** Если true, то производные от оригинала картники будут дублироваться в cassandra.
    * Если false, то производные будут только на локалхосте. */
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
      mediaIds      = mimg.dynImgId.mediaIdWithOriginalMediaId
    )
  }

  /** Собрать список MMedia для перечисленных картинок. Порядок любой. */
  def imgs2medias(imgs: TraversableOnce[MImgT]): Future[Seq[MMedia]] = {
    mMediasCache.multiGet {
      imgs
        .toIterator
        .flatMap { mimg =>
          mimg.dynImgId
            .mediaIdWithOriginalMediaId
        }
    }
  }

  /** Сборка media-hosts map для картинок. */
  def mkMediaHostsMap(imgs: TraversableOnce[MImgT]): Future[Map[String, Seq[MHostInfo]]] = {
    if (imgs.isEmpty) {
      Future.successful( Map.empty )
    } else {
      for {
        // Получить на руки media-инстансы для оригиналов картинок:
        medias <- imgs2medias(imgs)

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
    val fut = mImgs3.toLocalImg(args.original)
      .map(_.get)
      .flatMap { localImg =>
        // Есть исходная картинка в файле. Пора пережать её согласно настройкам.
        val newLocalImg = args.toLocalInstance
        // Запустить конвертацию исходной картинки
        for {
          _ <- convert(
            in     = mLocalImgs.fileOf(localImg),
            out    = mLocalImgs.fileOf(newLocalImg),
            outFmt = newLocalImg.dynImgId.dynFormat,
            imOps  = args.dynImgId.dynImgOps
          )
        } yield {
          // Вернуть финальную картинку, т.к. с оригиналом и так всё ясно.
          newLocalImg
        }
      }
    for (ex <- fut.failed) {
      val logPrefix = s"mkReadyImgToFile($args): "
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
            Source.fromFutureSource(streamFut)
              // TODO Тут хрень какая-то: конфликт между _ и NotUsed. _ приходит из play-ws.
              .asInstanceOf[Source[ByteString, NotUsed]]
          })
        Future.successful(src)
      }
      Source.fromFutureSource(srcFutCached)
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
          case Success(Some(img)) =>
            resultP.success( img )

          // Картинки в указанном виде нету. Нужно сделать её из оригинала:
          case Success(None) =>
            val localResultFut = mkReadyImgToFile(args)
            // В фоне запускаем сохранение полученной картинки в permanent-хранилище (если включено):
            if (SAVE_DERIVATIVES_TO_PERMANENT) {
              for (localImg2 <- localResultFut)
                mImgs3.saveToPermanent( localImg2.toWrappedImg )
            }
            // Заполняем результат, который уже в кеше:
            resultP.completeWith( localResultFut )

          case Failure(ex) =>
            resultP.failure(ex)
        }

      case Some(cachedResFut) =>
        resultP.completeWith( cachedResFut )
        //trace(s"ensureImgReady(): cache HIT for $ck -> $cachedResFut")
    }
    resultFut
  }

  val CONVERT_CACHE_TTL: FiniteDuration = {
    configuration.getOptional[Int]("dyn.img.convert.cache.seconds")
      .getOrElse(10)
      .seconds
  }


  /**
   * Конвертация из in в out согласно списку инструкций.
   * @param in Файл с исходным изображением.
   * @param out Файл для конечного изображения.
   * @param imOps Список инструкций, описывающий трансформацию исходной картинки.
   */
  def convert(in: File, out: File, outFmt: MImgFmt, imOps: Seq[ImOp]): Future[_] = {
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
      cmd.run(op)
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
      img.dynImgId.dynImgOps ++ Seq(op)
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
    import mMedias.Implicits._

    // Нельзя использовать deleteByQuery, ведь надо media-storage прочистить.
    // Поэтому рубим по хитрому:
    // 0. Организуем BulkProcessor.
    // 1. Запускаем scroll-проход для поточного чтения всех интересующих MMedia.
    // 2. Удаляем элемент из media-стораджа
    // 3. Отправляем удаление элемента в BulkProcessor.

    val logPrefix = s"deleteAllDerivatives()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix Started. deleteEvenStorageMissing=$deleteEvenStorageMissing")

    // Сборка поисковых аргументов для файла.
    val searchArgs = new MMediaSearchDfltImpl {
      override def limit = 20
      override def isOriginalFile = Some(false)
      override def fileMimes: Seq[String] = {
        MImgFmts.allMimesIter.toSeq
      }
    }

    val src = mMedias.source[MMedia]( searchArgs.toEsQuery )

    // Создаём и запускаем BulkProcessor:
    val bp = mMedias.bulkProcessor(
      listener = new mMedias.BulkProcessorListener( logPrefix + "BULK:" )
    )

    // Заготовить чтение элементов из elasticsearch:
    src
      .runFoldAsync( 0 ) { (counter0, mmedia) =>
        // Убедиться, что пришёл ожидавшийся элемент:
        if (mmedia.file.isOriginal) {
          // should never happen: оригинал картинки пришёл на вход
          LOGGER.warn( s"$logPrefix Original mmedia#${mmedia.idOrNull}, skipping it:\n $mmedia" )
          Future.successful(counter0)

        } else if (mmedia.file.imgFormatOpt.isEmpty) {
          // should never happen: Пришла не-картинка, а что-то другое.
          LOGGER.warn( s"$logPrefix Media#${mmedia.idOrNull} not an image: ${mmedia.file.mime}:\n $mmedia" )
          Future.successful(counter0)

        } else {
          for {
            // Удалить из хранилища:
            _ <- {
              LOGGER.debug(s"$logPrefix [$counter0] Will erase mmedia#${mmedia.idOrNull} of type ${mmedia.file.mime} size=${mmedia.file.sizeB}b wh=${mmedia.picture.whPx.orNull} storage=${mmedia.storage}")
              iMediaStorages
                .delete( mmedia.storage )
                .recover { case _: NoSuchElementException if deleteEvenStorageMissing =>
                  LOGGER.warn(s"$logPrefix Not found file ${mmedia.storage} in storage")
                  // TODO А что делать если вдруг шарда отвалилась?
                }
            }
          } yield {
            // Удалить из MMedia:
            bp.add( mMedias.deleteRequestBuilder(mmedia.id.get).request() )
            LOGGER.info(s"$logPrefix [$counter0] done with mmedia#${mmedia.idOrNull} | ${mmedia.storage}")

            counter0 + 1
          }
        }
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

    ImOp.getWhFromOps( dynImgId.dynImgOps ) match {
      case Some(whOpt @ Some(wh)) =>
        // Размер картинки уже очевиден из
        LOGGER.trace(s"$logPrefix Extracted WH from dynOps: $wh")
        Future.successful(whOpt)

      case other =>
        val mediasFut = mMediasCache.multiGet {
          // Если getWhFromOps() позволяет, то попытаться скопировать WH с оригинала.
          if (other.isEmpty) {
            LOGGER.trace(s"$logPrefix Ok to use orig.img#${dynImgId.rowKeyStr} as fallback. Will fetch both orig. and derivative MMedias...")
            dynImgId.mediaIdWithOriginalMediaId
          } else {
            LOGGER.trace(s"$logPrefix Cannot copy wh from orig, because it differs. Try to fetch only derivative MMedia...")
            dynImgId.mediaId :: Nil
          }
        }

        mediasFut.flatMap { medias =>
          if (medias.isEmpty) {
            LOGGER.warn(s"$logPrefix Requested MMedia (orig&dyn) not exist.")
            Future.successful(None)

          } else {
            LOGGER.trace(s"$logPrefix Found ${medias.size} mmedias")
            val mediaWhOpt = medias
              .sortBy(m => !m.file.isOriginal)
              .iterator
              .flatMap(_.picture.whPx)
              .toStream
              .headOption

            mediaWhOpt.fold [Future[Option[ISize2di]]] {
              for {
                // Защита от запуска действий по доступу к несуществующей картинке.
                mimg  <- ensureLocalImgReady(MImg3(dynImgId), cacheResult = true)
                whOpt <- mLocalImgs.getImageWH( mimg )
              } yield {
                LOGGER.trace(s"$logPrefix WH derived from handly-converted img: $whOpt")
                whOpt
              }
            } { wh =>
              // Размер картинки уже очевиден из dynOps.
              LOGGER.trace(s"$logPrefix WH derived from MMedia: $wh")
              Future.successful( Some(wh) )
            }
          }
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

    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[Criteria] = {
        val cr = Criteria(
          predicates = MPredicates.JdContent.Image :: Nil
        )
        cr :: Nil
      }
      override def withIds = nodeIds
      override def limit = nodesPerTime
    }

    val bp = mNodes.bulkProcessor(
      new mNodes.BulkProcessorListener(logPrefix)
    )
    val origImgFmt = MImgFmts.default

    val countProcessed = new AtomicInteger(0)

    mNodes
      .source[MNode]( msearch.toEsQuery )
      .mapAsyncUnordered(nodesPerTime) { mnode =>
        val edgesByUid = mnode.edges.edgesByUid
        val logPrefix2 = s"$logPrefix Node#${mnode.idOrNull}:"

        val jdImgEdgeIdsIter: TraversableOnce[MJdEdgeId] = mnode.common.ntype match {
          // Рекламная карточка. Растрясти шаблон документа карточки:
          case MNodeTypes.Ad =>
            for {
              doc <- mnode.extras.doc.iterator
              jdTag <- doc.template.flatten
              imgEdge <- jdTag.edgeUids
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
          jdEdgeId  <- jdImgEdgeIdsIter.toIterator
          medge     <- edgesByUid.get( jdEdgeId.edgeUid ).iterator
          if medge.predicate ==* MPredicates.JdContent.Image
          imgNodeId <- medge.nodeIds
        } yield {
          val mediaId = MDynImgId(imgNodeId, origImgFmt).mediaId
          jdEdgeId.edgeUid -> mediaId
        })
          .toMap
        LOGGER.trace(s"$logPrefix2 mediaId2edgesUid:\n ${mediaId2edgeUid.mkString(", \n")}")

        for {
          // Поискать оригинал картинки через media-кэш. Это даст ускорение.
          mmediasMap <- mMediasCache.multiGetMap( mediaId2edgeUid.values.toSet )
        } yield {
          def _upgradeJdIdOpt(jdIdOpt: Option[MJdEdgeId]): Option[MJdEdgeId] = {
            val resOpt = for {
              jdId        <- jdIdOpt
              jdMediaId   <- mediaId2edgeUid.get( jdId.edgeUid )
              mmedia      <- mmediasMap.get( jdMediaId )
            } yield {
              val jdId2 = jdId.withOutImgFormat( mmedia.file.imgFormatOpt )
              LOGGER.debug(s"$logPrefix2 UPGRADED edge: $jdId2")
              jdId2
            }
            resOpt.orElse {
              LOGGER.trace(s"$logPrefix2 Ignored edge $jdIdOpt")
              jdIdOpt
            }
          }

          val mnode2: MNode = mnode.common.ntype match {
            case MNodeTypes.Ad =>
              mnode.withExtras(
                mnode.extras.withDoc(
                  for (doc <- mnode.extras.doc) yield {
                    doc.withTemplate(
                      for (jdTag0 <- doc.template) yield {
                        // обновить bgImg
                        var jdTag2 = jdTag0
                        for (_ <- jdTag2.props1.bgImg) {
                          jdTag2 = jdTag2.withProps1(
                            jdTag2.props1.withBgImg(
                              _upgradeJdIdOpt( jdTag2.props1.bgImg )
                            )
                          )
                        }
                        // обновить qd edgeid
                        for {
                          qdProps <- jdTag2.qdProps
                          _       <- qdProps.edgeInfo
                        } {
                          jdTag2 = jdTag2.withQdProps(
                            Some(qdProps.withEdgeInfo(
                              _upgradeJdIdOpt( qdProps.edgeInfo )
                            ))
                          )
                        }

                        // Вернуть обновлённый тег
                        jdTag2
                      }
                    )
                  }
                )
              )

            case MNodeTypes.AdnNode =>
              mnode.withExtras(
                mnode.extras.withAdn(
                  for (adn <- mnode.extras.adn) yield {
                    val rv = adn.resView
                    adn.withResView(
                      rv.copy(
                        logo = _upgradeJdIdOpt(rv.logo),
                        wcFg = _upgradeJdIdOpt(rv.wcFg),
                        galImgs = rv.galImgs
                          .flatMap { galImg => _upgradeJdIdOpt(Some(galImg)) }
                      )
                    )
                  }
                )
              )

            // Should never happen
            case _ =>
              ???
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

/** Интерфейс для доступа к DI-полю с утилью для DynImg. */
trait IDynImgUtil {
  def dynImgUtil: DynImgUtil
}


/** Интерфейс поддержки JMX для [[DynImgUtil]]. */
trait DynImgUtilJmxMBean {
  def deleteAllDerivatives(deleteEvenStorageMissing: Boolean): String
  def resetJdImgDynFormatsToOrig(): String
  def resetJdImgDynFormatsToOrigOnNode(nodeId: String): String
}

/** Реализация поддержки JMX для [[DynImgUtil]]. */
final class DynImgUtilJmx @Inject() (
                                      dynImgUtil                : DynImgUtil,
                                      override implicit val ec  : ExecutionContext
                                    )
  extends JMXBase
  with DynImgUtilJmxMBean
  with MacroLogsImpl
{

  override def jmxName: String = "io.suggest:type=img,name=" + classOf[DynImgUtil].getSimpleName
  override def futureTimeout = 5.minutes

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
