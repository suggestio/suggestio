package io.suggest.lk.dt.interval.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 18:30
 * Description: vm'ка контейнера с инпутами дат начала и конца при кастомном задании периода.
 */
object DatesContainer extends FindDiv {
  override type T     = DatesContainer
  override def DOM_ID = DatesIntervalConstants.DATES_CONT_ID
}


import DatesContainer.Dom_t


trait DatesContainerT
  extends ShowHideDisplayT
  with IInitLayoutFsm
  with SetIsShown
  with SjsLogger
{
  override type T = Dom_t

  def start = StartVm.find()
  def end   = EndVm.find()
  def dtPickInitArgsInput = InitArgsInput.find()

  /** Инициализация контейнера с параметрами. */
  override def initLayout(fsm: SjsFsm): Unit = {
    val argsInputOpt = dtPickInitArgsInput
    val argsOpt = argsInputOpt.flatMap(_.valueJson)

    if (argsOpt.isEmpty) {
      warn( WarnMsgs.DT_PICKER_ARGS_MISSING )
    }

    val applyF = { dvm: DateVmT =>
      dvm.initLayout(fsm)
      dvm.initDtPicker(argsOpt.orNull)
    }

    start.foreach(applyF)
    end.foreach(applyF)

    // Тег с параметрами больше не нужен -- удалить его.
    for(ai <- argsInputOpt) {
      ai.remove()
    }
  }

  def datesPeriodOpt: Option[(String, String)] = {
    for {
      s   <- start
      sd  <- s.valueOpt
      e   <- end
      ed  <- e.valueOpt
    } yield {
      (sd, ed)
    }
  }

}


case class DatesContainer(
  override val _underlying: Dom_t
)
  extends DatesContainerT
