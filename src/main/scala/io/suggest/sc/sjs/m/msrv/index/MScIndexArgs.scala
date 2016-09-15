package io.suggest.sc.sjs.m.msrv.index

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.msrv.ToJsonWithApiVsnT
import io.suggest.sc.ScConstants.ReqArgs._

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:24
 * Description: Client-side версия серверной qs-модели m.sc.ScReqArgs.
 */
trait IScIndexArgs extends ToJsonWithApiVsnT {

  /** Ручные настройки для карточки приветствия. */
  def withWelcome: Option[Boolean]

  /** Данные по геолокации. */
  def geoMode : Option[IMGeoMode]

  /** Данные по экрану клиентского устройства. */
  def screen  : Option[IMScreen]

  /** id узла. */   // TODO На момент написания жил вне JSON-API. Нужно унифицировать два экшена к index-странице.
  def adnIdOpt: Option[String]

  /** Сериализация в JSON. */
  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    for (g <- geoMode)
      d(GEO_FN) = g.toQsStr
    for (scr <- screen)
      d(SCREEN_FN) = scr.toQsValue
    for (ww <- withWelcome)
      d(WITH_WELCOME_FN) = ww

    d
  }

}


/** Дефолтовая реализация [[IScIndexArgs]]. */
case class MScIndexArgs(
  override val adnIdOpt     : Option[String],
  override val geoMode      : Option[IMGeoMode],
  override val screen       : Option[IMScreen],
  override val withWelcome  : Option[Boolean] = None
) extends IScIndexArgs
