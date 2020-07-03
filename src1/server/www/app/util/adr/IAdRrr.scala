package util.adr

import java.io.File
import java.nio.file.Files

import io.suggest.async.IAsyncUtilDi
import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.MImgFormat
import io.suggest.primo.IToPublicString
import io.suggest.util.logs.MacroLogsImpl
import models.adr.IAdRenderArgs
import models.mproj.IMCommonDi

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.16 17:24
  * Description: Абстрактный класс для реализации конкретных рендереров карточек.
  * Renderer = Rrr
  */
abstract class IAdRrr
  extends MacroLogsImpl
  with IMCommonDi
  with IAsyncUtilDi
{

  import mCommonDi._

  val args: IAdRenderArgs

  protected def _cacheSeconds = 5

  /** Запустить рендер карточки, если результат уже не лежит в кеше. */
  def renderCached: Future[File] = {
    // Раньше в коде были комменты, что этот код очень косячный.
    // Но к моменту рефакторинга, этот код давно уже работал. Считаем его более-менее стабилизированным.
    cacheApiUtil
      .getOrElseFut(toString, _cacheSeconds.seconds)(_renderCached)
      .filter { file =>
        file.exists()
      }
      .recoverWith {
        case _: NoSuchElementException =>
          val fut1 = _renderCached
          LOGGER.warn("File returned is deleted already o_O")
          fut1
      }
  }

  private def _renderCached: Future[File] = {
    val fut = render

    // Удалить файл с диска через некоторое время, зависящие от времени кеширования.
    val deleteAt = System.currentTimeMillis() + (_cacheSeconds * 2).seconds.toMillis
    for (file <- fut) {
      val deleteAfterNow = Math.abs(deleteAt - System.currentTimeMillis()).milliseconds
      actorSystem.scheduler.scheduleOnce(deleteAfterNow) {
        file.delete
      }
    }

    fut
  }


  protected def exec(args: Array[String]): Unit = {
    val now = System.currentTimeMillis()

    val stdOutFile = File.createTempFile("ad-rrr-stdouterr", ".log")

    try {
      val p = new ProcessBuilder(args: _*)
        .redirectErrorStream(true)
        .redirectOutput(stdOutFile)
        .start()

      val result = p.waitFor()
      val tookMs = System.currentTimeMillis() - now

      lazy val logMsg = {
        val cmd = args.mkString(" ")
        val stdOutStr = Files.readAllLines(stdOutFile.toPath)
          .iterator()
          .asScala
          .mkString(" | ", "\n | ", "\n")
        s"[$now] $cmd  ===>>>  $result ; took = $tookMs ms\n $stdOutStr"
      }

      if ( isFinishedOk(result) ) {
        LOGGER.debug(logMsg)
      } else {
        LOGGER.error(logMsg)
        throw AdRrrFailedException(now, s"Cannot execute shell command (result: $result)", logMsg)
      }
    } finally {
      stdOutFile.delete()
    }
  }

  def isFinishedOk(returnCode: Int): Boolean = {
    returnCode == 0
  }

  /** Запустить рендер карточки. В зависимости от реализации и используемого рендерера, могут быть варианты. */
  def render: Future[File] = {
    /** Запустить рендер карточки. В зависимости от реализации и используемого рендерера, могут быть варианты. */
    val dstFile = File.createTempFile("ad_render", "." + args.outFmt.name)

    val fut = Future {
      renderSync(dstFile)
    }(asyncUtil.singleThreadIoContext)

    for (_ <- fut) yield {
      dstFile
    }
  }

  /** Синхронный рендер. */
  def renderSync(dstFile: File): Unit

}

/** Экзепшен при запуске рендера. */
case class AdRrrFailedException(now: Long, msg: String, privateMsg: String) extends RuntimeException with IToPublicString {
  override def getMessage = msg + "\n" + privateMsg
  override def toPublicString = s"Ad renderer[$now] internal error."
}

/** Трейт для реализации статической утили рендереров. */
trait IAdRrrUtil {
  def qualityDflt(scrSz: ISize2di, fmt: MImgFormat): Option[Int] = None
}

/** Интерфейс для будущих DI Factory, собирающих инстансы рендереров. */
trait IAdRrrDiFactory {
  def instance(args: IAdRenderArgs): IAdRrr
}
