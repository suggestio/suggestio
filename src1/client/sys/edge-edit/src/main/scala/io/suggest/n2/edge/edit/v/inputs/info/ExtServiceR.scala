package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiFormControlClasses, MuiMenuItem, MuiMenuItemProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.ext.svc.{MExtService, MExtServices}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.ExtServiceSet
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.2020 12:33
  * Description: Выбор опционального поля e.info.extService.
  */
class ExtServiceR(
                   crCtxProv: React.Context[MCommonReactCtx],
                 ) {

  type Props_t = Option[MExtService]
  type Props = ModelProxy[Props_t]


  case class State(
                    extServiceOptC : ReactConnectProxy[Props_t],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _onChangeCbF = ReactCommonUtil.cbFun1ToJsCb { (e: ReactEventFromInput) =>
      val extServiceOpt = for {
        extServiceRaw0 <- Option( e.target.value )
        extServiceRaw = extServiceRaw0.trim
        if extServiceRaw.nonEmpty
        extService <- MExtServices.withValueOpt( extServiceRaw )
      } yield extService

      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ExtServiceSet(extServiceOpt) )
    }

    def render(s: State): VdomElement = {
      crCtxProv.consume { crCtx =>
        val _label = crCtx.messages( MsgCodes.`External.service` ): VdomNode

        val emptyStr = ""

        // Список пунктов ext-сервисов, неизменяем:
        val _children: List[VdomElement] = {
          MuiMenuItem(
            new MuiMenuItemProps {
              override val value = emptyStr
            }
          )(
            `(`,
            crCtx.messages( MsgCodes.`empty` ),
            `)`,
          )
        } :: (for {
          extService <- MExtServices.values.iterator
        } yield {
          MuiMenuItem(
            new MuiMenuItemProps {
              override val value = extService.value
            }
          )(
            crCtx.messages( extService.nameI18N ),
          ): VdomElement
        })
          .toList

        val css = new MuiFormControlClasses {
          override val root = Css.flat( EdgeEditCss.inputLeft.htmlClass, EdgeEditCss.w200.htmlClass )
        }
        s.extServiceOptC { extServiceOptProxy =>
          MuiTextField {
            val _value = extServiceOptProxy
              .value
              .fold(emptyStr)(_.value)
            new MuiTextFieldProps {
              override val select   = true
              override val label    = _label.rawNode
              override val value    = _value
              override val onChange = _onChangeCbF
              override val classes  = css
              override val variant  = MuiTextField.Variants.standard
            }
          } (
            _children: _*
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        extServiceOptC = propsProxy.connect(identity),
      )
    }
    .renderBackend[Backend]
    .build

}
