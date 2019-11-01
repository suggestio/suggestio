package io.suggest.ad.edit.v.edit

import diode.react.ModelProxy
import io.suggest.ad.edit.m.RotateSet
import io.suggest.i18n.MsgCodes
import io.suggest.jd.JdConst
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
class RotateR(
               sliderOptR       : SliderOptR,
             ) {

  type Props_t = Option[Option[Int]]
  type Props = ModelProxy[Props_t]


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .stateless
    .render_P { propsOptProxy =>
      val label = Messages( MsgCodes.`Rotation` ): VdomNode
      val onChange = RotateSet.apply _

      propsOptProxy.wrap { valueOpt =>
        for (value <- valueOpt) yield {
          sliderOptR.PropsVal(
            label     = label,
            value     = value,
            onChange  = onChange,
            min       = -JdConst.ROTATE_MAX_ABS,
            max       =  JdConst.ROTATE_MAX_ABS,
            default   = 15,
          )
        }
      }( sliderOptR.apply )(implicitly, OptFastEq.Wrapped(sliderOptR.SliderOptRPropsValFastEq))
    }
    .build

  def apply(propsValOptProxy: Props) = component(propsValOptProxy)

}
