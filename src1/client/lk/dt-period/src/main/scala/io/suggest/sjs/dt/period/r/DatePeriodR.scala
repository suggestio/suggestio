package io.suggest.sjs.dt.period.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.dt.MAdvPeriod
import io.suggest.dt.interval.MRangeYmd
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.Implicits._
import io.suggest.sjs.common.dt.JsDateUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 18:53
  * Description: Date period for diode+react.
  * Компонент верхнего уровня, объединяющий все остальные компоненты подсистемы.
  */
object DatePeriodR {

  type Props = ModelProxy[MAdvPeriod]

  case class State(
                    dateRangeConn     : ReactConnectProxy[MRangeYmd]
                  )

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { props =>
      State(
        dateRangeConn = props.connect { r =>
          r.info.rangeYmd(JsDateHelper)
        }
      )
    }
    .renderPS { (_, props, state) =>
      DtpCont(
        DtpOptions( props ),

        DtpResult.Outer(
          state.dateRangeConn { dateRange =>
            DtpResult( dateRange )
          }
        )
      )
    }
    .build

}
