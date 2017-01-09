package io.suggest.sc.sjs.m.msrv.index

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.msrv.ToJsonWithApiVsnT
import io.suggest.sc.ScConstants.ReqArgs._
import io.suggest.sjs.common.model.loc.{ILocEnv, MLocEnv}

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:24
 * Description: Client-side версия серверной qs-модели m.sc.ScReqArgs.
 */
trait IScIndexArgs extends ToJsonWithApiVsnT {

  /** Ручные настройки для карточки приветствия. */
  def withWelcome: Boolean

  /** Физические данные по текущей локации. */
  def locEnv  : ILocEnv

  /** Данные по экрану клиентского устройства. */
  def screen  : Option[IMScreen]

  /** id узла index-выдачи. */
  def adnIdOpt: Option[String]

  /** Сериализация в JSON. */
  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    for (adnId <- adnIdOpt)
      d(ADN_ID_FN) = adnId

    val _le = locEnv
    if ( MLocEnv.nonEmpty(_le) )
      d(LOC_ENV_FN) = MLocEnv.toJson(_le)

    for (scr <- screen)
      d(SCREEN_FN) = scr.toQsValue

    d(WITH_WELCOME_FN) = withWelcome

    d
  }

}


/** Дефолтовая реализация [[IScIndexArgs]]. */
case class MScIndexArgs(
  override val adnIdOpt     : Option[String],
  override val locEnv       : ILocEnv,
  override val screen       : Option[IMScreen],
  override val withWelcome  : Boolean
)
  extends IScIndexArgs
