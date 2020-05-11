package io.suggest.lk.tags.edit.r

import diode.data.Pot
import io.suggest.common.tags.edit.TagsEditConstants.Search.Hints.ATTR_TAG_FACE
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.common.tags.edit.TagsEditConstants
import io.suggest.common.tags.search.MTagsFound
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.tags.edit.m.AddTagFound
import io.suggest.lk.tags.edit.vm.search.hints.SRow
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromHtml, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 18:52
  * Description: Компонент контейнера для найденных тегов.
  */
object TagsFoundR {

  type Props = ModelProxy[Pot[MTagsFound]]

  protected class Backend($: BackendScope[Props, Unit]) {

    /** Коллбэк выбора найденного тега с помощью клика по нему в списке тегов. */
    def onTagFoundClick(e: ReactEventFromHtml): Option[Callback] = {
      for {
        srow    <- SRow.ofHtmlElUp(e.target)
        tagFace <- srow.tagFace
      } yield {
        // Тримминг здесь -- это обычно бесполезная операция. Но делается чисто на всякий случай, для самоуспокоения.
        val tagFace2 = tagFace.trim
        dispatchOnProxyScopeCB($, AddTagFound(tagFace2))
      }
    }

    def render(props: Props): VdomElement = {
      val v = props()
      val isShown = v.exists(_.tags.nonEmpty) || v.isFailed
      <.div(
        ^.classSet1(
          Css.HintList.CONTAINER,
          Css.Display.HIDDEN -> !isShown
        ),
        ReactCommonUtil.maybeEl( isShown ) {
          <.div(
            ^.`class`     := Css.HintList.OUTER,
            <.div(
              ^.`class`   := Css.HintList.CONTENT,

              // Клик активен только когда есть по чему кликать.
              ReactCommonUtil.maybe( v.exists(_.tags.nonEmpty) ) {
                ^.onClick ==>? onTagFoundClick
              },

              // Если снова идёт поиск, то пусть будет спиннер прямо в текущем отображаемом контейнере.
              ReactCommonUtil.maybe(v.isPending) {
                val pleaseWait = Messages( MsgCodes.`Please.wait` )
                LkPreLoader.PRELOADER_IMG_URL.fold[TagMod](pleaseWait) { preloaderUrl =>
                  <.img(
                    ^.src := preloaderUrl,
                    ^.alt := pleaseWait,
                    ^.width := 16.px
                  )
                }
              },

              // Отрендерить список найденных тегов
              {
                val attrTagFace = VdomAttr(ATTR_TAG_FACE)
                val iter = for {
                  state     <- v.iterator
                  tagFound  <- state.tags.iterator
                } yield {
                  <.div(
                    ^.key := tagFound.face,
                    ^.`class` := (Css.HintList.ROW + " " + TagsEditConstants.Search.Hints.HINT_ROW_CLASS),
                    attrTagFace := tagFound.face,

                    <.div(
                      ^.`class` := Css.NAME,
                      <.span(
                        ^.`class` := Css._PREFIX,
                        HtmlConstants.DIEZ
                      ),
                      tagFound.face
                    ),
                    <.div(
                      ^.`class` := Css.VALUE,
                      tagFound.count
                    )
                  )
                }
                iter.toVdomArray
              },

              // При ошибке надо тоже надо не молчать, чтобы эту ошибку быстрее обнаружили и устранили.
              ReactCommonUtil.maybeEl(v.isFailed) {
                <.div(
                  ^.`class` := Css.Colors.RED,
                  Messages( MsgCodes.`Something.gone.wrong` )
                )
              }

            )
          )
        }
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(resp: Props) = component(resp)

}
