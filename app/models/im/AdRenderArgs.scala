package models.im

import java.io.File

import models.MImgSizeT
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Akka
import util.{PlayMacroLogsDyn, PlayMacroLogsI}
import util.async.AsyncUtil
import util.xplay.CacheUtil
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.03.15 17:34
 * Description: Модель и модельная утиль для доступа к отрендеренным в картинки карточкам.
 */

object AdRenderArgs {

  /** Сколько кешировать в памяти сгенеренную картинку. Картинки жирные и нужны недолго, поэтому следует и
    * кеширование снизить до минимума. Это позволит избежать DoS-атаки. */
  val CACHE_TTL_SECONDS = configuration.getInt("ad.render.cache.ttl.seconds") getOrElse 5

  /** Используемый по умолчанию рендерер. Влияет на дефолтовый рендеринг карточки. */
  val RENDERER: IAdRendererCompanion = {
    val ck = "ad.render.renderer.dflt"
    configuration.getString(ck)
      .fold [IAdRendererCompanion] (WkHtmlArgs) { raw =>
        if (raw startsWith "wkhtml") {
          WkHtmlArgs
        } else if (raw startsWith "phantom") {
          PhantomJsAdRenderArgs
        } else {
          throw new IllegalArgumentException("Unknown ad2img renderer: " + ck + " = " + raw)
        }
      }
  }

  // TODO safe render, который сначала пытается запустить дефолтовый (предпочитаемый) рендер, а при ошибке -- другие доступные.

}


/** Абстрактные параметры для рендера. Даже wkhtml этого обычно достаточно. */
trait IAdRenderArgs extends PlayMacroLogsDyn {

  /** Ссыка на страницу, которую надо отрендерить. */
  def src     : String

  /** 2015.03.06 ЭТО 100% !!ОБЯЗАТЕЛЬНЫЙ!! размер окна браузера и картинки (если кроп не задан).
    * В доках обязательность этого параметра не отражена толком, а --height в man вообще не упоминается. */
  def scrSz   : MImgSizeT

  /** Качество сжатия результирующей картинки. */
  def quality : Option[Int]

  /** Формат сохраняемой картинки. */
  def outFmt  : OutImgFmt

  /** Запустить рендер карточки. В зависимости от реализации и используемого рендерера, могут быть варианты. */
  def render: Future[File]

  /** Время кеширования в секундах. */
  def cacheSeconds: Int = AdRenderArgs.CACHE_TTL_SECONDS

  /** Запустить рендер карточки, если результат уже не лежит в кеше. */
  def renderCached: Future[File] = {
    // TODO Нужно по-лучше протестировать этот код. Он какой-то взрывоопасный.
    val res = CacheUtil.getOrElse(toString, cacheSeconds)(_renderCached)
    // Пока код не оттесирован, используем подобный костыль, чтобы убедиться, что всё ок:
    res.filter { file =>
      file.exists()
    } recoverWith {
      case ex: NoSuchElementException =>
        val fut1 = _renderCached
        LOGGER.error("Please fix this buggy piece of code! File returned is deleted already.")
        fut1
    }
  }


  private def _renderCached: Future[File] = {
    val fut = render
    // Удалить файл с диска через некоторое время, зависящие от времени кеширования.
    val deleteAt = System.currentTimeMillis() + (cacheSeconds * 2).seconds.toMillis
    fut onSuccess { case file =>
      val deleteAfterNow = Math.abs( deleteAt - System.currentTimeMillis() ).milliseconds
      Akka.system.scheduler.scheduleOnce(deleteAfterNow) {
        file.delete
      }
    }
    // Вернуть исходный фьючерс
    fut
  }

}


/** Надстройка над [[IAdRenderArgs]] для поддержки рендера внешней софтиной в указанный файл. */
trait IAdRenderArgsSyncFile extends IAdRenderArgs with PlayMacroLogsI {

  /** Синхронный рендер. */
  def renderSync(dstFile: File): Unit

  protected def exec(args: Array[String]): Unit = {
    val now = System.currentTimeMillis()
    val p = Runtime.getRuntime.exec(args)
    val result = p.waitFor()
    val tookMs = System.currentTimeMillis() - now
    lazy val cmd = args.mkString(" ")
    LOGGER.trace(cmd + "  ===>>>  " + result + " ; took = " + tookMs + "ms")
    if (result != 0) {
      throw new RuntimeException(s"Cannot execute shell command (result: $result) : $cmd")
    }
  }

  /** Запустить рендер карточки. В зависимости от реализации и используемого рендерера, могут быть варианты. */
  override def render: Future[File] = {
    /** Запустить рендер карточки. В зависимости от реализации и используемого рендерера, могут быть варианты. */
    val dstFile = File.createTempFile("ad_render", "." + outFmt.name)
    val fut = Future {
      renderSync(dstFile)
    }(AsyncUtil.singleThreadIoContext)
    fut map { _ =>
      dstFile
    }
  }
}


/** Интерфейс компаньона-генератора параметров. Полезен для доступа к абстрактному рендереру. */
trait IAdRendererCompanion {
  /** Дефолтовое значение quality, если не задано. */
  def qualityDflt(scrSz: MImgSizeT, fmt: OutImgFmt): Option[Int] = None

  def forArgs(src: String, scrSz: MImgSizeT, outFmt: OutImgFmt): IAdRenderArgs = {
    forArgs(src = src, scrSz = scrSz, outFmt = outFmt, quality = qualityDflt(scrSz, outFmt))
  }
  def forArgs(src: String, scrSz: MImgSizeT, outFmt: OutImgFmt, quality : Option[Int]): IAdRenderArgs
}

