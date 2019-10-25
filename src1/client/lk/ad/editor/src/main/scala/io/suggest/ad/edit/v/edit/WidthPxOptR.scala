package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.blk.BlockWidths
import io.suggest.i18n.MsgCodes
import io.suggest.jd.JdConst
import io.suggest.jd.edit.m.ResizeContent
import io.suggest.lk.r.SliderOptR
import io.suggest.msg.Messages
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 22:14
  * Description: Компонент галочки и слайдера ротации.
  */
class WidthPxOptR(
                   sliderOptR       : SliderOptR,
                 ) {

  case class PropsVal(
                       value: Option[Int]
                     )
  implicit object WidthPxPropsValPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.value ==* b.value
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsOptProxy =>
      val label = Messages( MsgCodes.`Width` ): VdomNode
      val onChange = ResizeContent.apply _

      propsOptProxy.wrap { propsOpt =>
        for (props <- propsOpt) yield {
          val min = JdConst.ContentWidth.MIN_PX
          sliderOptR.PropsVal(
            label     = label,
            value     = props.value,
            onChange  = onChange,
            min       = min,
            max       = JdConst.ContentWidth.MAX_PX,
            default   = BlockWidths.min.value,
          )
        }
      }( sliderOptR.apply )(implicitly, OptFastEq.Wrapped(sliderOptR.SliderOptRPropsValFastEq))
    }
    .build

  def apply(propsValOptProxy: Props) = component(propsValOptProxy)

}
