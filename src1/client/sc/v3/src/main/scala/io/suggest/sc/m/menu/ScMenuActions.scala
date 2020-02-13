package io.suggest.sc.m.menu

import io.suggest.dev.MOsFamily
import io.suggest.sc.app.MScAppGetResp
import io.suggest.sc.m.ISc3Action

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 14:43
  * Description: Экшены менюшки выдачи.
  */
sealed trait IScMenuAction extends ISc3Action


sealed trait IScAppAction extends IScMenuAction

/** Экшен управления диалогом скачивания приложения. */
case class OpenCloseAppDl( opened: Boolean ) extends IScAppAction

/** Смена платформы в селекте платформы диалога скачивания приложения. */
case class PlatformSetAppDl( osPlatform: MOsFamily ) extends IScAppAction


/** Экшен ручного запуска запроса инфы по приложению. */
case object MkAppDlInfoReq extends IScAppAction

/** Экшен обработки ответа на запрос инфы по скачиванию для платформы. */
case class DlInfoResp( timeStampMs: Long, tryResp: Try[MScAppGetResp] ) extends IScAppAction

