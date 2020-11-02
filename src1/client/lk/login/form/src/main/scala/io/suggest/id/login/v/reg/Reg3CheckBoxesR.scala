package io.suggest.id.login.v.reg

import com.materialui.{MuiFormControlLabel, MuiFormControlLabelProps, MuiFormGroup, MuiFormGroupClasses, MuiFormGroupProps, MuiLink, MuiLinkProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.{LoginFormDiConfig, RegPdnSetAccepted, RegTosSetAccepted}
import io.suggest.id.login.m.reg.step3.MReg3CheckBoxes
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.CheckBoxR
import io.suggest.proto.http.client.HttpClient
import io.suggest.react.ReactCommonUtil
import io.suggest.routes.routes
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 18:52
  * Description: Шаг финальных галочек и окончания регистрации.
  */
class Reg3CheckBoxesR(
                       checkBoxR           : CheckBoxR,
                       loginFormDiConfig   : LoginFormDiConfig,
                       crCtx               : React.Context[MCommonReactCtx],
                       loginFormCssCtxP    : React.Context[LoginFormCss],
                     ) {

  type Props = ModelProxy[MReg3CheckBoxes]


  private def _privacyPolicyRoute() =
    routes.controllers.Static.privacyPolicy()


  class Backend( $: BackendScope[Props, Unit] ) {

    private val _onPrivacyPolicyLinkClick = {
      ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
        ReactCommonUtil.stopPropagationCB(e) >> {
          $.props >>= { props: Props =>
            loginFormDiConfig
              .showInfoPage
              .fold {
                Callback.empty
              } { openUrlF =>
                openUrlF(
                  HttpClient.mkAbsUrl(
                    HttpClient.route2url(
                      _privacyPolicyRoute() ) ) )
              }
          }
        }
      }
    }

    def render(p: Props): VdomElement = {
      loginFormCssCtxP.consume { loginFormCss =>
        MuiFormGroup {
          val css = new MuiFormGroupClasses {
            override val root = loginFormCss.h100.htmlClass
          }
          new MuiFormGroupProps {
            override val row = true
            override val classes = css
          }
        } (

          // Галочка согласия с офертой suggest.io:
          MuiFormControlLabel {
            val acceptTosText = crCtx.consume { crCtx =>
              <.span(
                crCtx.messages( MsgCodes.`I.accept` ),
                HtmlConstants.SPACE,
                MuiLink(
                  new MuiLinkProps {
                    val href = HttpClient.route2url( _privacyPolicyRoute() )
                    override val onClick = _onPrivacyPolicyLinkClick
                    val target = HtmlConstants.Target.blank
                  }
                )(
                  crCtx.messages( MsgCodes.`terms.of.service` ),
                ),
              )
            }
            val cbx = p.wrap { props =>
              checkBoxR.PropsVal(
                checked   = props.tos.isChecked,
                onChange  = RegTosSetAccepted,
              )
            }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq)
            new MuiFormControlLabelProps {
              override val control = cbx.rawElement
              override val label   = acceptTosText.rawNode
            }
          },

          // Галочка разрешения на обработку ПДн:
          MuiFormControlLabel {
            val pdnText = crCtx.message( MsgCodes.`I.allow.personal.data.processing` )

            val cbx = p.wrap { props =>
              checkBoxR.PropsVal(
                checked   = props.pdn.isChecked,
                onChange  = RegPdnSetAccepted,
              )
            }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq)
            new MuiFormControlLabelProps {
              override val control = cbx.rawElement
              override val label   = pdnText.rawNode
            }
          },

        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
