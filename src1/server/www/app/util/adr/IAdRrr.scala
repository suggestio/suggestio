package util.adr

import java.io.{File, StringWriter}

import io.suggest.async.IAsyncUtilDi
import models.MImgSizeT
import models.adr.IAdRenderArgs
import models.im.OutImgFmt
import models.mproj.IMCommonDi
import org.apache.commons.io.IOUtils
import util.PlayMacroLogsImpl

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.16 17:24
  * Description: Абстрактный класс для реализации конкретных рендереров карточек.
  * Renderer = Rrr
  */
abstract class IAdRrr
  extends PlayMacroLogsImpl
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
        case ex: NoSuchElementException =>
          val fut1 = _renderCached
          LOGGER.warn("File returned is deleted already o_O")
          fut1
      }
  }

  private def _renderCached: Future[File] = {
    val fut = render

    // Удалить файл с диска через некоторое время, зависящие от времени кеширования.
    val deleteAt = System.currentTimeMillis() + (_cacheSeconds * 2).seconds.toMillis
    fut.onSuccess { case file =>
      val deleteAfterNow = Math.abs(deleteAt - System.currentTimeMillis()).milliseconds
      actorSystem.scheduler.scheduleOnce(deleteAfterNow) {
        file.delete
      }
    }

    fut
  }


  protected def exec(args: Array[String]): Unit = {
    val now = System.currentTimeMillis()
    val p = Runtime.getRuntime.exec(args)

    // Запустить чтение из stderr
    val stdErr = new StringWriter()
    IOUtils.copy( p.getErrorStream, stdErr)

    val result = p.waitFor()
    val tookMs = System.currentTimeMillis() - now

    lazy val cmd = args.mkString(" ")
    lazy val stdErrStr = stdErr.toString

    LOGGER.trace(s"$cmd  ===>>>  $result ; took = $tookMs ms\n$stdErrStr")
    if (result != 0) {
      throw AdRrrFailedException(s"Cannot execute command (result: $result)", cmd + "\n" + stdErrStr)
    }
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
case class AdRrrFailedException(msg: String, privateMsg: String) extends RuntimeException

/** Трейт для реализации статической утили рендереров. */
trait IAdRrrUtil {
  def qualityDflt(scrSz: MImgSizeT, fmt: OutImgFmt): Option[Int] = None
}

/** Интерфейс для будущих DI Factory, собирающих инстансы рендереров. */
trait IAdRrrDiFactory {
  def instance(args: IAdRenderArgs): IAdRrr
}
