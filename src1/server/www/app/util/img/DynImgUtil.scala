package util.img

import java.io.File

import com.google.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.im._
import models.mproj.ICommonDi
import org.im4java.core.{ConvertCmd, IMOperation}
import play.api.mvc.Call

import scala.collection.JavaConversions._
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
  mCommonDi                 : ICommonDi
)
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Сколько времени кешировать результат подготовки картинки?
    * Кеш используется для подавления параллельных запросов. */
  val ENSURE_DYN_CACHE_TTL = {
    configuration.getInt("img.dyn.ensure.cache.ttl.seconds")
      .getOrElse(10)
      .seconds
  }

  /** Если true, то производные от оригинала картники будут дублироваться в cassandra.
    * Если false, то производные будут только на локалхосте. */
  val SAVE_DERIVATIVES_TO_PERMANENT: Boolean = {
    configuration.getBoolean("img.dyn.derivative.save.to.permanent.enabled")
      .getOrElse(true)
  }

  /** Активен ли префетчинг, т.е. опережающая подготовка картинки?
    * Префетчинг начинается асинхронно в момент генерации ссылки на картинку. */
  val PREFETCH_ENABLED: Boolean = {
    configuration.getBoolean("img.dyn.prefetch.enabled")
      .getOrElse(true)
  }

  info(s"DynImgUtil: esnureCache=$ENSURE_DYN_CACHE_TTL, saveDerivatives=$SAVE_DERIVATIVES_TO_PERMANENT, prefetch=$PREFETCH_ENABLED")

  /**
   * Враппер для вызова routes.Img.dynImg(). Нужен чтобы навешивать сайд-эффекты и трансформировать результат вызова.
   * @param dargs Аргументы генерации картинки.
   * @return Экземпляр Call, пригодный к употреблению.
   */
  def imgCall(dargs: MImgT): Call = {
    if (PREFETCH_ENABLED) {
      Future {
        ensureImgReady(dargs, cacheResult = true)
      } .flatMap(identity)
        .onFailure { case ex =>
          error("Failed to prefetch dyn.image: " + dargs.fileName, ex)
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
            in  = mLocalImgs.fileOf(localImg),
            out = mLocalImgs.fileOf(newLocalImg),
            imOps = args.dynImgOps
          )
        } yield {
          // Вернуть финальную картинку, т.к. с оригиналом и так всё ясно.
          newLocalImg
        }
      }
    fut.onFailure { case ex =>
      val logPrefix = s"mkReadyImgToFile($args): "
      val msg = ex match {
        case nsee: NoSuchElementException =>
          "Image original does not exists in storage"
        case _ =>
          "Unknown exception during image prefetch"
      }
      error(logPrefix + msg, ex)
    }
    fut
  }


  /**
   * Убедиться, что картинка доступна локально для раздачи клиентам.
   * Для подавления параллельных запросов используется play.Cache, кеширующий фьючерсы результатов.
   * @param args Запрашиваемая картинка.
   * @param cacheResult Сохранять ли в кеш незакешированный результат этого действия?
   *                    true когда это опережающий запрос подготовки картинки.
   *                    Иначе надо false.
   * @param saveToPermanent Переопределить значение настройки [[SAVE_DERIVATIVES_TO_PERMANENT]].
   * @return Фьючерс с экземпляром MLocalImg или экзепшеном получения картинки.
   *         Throwable, если не удалось начать обработку. Такое возможно, если какой-то баг в коде.
   */
  def ensureImgReady(args: MImgT, cacheResult: Boolean, saveToPermanent: Boolean = SAVE_DERIVATIVES_TO_PERMANENT): Future[MLocalImg] = {
    // Используем StringBuilder для сборки ключа, т.к. обычно на момент вызова этого метода fileName ещё не собран.
    val resultP = Promise[MLocalImg]()
    val resultFut = resultP.future
    val ck = args.fileNameSb()
      .append(":eIR")
      .toString()
    // TODO Тут наверное можно задейстовать cacheApiUtil.
    cache.get [Future[MLocalImg]] (ck) match {
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
            if (saveToPermanent) {
              localResultFut.onSuccess { case localImg2 =>
                mImgs3.saveToPermanent( localImg2.toWrappedImg )
              }
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
    configuration.getInt("dyn.img.convert.cache.seconds")
      .getOrElse(10)
      .seconds
  }


  /**
   * Конвертация из in в out согласно списку инструкций.
   * @param in Файл с исходным изображением.
   * @param out Файл для конечного изображения.
   * @param imOps Список инструкций, описывающий трансформацию исходной картинки.
   */
  def convert(in: File, out: File, imOps: Seq[ImOp]): Future[_] = {
    val op = new IMOperation
    op.addImage(in.getAbsolutePath + "[0]")
    for (imOp <- imOps) {
      imOp.addOperation(op)
    }
    op.addImage(out.getAbsolutePath)
    val cmd = new ConvertCmd()
    cmd.setAsyncMode(true)
    val opStr = op.toString
    // Бывает, что происходят двойные одинаковые вызовы из-за слишком сильной параллельности в работе системы.
    // Пытаемся подавить двойные вызовы через короткий Cache.
    cacheApiUtil.getOrElseFut[Int](opStr, expiration = CONVERT_CACHE_TTL) {
      // Запускаем генерацию картинки.
      val listener = new Im4jAsyncSuccessProcessListener
      cmd.addProcessEventListener(listener)
      cmd.run(op)
      val resFut = listener.future
      if (LOGGER.underlying.isTraceEnabled()) {
        val tstamp = System.currentTimeMillis() * imOps.hashCode() * in.hashCode()
        trace(s"convert(): [$tstamp] ${cmd.getCommand.mkString(" ")} $opStr")
        resFut onSuccess { case res =>
          trace(s"convert(): [$tstamp] returned $res, result ${out.length} bytes")
        }
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
    val op = AbsResizeOp(MImgInfoMeta(256, 256), flags)
    val imgThumb = img.withDynOps(
      img.dynImgOps ++ Seq(op)
    )
    imgCall(imgThumb)
  }

}

