package io.suggest.sc.model.dia.first

import diode.Effect
import io.suggest.perm.IPermissionState
import io.suggest.sc.model.ISc3Action

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 17:31
  * Description: Экшены для визарда/визардов.
  */
sealed trait IWz1Action extends ISc3Action


/** Скрыто отрендерить диалог первого запуска, чтобы была анимация на первом шаге.
  *
  * @param showHide true - Инициализация с возможным отображением диалога первого запуска,
  *                 если это действительно первый запуск.
  *                 false - Деинициализация (сокрытие).
  * @param onlyPhases Only selected phases.
  * @param noAsk Skip asking user before permission request, and run native permission request as soon as possible.
  * @param onComplete Effect to call after showHide=false.
  */
case class InitFirstRunWz( showHide     : Boolean,
                           onlyPhases   : Seq[MWzPhase]   = Nil,
                           noAsk        : Boolean         = false,
                           onComplete   : Option[Effect]  = None,
                         )
  extends IWz1Action

/** Клик по кнопкам "да" или "нет". */
case class YesNoWz( yesNo: Boolean ) extends IWz1Action


/** Экшен результата реального запроса пермишшена у юзера.
  *
  * @param phase Фаза, в рамках которой был получен ответ.
  * @param res Ответ, если есть. None - таймаут.
  * @param startTimeMs Mandatory for timers and other WzFirstAh-internal usages.
  *                    None may be used outside usage, like TailAh.
  */
case class WzPhasePermRes(phase: MWzPhase,
                          res: Try[IPermissionState],
                          reason: Option[WzReadPermissions] = None,
                          startTimeMs: Option[Long],
                         )
  extends IWz1Action


/** Async reading all current permissions states into wz's state perms.map. */
case class WzReadPermissions(
                              onComplete    : Option[Effect]          = None,
                              onlyPhases    : Seq[MWzPhase]           = Nil,
                            )
  extends IWz1Action


/** For debugging: show on screen some part of wizard. */
case class WzDebugView(phase: MWzPhase, frame: MWzFrame) extends IWz1Action
