package io.suggest.sys.mdr.v.main

import com.materialui.{Mui, MuiCard, MuiCardContent, MuiCircularProgress, MuiPaper, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs}
import io.suggest.jd.render.v.{JdCss, JdR}
import io.suggest.jd.tags.JdTag
import io.suggest.jd.{MJdAdData, MJdConf}
import io.suggest.msg.Messages
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.routes.routes
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sys.mdr.SysMdrConst
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.18 11:25
  * Description: Рендер узла в отображаемый вид для модерации.
  */
class NodeRenderR(
                   jdR          : JdR,
                 ) {

  case class PropsVal(
                       adData       : Option[MJdAdData],
                       jdCss        : JdCss,
                       adnNodeOpt   : Option[MSc3IndexResp],
                       isSu         : Boolean,
                     )
  implicit object NodeRenderRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.adData ===* b.adData) &&
      (a.jdCss  ===* b.jdCss) &&
      (a.adnNodeOpt ===* b.adnNodeOpt) &&
      (a.isSu ==* b.isSu)
    }
  }


  type Props_t = Pot[Option[PropsVal]]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsOptPotProxy: Props): VdomElement = {
      val propsOptPot = propsOptPotProxy.value

      <.div(

        // Ожидание загрузки.
        propsOptPot.renderPending { _ =>
          MuiCircularProgress()
        },

        // Рендер узла
        propsOptPot.render { propsOpt =>
          propsOpt.fold[VdomNode] {
            // Плашка о том, что ничего не найдено:
            MuiPaper()(
              MuiCard()(
                MuiCardContent()(
                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.h5
                    }
                  )(
                    Messages( MsgCodes.`Nothing.to.moderate` ),
                  ),
                  <.span(
                    ^.`class` := Css.Floatt.RIGHT,
                    Mui.SvgIcons.WbSunny()(),
                  ),
                  <.br,
                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.body2
                    }
                  )(
                    Messages( MsgCodes.`No.incoming.adv.requests` ),
                  )
                ),
              )
            )

          } { props =>
            // Рендер узла:
            <.div(

              // Рендер jd-карточки:
              props.adData.whenDefined { adData =>
                propsOptPotProxy.wrap { _ =>
                  MJdArgs(
                    template  = adData.template,
                    edges     = adData.edgesMap
                      .mapValues( MEdgeDataJs(_) ),
                    jdCss     = props.jdCss,
                    conf      = props.jdCss.jdCssArgs.conf
                  )
                } { jdR.apply }
              },

              // Рендер данных об узле
              props.adnNodeOpt
                .filter(_ => props.isSu)
                .whenDefined { nodeProps =>
                  // TODO Логотип TODO Картинка приветствия, TODO Цвета
                  <.a(
                    nodeProps.nodeId.whenDefined { nodeId =>
                      ^.href := routes.controllers.SysMarket.showAdnNode(nodeId).url
                    },
                    nodeProps.nameOrIdOrEmpty
                  )
                },

            )
          }
        },

        // Ошибка загрузки.
        propsOptPot.renderFailed { ex =>
          <.div(
            Mui.SvgIcons.Error()(),
            ex.toString()
          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsPotProxy: Props ) = component( propsPotProxy )

}

object NodeRenderR {

  /** jdConf всегда один: */
  val JD_CONF = MJdConf(
    isEdit = false,
    szMult = SysMdrConst.SZ_MULT,
    gridColumnsCount = 2
  )

  /** Ленивая сборка jdCss на основе шаблонов. */
  def mkJdCss(jdCss0Opt: Option[JdCss] = None)(tpl: Tree[JdTag]*): JdCss = {
    val args2 = MJdCssArgs(tpl, JD_CONF)
    jdCss0Opt
      // Не пересобирать JdCss, если args не изменились.
      .filter { jdCss0 =>
        MJdCssArgs.MJdCssArgsFastEq.eqv(args2, jdCss0.jdCssArgs)
      }
      .getOrElse {
        // Пересборка JdCss.
        JdCss( args2 )
      }
  }

}
