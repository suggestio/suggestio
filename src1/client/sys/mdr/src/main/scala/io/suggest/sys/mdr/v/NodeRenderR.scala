package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiCircularProgress}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.jd.{MJdAdData, MJdConf}
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs}
import io.suggest.jd.render.v.{JdCss, JdR}
import io.suggest.jd.tags.JdTag
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.routes.routes
import io.suggest.sys.mdr.SysMdrConst
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalaz.Tree
import japgolly.univeq._

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
                       adnNodeOpt   : Option[MAdvGeoMapNodeProps],
                     )
  implicit object NodeRenderRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.adData ===* b.adData) &&
      (a.jdCss  ===* b.jdCss) &&
      (a.adnNodeOpt ===* b.adnNodeOpt)
    }
  }


  type Props_t = Pot[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsPotProxy: Props): VdomElement = {
      val propsPot = propsPotProxy.value

      <.div(

        // Ожидание загрузки.
        propsPot.renderPending { _ =>
          MuiCircularProgress()
        },

        // Рендер узла
        propsPot.render { props =>
          <.div(

            // Рендер jd-карточки:
            props.adData.whenDefined { adData =>
              propsPotProxy.wrap { _ =>
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
            props.adnNodeOpt.whenDefined { nodeProps =>
              // TODO Логотип TODO Картинка приветствия, TODO Цвета
              <.a(
                ^.href := routes.controllers.SysMarket.showAdnNode( nodeProps.nodeId ).url,
                nodeProps.hintOrId
              )
            },

          )
        },

        // Ошибка загрузки.
        propsPot.renderFailed { ex =>
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
