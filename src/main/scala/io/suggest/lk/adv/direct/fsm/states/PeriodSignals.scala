package io.suggest.lk.adv.direct.fsm.states

import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.lk.dt.interval.m.{PeriodEith_t, PeriodChangedEvent, IDateChangedEvt}
import io.suggest.lk.dt.interval.vm.DatesContainer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 16:05
 * Description: Трейты для сборки состояний, воспринимающих изменения периода размещения.
 */
trait PeriodSignals extends FsmStubT {

  /** Трейт обработки событий интерфейса. */
  protected[this] trait PeriodSignalsState extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Юзер сменил период размещения
      case pce: PeriodChangedEvent =>
        periodChanged(pce)

      // Юзер сменил одну из дат размещения.
      case dtce: IDateChangedEvt =>
        periodDateChanged(dtce)
    }

    /** Юзер выставил новый период в select'е периода дат. */
    protected def periodChanged(pce: PeriodChangedEvent): Unit = {
      val sd0 = _stateData
      val select = pce.vm

      // Понять, изменился ли период. И если изменился, то обновить состояние FSM.
      val newPeriodOpt = select
        .isoPeriodOpt
        .fold [Option[PeriodEith_t]] {
          // Теперь кастомный период. А был до этого какой?
          for {
            oldPeriod     <- sd0.period.right.toOption
            result        <- _getDatesPeriodEithOpt
          } yield {
            result
          }

        } { isoPeriod =>
          // Включен какой-то период-презет.
          val p = sd0.period
          if (p.isLeft || p.right.exists(_ != isoPeriod)) {
            Some(Right(isoPeriod))
          } else {
            None
          }
        }

      _handleNewPeriodOpt(newPeriodOpt, sd0)
    }


    /** Юзер изменил дату в каком-то input'е одной из дат. */
    protected def periodDateChanged(dtce: IDateChangedEvt): Unit = {
      val sd0 = _stateData

      val newPeriodOpt = for {
        oldDatesPeriod <- sd0.period.left.toOption
        result         <- _getDatesPeriodEithOpt
      } yield {
        result
      }

      _handleNewPeriodOpt(newPeriodOpt, sd0)
    }


    /** Общий код извелечения периода дат. */
    protected def _getDatesPeriodEithOpt: Option[PeriodEith_t] = {
      for {
        datesCont     <- DatesContainer.find()
        datesPeriod   <- datesCont.datesPeriodOpt
      } yield {
        Left(datesPeriod)
      }
    }

    /** Общий код реакции на необходимость обновить данные состояния в области периода размещения. */
    protected def _handleNewPeriodOpt(newPeriodOpt: Option[PeriodEith_t], sd0: SD = _stateData): Unit = {
      for (newPeriod <- newPeriodOpt) {
        // Необходимо обновить состояние FSM.
        val sd1 = sd0.copy(
          period = newPeriod
        )
        become(updateSrvDataState, sd1)
      }
    }

    /** Состояние запроса к серверу за инфой (цена, отчет по датам размещения, etc). */
    protected def updateSrvDataState: FsmState

  }

}
