package io.suggest.sys.mdr.v.main

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiSnackBarContent, MuiSnackBarContentProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.{COLON, SPACE}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.routes.routes
import io.suggest.sys.mdr.m.FixNode
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
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
                       fixNodesPots : Map[String, Pot[None.type]],
                     )
  implicit object MdrErrorsRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.errorNodeIds ===* b.errorNodeIds) &&
      (a.isSu ==* b.isSu) &&
      (a.fixNodesPots ===* b.fixNodesPots)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    /** Реакция на клик по кнопке ремонта узла. */
    private def _onFixNodeBtnClick(nodeId: String)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, FixNode(nodeId) )

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>

        ReactCommonUtil.maybeEl( props.errorNodeIds.nonEmpty ) {
          <.div(

            // Рендер списка направильных узлов, присланных сервером:
            props.errorNodeIds.toVdomArray { errNodeId =>
              val potOpt = props.fixNodesPots.get( errNodeId )

              MuiSnackBarContent.component.withKey(errNodeId) {
                val _message = <.span(
                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.h5
                      override val color = MuiColorTypes.secondary
                    }
                  )(
                    Messages( MsgCodes.`Error` ),
                    COLON,
                  ),

                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.subtitle1
                      override val color = MuiColorTypes.secondary
                    }
                  )(
                    Messages( MsgCodes.`Lost.node` ),
                    COLON, SPACE,
                    errNodeId,

                    // Кнопки управления
                    ReactCommonUtil.maybeNode( props.isSu ) {
                      <.span(
                        HtmlConstants.NBSP_STR,

                        // Ссылка на неисправный узел. Она обычно бесполезна.
                        <.a(
                          ^.href := routes.controllers.SysMarket.showAdnNode( errNodeId ).url,
                          MuiIconButton()(
                            Mui.SvgIcons.Link()()
                          )
                        ),
                        HtmlConstants.NBSP_STR,

                        // Кнопка запуска авто-ремонта узла - доступна не всегда
                        ReactCommonUtil.maybeNode( !potOpt.exists(_.isReady) ) {
                          val onClickF = ReactCommonUtil.cbFun1ToJsCb( _onFixNodeBtnClick(errNodeId) )
                          val isDisabled = potOpt.exists(_.isPending)
                          MuiIconButton(
                            new MuiIconButtonProps {
                              override val onClick  = onClickF
                              override val disabled = isDisabled
                            }
                          )(
                            Mui.SvgIcons.Build()()
                          )
                        }

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
