package io.suggest.sjs.dt.period.vm

import io.suggest.adv.AdvConstants
import io.suggest.common.qs.QsConstants
import io.suggest.css.Css
import io.suggest.lk.r.{Forms, PropTable}
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.vm.LkMessagesWindow.lkJsMessages
import io.suggest.sjs.common.dt.interval.{QuickAdvPeriod, QuickAdvPeriods}
import io.suggest.sjs.dt.period.m.{IDateInfo, IDatesPeriodInfo}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:49
  * Description: Реализация виджета выбора периода размещения на базе react.js.
  * Шаблон и логика в одном флаконе.
  *
  * Эксплуатировать сий код надо вот так:
  * {{{
  *   Cont(
  *     DtpOptions(...),  // (1) Передать исходные параметры.
  *     DtpResult(...)    // (2) Значения пока что обсчитываются и рендерятся на сервере на основе (1)
  *                       // вместе с ценой (которая обитает вне этого модуля).
  *   )
  * }}}
  */
object ReactDtPeriod {

  /** Компонент внешнего контейнера виджета. */
  object Cont {

    val component = ReactComponentB[Unit]("DtpCont")
      .stateless
      .render_C { children =>
        // Контейнер виджета
        <.div(
          ^.`class` := Css.Lk.Adv.RIGHT_BAR,

          // Заголовок виджета
          <.h2(
            ^.`class` := Css.Lk.MINOR_TITLE,
            lkJsMessages( "Date.choosing" )
          ),

          // Контейнер кусков виджета.
          <.div(
            ^.`class` := Css.Dt.DT_WIDGET,
            children
          )
        )
      }
      .build

    def apply(children: ReactElement*) = component(children)

  }


  /** Поле ввода даты. */
  object DateInput {

    case class Props(fn: String, value0: Option[String])

    val component = ReactComponentB[Props]("DtpDateInput")
      .initialState_P(_.value0.getOrElse(""))
      .renderPS { (_, props, state) =>
        PropTable.Outer(
          PropTable.Row(
            lkJsMessages( "Date." + props.fn ),

            Forms.InputCont(
              <.input(
                ^.`type` := "text",
                ^.name   := (AdvConstants.PERIOD_FN + QsConstants.KEY_PARTS_DELIM_STR + AdvConstants.DtPeriod.DATES_INTERVAL_FN + QsConstants.KEY_PARTS_DELIM_STR + props.fn),
                ^.value  := state
              )
            )
          )
        )
      }
      .build

    def apply(props: Props) = component(props)

  }


  /** Рендер Options-раздела целиком. */
  object DtpOptions {

    case class State(
      qap         : QuickAdvPeriod,
      customDates : Option[(String, String)] = None
    )

    case class Props(
      onChange  : () => Unit,
      state0    : State = State(QuickAdvPeriods.default)
    )


    class Backend($: BackendScope[Props, State]) {

      /** Реакция на смену значения в селекте периода размещения. */
      def onQapChange(e: ReactEventI): Callback = {
        QuickAdvPeriods.maybeWithName( e.target.value )
          .fold(Callback.empty) { qap2 =>
            for {
              _     <- $.modState { _.copy(qap = qap2) }
              props <- $.props
            } yield {
              props.onChange()
            }
          }
      }

      def render(state: State) = {
        <.div(
          ^.`class` := Css.Dt.OPTIONS,
          PropTable.Outer(
            PropTable.Row(
              // Пояснение по сути
              lkJsMessages( "Advertising.period" ),

              // Селект
              Forms.InputCont(
                Css.Size.S,
                <.select(
                  ^.name := ( AdvConstants.PERIOD_FN + QsConstants.KEY_PARTS_DELIM_STR + AdvConstants.DtPeriod.QUICK_PERIOD_FN ),
                  ^.value := state.qap.strId,
                  // Отрендерить опшены: сначала quick-периоды, затем кастомные режимы дат.
                  for (qap <- QuickAdvPeriods.values) yield {
                    <.option(
                      ^.value := qap.strId,
                      lkJsMessages( qap.messagesCode )
                    )
                  }
                )
              )
            )
          ),

          (state.qap == QuickAdvPeriods.Custom) ?= <.div(
            DateInput(
              DateInput.Props("start", state.customDates.map(_._1))
            ),
            DateInput(
              DateInput.Props("end", state.customDates.map(_._2))
            )
          )
        )
      }

    }

    val component = ReactComponentB[Props]("DtpOptions")
      .initialState_P( _.state0 )
      .renderBackend[Backend]
      .build

    def apply(props: Props) = component(props)

  }

  //----------------------------------------------------------
  // Рассчетный результат периода дат:
  // DtpResult(Props(start, end))
  //----------------------------------------------------------

  /** Интерфейс будущей json-модели, хранящей отрендеренные даты размещения. */
  // TODO Вынести отсюда куда-нибудь в модели.


  val ResValDate = ReactComponentB[IDateInfo]("DtpResDate")
    .stateless
    .render_P { props =>
      <.span(
        ^.title := props.dow,
        props.date
      )
    }
    .build


  /** Отчёт о датах размещения. */
  object DtpResult {

    val component = ReactComponentB[IDatesPeriodInfo]("DtpResVal")
      .stateless
      .render_P { props =>
        // Контейнер
        <.div(
          ^.`class` := Css.Dt.RESULT,

          // Статический заголовок по рассчётным датам размещения:
          <.div(
            ^.`class` := (Css.Block.BLOCK :: Css.Margin.S :: Nil).mkString(" "),
            lkJsMessages( "Your.ad.will.adv" )
          ),

          // Содержимое
          <.div(
            ^.`class` := Css.Dt.RESULT_VALUE,

            // с [дата начала]
            lkJsMessages( "from._date" ),
            ResValDate(props.start),

            // до [дата окончания]
            lkJsMessages( "till._date" ),
            ResValDate(props.end)
          )
        )
      }
      .build

    def apply(props: IDatesPeriodInfo) = component(props)

  }

}
