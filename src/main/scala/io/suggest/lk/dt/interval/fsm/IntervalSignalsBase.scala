package io.suggest.lk.dt.interval.fsm

import io.suggest.fsm.StateData
import io.suggest.lk.dt.interval.m.{PeriodEith_t, IDateChangedEvt, PeriodChangedEvent}
import io.suggest.lk.dt.interval.vm.DatesContainer
import io.suggest.sjs.common.fsm.SjsFsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 9:41
 * Description: Довольно абстрагированный от реализаций FSM-аддон
 * для поддержки принятия сигналов от подформы периодов.
 */
trait IntervalSignalsBase
  extends SjsFsm
  with StateData
{

  /**
   * Получить из state-data текущее значение периода.
   * @param sd Исходное значение _stateData.
   * @return Значение периода.
   */
  protected[this] def _sdGetPeriod(sd: SD): PeriodEith_t

  /**
   * Выставить новое значение периода в контейнер данных состояния.
   * @param newPeriod Новое значение периода.
   * @param sd0 Инстанс данных состояния.
   * @return Новый инстанс данных состояния.
   */
  protected[this] def _sdSetPeriod(newPeriod: PeriodEith_t, sd0: SD): SD


  /** Трейт обработки событий интерфейса виджета задания интервала размещения. */
  protected[this] trait PeriodSignalsStateT extends FsmEmptyReceiverState {

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
            oldPeriod     <- _sdGetPeriod(sd0).right.toOption
            result        <- _getDatesPeriodEithOpt
          } yield {
            result
          }

        } { isoPeriod =>
          // Включен какой-то период-презет.
          val p = _sdGetPeriod(sd0)
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
        oldDatesPeriod <- _sdGetPeriod(sd0).left.toOption
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
        val sd1 = _sdSetPeriod(newPeriod, sd0)
        become(_srvUpdateFormState, sd1)
      }
    }

    /** Состояние запроса к серверу за инфой (цена, отчет по датам размещения, etc). */
    protected def _srvUpdateFormState: State_t

  }


}
