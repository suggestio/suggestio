package io.suggest.quill.v

import com.github.zenoamaro.react.quill._
import com.quilljs.delta.Delta
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.msg.ErrorMsgs
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillInit
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.log.Log
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.QuillUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 21:50
  * Description: react-компонент для редактирования текста.
  */
class QuillEditorR(
                    quillInit: QuillInit
                  )
  extends Log {

  /** Модель пропертисов компонента, учитывающая необходимость костылей.
    * Quill теряет фокус и плохо себя ведёт, если его перерендеривать каждый.
    * Поэтому пере-рендер делается только когда есть реальная необходимость.
    * Однако, всё равно проблемы случаются, и рендер запускается в обход воли FastEq.
    * Поэтому есть значение realDelta, который содержит фактическую текущую дельту.
    *
    * @param initDelta Начальная дельта на момент инициализации редактора.
    * @param realDelta Текущая актуальная дельта, если отличается от initDelta.
    */
  case class PropsVal(
                       initDelta    : Delta,
                       realDelta    : Option[Delta]
                     ) {

    /** Вернуть текущую актуальную дельту. */
    def delta: Delta = {
      realDelta.getOrElse( initDelta )
    }

  }

  implicit object QuillEditorPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.initDelta ===* b.initDelta
      // Не учитываем realDelta в сравнении, см.коммент выше.
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback реагирования на изменение текста в редакторе. */
    private def onTextChanged(html: String, changeset: Delta, source: Source_t,
                              editorProxy: QuillUnpriveledged): Callback = {
      dispatchOnProxyScopeCB($, TextChanged(
        diff      = changeset,
        fullDelta = editorProxy.getContents()
      ))
    }

    private val _onTextChangedF = ReactCommonUtil.cbFun4ToJsCb( onTextChanged )

    private val _quillModulesConf = quillInit.adEditorModules

    def render(propsProxy: Props): VdomElement = {
      val propsOpt = propsProxy.value
      //println(propsOpt.map(p => JSON.stringify(p.initDelta)))

      propsOpt.whenDefinedEl { props =>
        <.div(
          ^.classSet(
            Css.Display.HIDDEN -> propsOpt.isEmpty
          ),

          // Защита от нечитабельных или непонятных багрепортов в console при проблемах инициализации компонента.
          try {
            ReactQuill(
              new ReactQuillPropsR {
                override val value    = props.delta
                override val onChange = _onTextChangedF
                override val modules  = _quillModulesConf
              }
            )
          } catch {
            case ex: Throwable =>
              logger.error(ErrorMsgs.EXT_COMP_INIT_FAILED, ex, props)
              EmptyVdom
          }
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(textOptProxy: Props) = component( textOptProxy )

}
