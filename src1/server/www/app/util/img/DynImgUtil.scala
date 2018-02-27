package util.img

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.routes
import io.suggest.common.geom.d2.MSize2di
import io.suggest.img.MImgFmt
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.mproj.ICommonDi
import org.im4java.core.{ConvertCmd, IMOperation}
import play.api.mvc.Call

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
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


  //LOGGER.trace(s"DynImgUtil: ensureCache=$ENSURE_DYN_CACHE_TTL, saveDerivatives=$SAVE_DERIVATIVES_TO_PERMANENT, prefetch=$PREFETCH_ENABLED")

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
      val msg = ex match {
        case _: NoSuchElementException =>
          "Image original does not exists in storage"
        case _ =>
          "Unknown exception during image prefetch"
      }
      LOGGER.error(logPrefix + msg, ex)
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
        LOGGER.trace(s"$logPrefix [$tstamp]\n ${cmd.getCommand.iterator().asScala.mkString(" ")}\n $opStr")
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

}

/** Интерфейс для доступа к DI-полю с утилью для DynImg. */
trait IDynImgUtil {
  def dynImgUtil: DynImgUtil
}
