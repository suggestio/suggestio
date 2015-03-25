package models.im

import java.io.File
import java.nio.file.Files

import models.MImgSizeT
import play.api.Play.{current, configuration}
import util.PlayMacroLogsI
import util.async.AsyncUtil
import util.xplay.CacheUtil
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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

  /** Дефолтовый формат сохраняемой картинки. */
  val OUT_FMT_DFLT = configuration.getString("ad.render.img.fmt.dflt")
    .fold(OutImgFmts.PNG) { OutImgFmts.withName }

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
trait IAdRenderArgs {

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
  def render: Future[Array[Byte]]


  /** Время кеширования в секундах. */
  def cacheSeconds: Int = AdRenderArgs.CACHE_TTL_SECONDS

  /** Запустить рендер карточки, если результат уже не лежит в кеше. */
  def renderCached: Future[Array[Byte]] = {
    CacheUtil.getOrElse(toString, cacheSeconds)(render)
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
  override def render: Future[Array[Byte]] = {
    /** Запустить рендер карточки. В зависимости от реализации и используемого рендерера, могут быть варианты. */
    val dstFile = File.createTempFile("ad_render", "." + outFmt.name)
    val fut = Future {
      renderSync(dstFile)
      Files.readAllBytes(dstFile.toPath)
    }(AsyncUtil.singleThreadIoContext)
    fut onComplete { case _ =>
      dstFile.delete()
    }
    fut
  }
}


/** Интерфейс компаньона-генератора параметров. Полезен для доступа к абстрактному рендереру. */
trait IAdRendererCompanion {
  def forArgs(src: String, scrSz: MImgSizeT, quality : Option[Int], outFmt: OutImgFmt): IAdRenderArgs
}

