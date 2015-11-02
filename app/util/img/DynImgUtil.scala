package util.img

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.UUID

import com.google.inject.{Singleton, Inject}
import controllers.routes
import io.suggest.playx.CacheApiUtil
import io.suggest.util.UuidUtil
import models._
import models.im._
import org.im4java.core.{ConvertCmd, IMOperation}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.mvc.Call
import util.PlayMacroLogsImpl
import util.async.AsyncUtil
import scala.collection.JavaConversions._

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.duration._
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
  cacheApi          : CacheApi,
  cacheApiUtil      : CacheApiUtil,
  configuration     : Configuration,
  implicit val ec   : ExecutionContext
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

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
    val img = MImg(filename)
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
    val fut = args
      .original
      .toLocalImg
      .map(_.get)
      .flatMap { localImg =>
        // Есть исходная картинка в файле. Пора пережать её согласно настройкам.
        val newLocalImg = args.toLocalInstance
        convert(localImg.file, newLocalImg.file, args.dynImgOps) map { _ =>
          newLocalImg
        }
      }
    fut onFailure { case ex =>
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
    cacheApi.get [Future[MLocalImg]] (ck) match {
      // Результирующего фьючерс нет в кеше. Запускаем поиск/генерацию картинки:
      case None =>
        val localImgResult = args.toLocalImg
        // Если настроено, фьючерс результата работы сразу кешируем, не дожидаясь результатов:
        if (cacheResult)
          cacheApi.set(ck, resultFut, expiration = ENSURE_DYN_CACHE_TTL)
        // Готовим асинхронный результат работы:
        localImgResult onComplete {
          case Success(Some(img)) =>
            resultP success img

          // Картинки в указанном виде нету. Нужно сделать её из оригинала:
          case Success(None) =>
            val localResultFut = mkReadyImgToFile(args)
            // В фоне запускаем сохранение полученной картинки в permanent-хранилище (если включено):
            if (saveToPermanent) {
              localResultFut onSuccess { case localImg2 =>
                localImg2.toWrappedImg
                  .saveToPermanent
              }
            }
            // Заполняем результат, который уже в кеше:
            resultP completeWith localResultFut

          case Failure(ex) =>
            resultP failure ex
        }

      case Some(cachedResFut) =>
        resultP completeWith cachedResFut
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
    imOps.foreach { imOp =>
      imOp addOperation op
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
      trace("convert(): " + cmd.getCommand.mkString(" ") + " " + opStr)
      cmd run op
      val resFut = listener.future
      resFut onSuccess { case res =>
        trace(s"convert(): returned $res, result ${out.length} bytes")
      }
      resFut
    }
  }


  /**
   * Сохранение картинки, сгенеренной в mkReadyImgToFile().
   * Метод блокируется на i/o операциях.
   * @param imgFile Файл с картинкой.
   * @param rowKey Сохранить по указанному ключу.
   * @param qualifier Сохранить в указанной колонке.
   * @param saveDt Сохранить этой датой.
   * @return Фьючерс для синхронизации.
   */
  def saveDynImg(imgFile: File, rowKey: UUID, qualifier: String, saveDt: DateTime): Future[_] = {
    // TODO Отделить толстые внешние операции от остальных чтобы улучшить распараллеливание:
    val imgBytes = Files.readAllBytes(imgFile.toPath)
    // Запускаем сохранение исходной картинки
    val mui2 = MUserImg2(id = rowKey, q = qualifier, img = ByteBuffer.wrap(imgBytes), timestamp = saveDt)
    val imgSaveFut = mui2.save
    lazy val rowKeyStr = UuidUtil.uuidToBase64(rowKey)
    imgSaveFut onComplete {
      case Success(saveImgResult) => trace("Dyn img saved ok: " + saveImgResult)
      case Failure(ex)            => error(s"Failed to save dyn img id[$rowKeyStr] q[$qualifier]", ex)
    }
    // Сохраняем метаданные:
    val imgInfo = OrigImageUtil.identify(imgFile)
    val md = ImgFormUtil.identifyInfo2md(imgInfo)
    val muiMd2 = MUserImgMeta2(id = rowKey, q = qualifier, md = md, timestamp = saveDt)
    val muiMdSaveFut = muiMd2.save
    muiMdSaveFut.onComplete {
      case Success(saveMetaResult) => trace("Dyn img meta saved ok: " + saveMetaResult)
      case Failure(ex)             => error(s"Dyn img meta failed to save: id[$rowKeyStr] q[$qualifier]", ex)
    }
    // Удаляем файл, исходный файл
    imgFile.delete()
    imgSaveFut.flatMap(_ => muiMdSaveFut)
  }

  /**
   * Полностью неблокирующий вызов к saveDynImg(). Все блокировки идут в отдельном потоке.
   * @param imgFile Файл с картинкой.
   * @param rowKey Сохранить по указанному ключу.
   * @param qualifier Сохранить в указанной колонке.
   * @param saveDt Сохранить этой датой.
   * @return Фьючерс для синхронизации.
   */
  def saveDynImgAsync(imgFile: File, rowKey: UUID, qualifier: String, saveDt: DateTime): Future[_] = {
    val futFut = Future {
      saveDynImg(imgFile, rowKey, qualifier, saveDt)
    }(AsyncUtil.singleThreadCpuContext)
    // Распрямить вложенный фьючерс.
    futFut flatMap identity
  }


  /** Сгенерить превьюшку размера не более 256х256. */
  def thumb256Call(fileName: String, fillArea: Boolean): Call = {
    val img = MImg(fileName)
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

