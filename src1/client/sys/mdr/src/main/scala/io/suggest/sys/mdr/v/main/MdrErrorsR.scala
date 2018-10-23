package io.suggest.sys.mdr.v.main

import chandu0101.scalajs.react.components.materialui.{MuiColorTypes, MuiSnackBarContent, MuiSnackBarContentProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.{COLON, SPACE}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.routes.routes
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 15:11
  * Description: Компонент для сообщений модератору о каких-то ошибках.
  */
class MdrErrorsR {

  case class PropsVal(
                       errorNodeIds : Iterable[String],
                       isSu         : Boolean,
                     )
  implicit object MdrErrorsRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.errorNodeIds ===* b.errorNodeIds) &&
      (a.isSu ==* b.isSu)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>

        ReactCommonUtil.maybeEl( props.errorNodeIds.nonEmpty ) {
          <.div(

            // Рендер списка направильных узлов, присланных сервером:
            props.errorNodeIds.toVdomArray { errNodeId =>
              MuiSnackBarContent.component.withKey( errNodeId) {
                val _message = <.span(
                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.headline
                      override val color = MuiColorTypes.secondary
                    }
                  )(
                    Messages( MsgCodes.`Error` ),
                    COLON,
                  ),

                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.subheading
                      override val color = MuiColorTypes.secondary
                    }
                  )(
                    Messages( MsgCodes.`Lost.node` ),
                    COLON, SPACE,
                    errNodeId,
                    ReactCommonUtil.maybeNode( props.isSu ) {
                      <.a(
                        ^.href := routes.controllers.SysMarket.showAdnNode( errNodeId ).url,
                        HtmlConstants.`~`
                      )
                    }
                  )
                )
                new MuiSnackBarContentProps {
                  override val message = _message.rawNode
                }
              }
            }

          )
        }

      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
