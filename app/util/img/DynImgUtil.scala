package util.img

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.UUID

import controllers.routes
import io.suggest.util.UuidUtil
import models._
import models.im.{MLocalImg, MImg, ImOp}
import org.im4java.core.{ConvertCmd, IMOperation}
import org.joda.time.DateTime
import play.api.mvc.Call
import util.{AsyncUtil, PlayMacroLogsImpl}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 12:25
 * Description: Утиль для работы с динамическими картинками.
 * В основном -- это прослойка между img-контроллером и моделью orig img и смежными ей.
 */
object DynImgUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Враппер для вызова routes.Img.dynImg(). Нужен чтобы навешивать сайд-эффекты и трансформировать результат вызова.
   * @param dargs Аргументы генерации картинки.
   * @return Экземпляр Call, пригодный к употреблению.
   */
  def imgCall(dargs: MImg): Call = {
    routes.Img.dynImg(dargs)
  }

  /**
   * Найти в базе готовую картинку, ранее уже отработанную, и сохранить её в файл.
   * Затем накатить на неё необходимые параметры, пересжав необходимым образом.
   * @param args Данные запроса картинки. Ожидается, что набор параметров не пустой, и подходящей картинки нет в хранилище.
   * @return Файл, содержащий результирующую картинку.
   */
  def mkReadyImgToFile(args: MImg): Future[MLocalImg] = {
    args
      .original
      .toLocalImg
      .map { localImg =>
        // Есть исходная картинка в файле. Пора пережать её согласно настройкам.
        val newLocalImg = args.toLocalInstance
        convert(localImg.get.file, newLocalImg.file, args.dynImgOps)
        newLocalImg
      }(AsyncUtil.jdbcExecutionContext)
  }


  /**
   * Конвертация из in в out согласно списку инструкций.
   * @param in Файл с исходным изображением.
   * @param out Файл для конечного изображения.
   * @param imOps Список инструкций, описывающий трансформацию исходной картинки.
   */
  def convert(in: File, out: File, imOps: Seq[ImOp]): Unit = {
    val op = new IMOperation
    op.addImage(in.getAbsolutePath + "[0]")
    imOps.foreach { imOp =>
      imOp addOperation op
    }
    op.addImage(out.getAbsolutePath)
    val cmd = new ConvertCmd()
    trace("convert(): " + cmd.getCommand.mkString(" ") + " " + op.toString)
    cmd run op
    trace("convert(): Result is " + out.length + " bytes")
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
    }(AsyncUtil.jdbcExecutionContext)
    // Распрямить вложенный фьючерс.
    futFut flatMap identity
  }

}


