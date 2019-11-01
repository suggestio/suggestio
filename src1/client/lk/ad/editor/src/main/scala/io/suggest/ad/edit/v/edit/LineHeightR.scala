package io.suggest.ad.edit.v.edit

import diode.react.ModelProxy
import io.suggest.ad.edit.m.LineHeightSet
import io.suggest.i18n.MsgCodes
import io.suggest.jd.tags.MJdtProps1
import io.suggest.lk.r.SliderOptR
import io.suggest.msg.Messages
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.2019 12:53
  * Description: Настройка высоты межстрочки.
  */
class LineHeightR(
                   sliderOptR: SliderOptR,
                 ) {

  type Props_t = Option[Option[Int]]
  type Props = ModelProxy[Props_t]

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .stateless
    .render_P { propsOptProxy =>
      val label = Messages( MsgCodes.`Line.height` ): VdomNode
      val onChange = LineHeightSet.apply _

      propsOptProxy.wrap { valueOpt =>
        for (value <- valueOpt) yield {
          val max = MJdtProps1.LineHeight.MAX
          sliderOptR.PropsVal(
            label     = label,
            value     = value,
            onChange  = onChange,
            min       = MJdtProps1.LineHeight.MIN,
            max       = max,
            default   = 25,
          )
        }
      }( sliderOptR.apply )(implicitly, OptFastEq.Wrapped(sliderOptR.SliderOptRPropsValFastEq))
    }
    .build

  def apply(propsValOptProxy: Props) = component(propsValOptProxy)

}
