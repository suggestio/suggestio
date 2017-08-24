package io.suggest.jd.render.v

import scalacss.ScalaCssReact._
import io.suggest.jd.render.m.MJdCommonRa
import io.suggest.jd.tags.{IDocTag, Picture, PlainPayload, Strip}
import io.suggest.model.n2.edge.MPredicates
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

class JdRendererR(bCss: JdCss, common: MJdCommonRa) {

  def render(idt: IDocTag): VdomElement = {
    idt match {

      // Рендер текста.
      case pp: PlainPayload =>
        common.edges.get( pp.edgeId ).whenDefinedEl { e =>
          e.predicate match {
            case MPredicates.Text =>
              // TODO span по факту не нужен: просто текст не подходит под тип VDomElement.
              <.span(
                e.text.get
              )
          }
        }


      // Рендер изображения/картинки
      case p: Picture =>
        common.edges.get(p.edgeUid).whenDefinedEl { e =>
          <.img(
            ^.src := e.url,
            // TODO Отрендерить фактические аттрибуты wh загружаемого файла изображения.
            e.whOpt.whenDefined { wh =>
              // Это фактические размеры изображения внутри файла по указанной ссылке.
              VdomArray(
                ^.width   := wh.width.px,
                ^.height  := wh.height.px
              )
            }
          )
        }

      // Рендер одной полосы
      case s: Strip =>
        <.div(
          bCss.smBlock,
          bCss.smBlockOuterF( common.nodeId ),

          s.children.toVdomArray(render)
        )

    }
  }

}
